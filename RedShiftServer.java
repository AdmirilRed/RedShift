import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Deque;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.net.DatagramSocket;

public class RedShiftServer implements Runnable {

    public static final String DEFAULT_MOTD = "Welcome to the RedShift server!";
    public static final String DEFAULT_CHANNEL_NAME = "general";
    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_BACKLOG = 50;
    public static final int DEFAULT_PORT = 25565;
    public static final int MAX_ATTEMPTS = 3;

    private final Set<Channel> channels;
    private final ServerSocket socket;
    private final int port;

    private boolean requirePassword;
    private Channel defaultChannel;
    private boolean running;
    private byte[] password;
    private String salt;
    private String motd;

    public static void main(String [] args) {

        try {

            RedShiftServer server;
            if(args.length == 0)
                server = new RedShiftServer(DEFAULT_PORT);
            else if(args.length == 1)
                server = new RedShiftServer(Integer.parseInt(args[0]));
            else
                server = new RedShiftServer(Integer.parseInt(args[0]), args[1]);
            Thread t = new Thread(server);
            t.start();

        } catch(Exception e) {

            e.printStackTrace();
            
        }
    }

    public RedShiftServer()
        throws IOException, NoSuchAlgorithmException {

        this(DEFAULT_PORT);

    }

    public RedShiftServer(int port)
        throws IOException, NoSuchAlgorithmException {

        this(port, null);

    }

    public RedShiftServer(int port, String password) 
        throws IOException, NoSuchAlgorithmException {


        this.port = port;
        InetAddress addr = InetAddress.getLocalHost();
        this.socket = new ServerSocket(this.port, DEFAULT_BACKLOG, addr);

        this.channels = new HashSet<>();
        this.defaultChannel = this.createChannel(DEFAULT_CHANNEL_NAME);

        this.motd = DEFAULT_MOTD;

        if(password != null) {

            this.requirePassword = true;
            this.setPassword(password);

        }

    }

    public void setPassword(String password) 
        throws NoSuchAlgorithmException {

        SecureRandom cryptogen = new SecureRandom();
        this.salt = "" + cryptogen.nextInt();
        this.password = hash(this.salt + password);

    }

    public boolean validatePassword(String token)
        throws NoSuchAlgorithmException {

        byte[] temp = RedShiftServer.hash(token);
        if(temp.length == this.password.length) {

            for(int index = 0; index < temp.length; index++) {

                byte b1 = temp[index];
                byte b2 = this.password[index];

                if(b1 != b2)
                    return false;

            }

            return true;
        }

        return false;
    }

    public boolean authenticate(Client client) {

        if(this.requiresPassword()) {

            try {

                int attempts = RedShiftServer.MAX_ATTEMPTS;
                while(attempts > 0) {

                    client.sendMessage("Server password required. Please enter it now.");
                    String token = this.salt + client.getMessage();
                    if(!this.validatePassword(token)) {

                        attempts--;
                        if(attempts > 0) {
                            
                            client.sendMessage("Incorrect password: " + attempts + " attempts left.");
                            Thread.sleep(1000);
                            
                        } else {

                            client.sendMessage("Max attemps reached. Disconnecting.");
                            client.disconnect();
                            return false;

                        }

                    } else {

                        client.sendMessage("Password accepted!");
                        return true;

                    }
                }

            } catch(Exception e) {

                e.printStackTrace();
                return false;

            }


        }

        return true;
    }

    public boolean requiresPassword() {

        return this.requirePassword;
    }

    public Channel createChannel(String name) {

        Channel newChannel = new Channel(name);
        this.channels.add(newChannel);
        return newChannel;

    }

    public Channel getDefaultChannel() {

        return this.defaultChannel;

    }

    public String getMOTD() {

        return this.motd;

    }

    public static byte[] hash(String text) 
        throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

