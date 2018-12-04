import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Deque;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;

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
    private Command cmd;

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
        this.cmd = new Command(this);

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



