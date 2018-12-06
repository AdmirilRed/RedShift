import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;

public class Client implements Runnable {

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

                if(server.cmd.isCommand(message))
                    server.cmd.parseCommand(message, this, currentChannel);
                else {
                    message = String.format("[%s]: %s", this.handle, message);
                    this.currentChannel.broadcast(message);
                }
            }

        } catch(Exception e) {

            this.disconnect();
            e.printStackTrace();
            System.out.println("Error with client " + this.toString());
            return;
        }
    }

    public Channel getCurrentChannel(){
        return currentChannel;
    }

}