        return hash;
    }

    public void run() {
        
        if(!this.running) {

            String startMessage = String.format("Server started at %s on port %d.\n", 
                this.socket.getInetAddress(), this.socket.getLocalPort());
            System.out.println(startMessage);

            this.running = true;
            while(this.running) {

                try {

                    System.out.println("Waiting for connection...");
                    Socket connection = this.socket.accept();
                    Client newClient = new Client(this, connection);
                    Thread t = new Thread(newClient);
                    t.start();

                } catch(Exception e) {

                    e.printStackTrace();
                    System.out.println("Error establishing connection!");

                }
            }
        }
    }
}

class Channel {

    private final Set<Client> clients;

    private String name;

    public Channel(String name) {

        this.clients = new HashSet<>();
        this.name = name;

    }

    public String getName() {

        return this.name;
    }

    public boolean addClient(Client client) {

        boolean result = this.clients.add(client);
        if(result) {

            this.broadcast(client.getHandle() + " joined the channel.");
        }

        return result;
    }

    public boolean removeClient(Client client) {

        boolean result = this.clients.remove(client);
        if(result) {

            this.broadcast(client.getHandle() + " disconnected from the channel.");
        }

        return result;
    }

    public void broadcast(String message) {

        for(Client client : clients)
            client.sendMessage(message);
        System.out.printf("(%s)|%s\n", this.getName(), message);
    }

    public int currentUsers() {

        return this.clients.size();
    }

    public String listHandles() {

        StringBuilder result = new StringBuilder();
        for(Client client : this.clients) {

            result.append(client.getHandle());
            result.append(", ");

        }

        result.delete(result.length() - 2, result.length());
        return result.toString();
    }

    public String toString() {

        String result = String.format("Channel: \"%s\" Clients: %s", this.name, this.clients);
        return result;
        
    }

}

class Client implements Runnable {

    private final RedShiftServer server;
    private final Socket connection;
    private final BufferedReader in;
    private final PrintWriter out;
    
    private Channel currentChannel;
    private boolean validated;
    
    private String handle;

    public Client(RedShiftServer server, Socket connection) throws IOException {

        this.server = server;
        this.connection = connection;
        this.in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        this.out = new PrintWriter(connection.getOutputStream());
        System.out.println("Connection esablished with " + connection.getInetAddress());

    }

    public String getHandle() {

        return this.handle;
    }

    public void setHandle(String handle) {

        String oldHandle = this.handle;
        this.handle = handle;

        if(this.currentChannel != null)
            this.currentChannel.broadcast(oldHandle + " is now known as " + this.handle);

    }

    public String toString() {

        InetAddress address = this.connection.getInetAddress();
        String result = String.format("<%s:%s>", this.handle, address);
        
        return result;
    }

    public void sendMessage(String message) {

        this.out.println(message);
        this.out.flush();

    }

    public String getMessage() 
        throws IOException {

        return this.in.readLine();
    }

    public void joinChannel(Channel channel) {

        if(this.currentChannel != null)
            currentChannel.removeClient(this);
        this.currentChannel = channel;
        this.sendMessage("Joining channel \"" + this.currentChannel.getName() + "\"");
        if(this.currentChannel.currentUsers() > 0)
            this.sendMessage("Online: " + this.currentChannel.listHandles());
        this.currentChannel.addClient(this);
    }

    public void disconnect() {

        try {

            this.out.close();
            this.in.close();
            this.connection.close();

        } catch(IOException e) {

            e.printStackTrace();

        }

        if(this.currentChannel != null)
            this.currentChannel.removeClient(this);

    }

    public void run() {

        try {

            this.validated = this.server.authenticate(this);
            if(!this.validated)
                return;
            this.sendMessage(this.server.getMOTD());
            this.sendMessage("Please enter a username.");
            String handle = this.in.readLine();
            this.setHandle(handle);
            this.joinChannel(this.server.getDefaultChannel());
            
            while(true) {

                String message = this.getMessage();
                if(message == null) {

                    this.disconnect();
                    return;
                }
                message = String.format("[%s]: %s", this.handle, message);
                this.currentChannel.broadcast(message);
                
            }

        } catch(Exception e) {

            this.disconnect();
            e.printStackTrace();
            System.out.println("Error with client " + this.toString());
            return;
        }
    }

}