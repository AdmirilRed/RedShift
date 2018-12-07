import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Deque;
import java.util.*;
import java.lang.*;
import java.net.*;
import org.json.*;
import java.io.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RedShiftServer implements Runnable {

    public static final String DEFAULT_MOTD = "Welcome to the RedShift server!";
    public static final String DEFAULT_CHANNEL_NAME = "general";
    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_BACKLOG = 50;
    public static final int DEFAULT_PORT = 25565;
    public static final int MAX_ATTEMPTS = 3;

     
    private final Set<Channel> channels;
    private final ServerSocket socket;
    private final String configFile;
    private final int port;

    private boolean requirePassword;
    private Channel defaultChannel;
    private boolean running;
    private byte[] password;
    private String salt;
    private String motd;
    public Command cmd;

    public static void main(String [] args) {

        try {

            RedShiftServer server = null;
            String configFile = "config.json";

            if(args.length == 1)
                configFile = args[0];
            if(new File(configFile).isFile())
                server = new RedShiftServer("config.json");
            else {
                System.out.println("The file \"" + configFile + "\" was not found! Startup aborted.");
                System.exit(1);
            }
            Thread t = new Thread(server);
            t.start();

        } catch(Exception e) {

            e.printStackTrace();
            
        }
    }

    public RedShiftServer(String configFile)
        throws IOException, NoSuchAlgorithmException {

        JSONParser parser = new JSONParser();

        this.channels = new HashSet<>();
        this.configFile = configFile;
        InetAddress addr = null;
        int tempPort = -1;

        try {

            JSONObject config = (JSONObject) parser.parse(new FileReader(configFile));
            
            String serverName = (String) config.get("serverName");
            String password = (String) config.get("password");
            if(!password.isEmpty())
                this.setPassword(password);
            String motd = (String) config.get("motd");
            if(!motd.isEmpty())
                this.motd = motd;
            else
                this.motd = DEFAULT_MOTD;
            String address = (String) config.get("address");
            addr = InetAddress.getLocalHost();
            if(!address.isEmpty())
                addr = InetAddress.getByName(address);
            String port = (String) config.get("port");
            if(!port.isEmpty())
                tempPort = Integer.parseInt(port);
            else
                tempPort = DEFAULT_PORT;
            JSONArray channels = (JSONArray) config.get("channels");
            for(Object obj : channels) {
                
                String channelName = (String) obj;
                Channel temp = this.createChannel(channelName);
                if(this.defaultChannel == null)
                    this.defaultChannel = temp;
                
            }
            this.cmd = new Command(this);

        } catch(Exception e) {

            System.out.println("Error processing the config file!");
            e.printStackTrace();
            System.exit(1);

        } finally {

            this.port = tempPort;
            this.socket = new ServerSocket(this.port, DEFAULT_BACKLOG, addr);

        }

    }

    public Channel findChannel(String channelName) {
        for(Channel channel : channels){
            if(channel.getName().equals(channelName)) {
                return channel;
            }
        }
        return null;
    }

    public void setPassword(String password) 
        throws NoSuchAlgorithmException {

        SecureRandom cryptogen = new SecureRandom();
        this.salt = "" + cryptogen.nextInt();
        this.password = hash(this.salt + password);
        this.requirePassword = true;

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

    public String toString() {
        StringBuilder result = new StringBuilder();
        for(Channel channel : channels) {
            result.append(channel.toString());
            result.append("\n");
        }
        return result.toString();
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



