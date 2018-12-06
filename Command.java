import java.util.*;
public class Command {
    private RedShiftServer server;

    public Command(RedShiftServer server) {
        this.server = server;
    }

    public void parseCommand(String commandTxt, Client client) {
        String[] commandList = commandTxt.split(" ");
        String arguments = null;
        if(commandList.length==2)
            arguments = commandList[1];
        String command = commandList[0];
        switch(command) {
            case "/leave":
                leave(client);
                break;
            case "/join":
                join(client, arguments);
                break;
            case "/create":
                create(client, arguments);
                break;
            case "/kick":
                kick(client, arguments);
                break;
            case "/ping":
                ping(client);
                break;
            case "/list":
                list(client);
                break;
            case "/help":
            default:
                help(client);
                break;
        }
    }

    public boolean isCommand(String message) {
        if(message!=null && message.startsWith("/"))
            return true;
        return false;
    }

    public void leave(Client client) {

    }

    public void join(Client client, String args) {
        if(args==null) {
            client.sendMessage("Please specify a channel to join");
            return;
        }
        String channel = args;
        Channel next = server.findChannel(channel);
        if(next!= null)
            client.joinChannel(next);
        else
            client.sendMessage("Could not find channel "+channel);
    }

    public void kick(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a user");
            return;
        }
        Channel channel = client.getCurrentChannel();
        String user = args;
        if(channel!=null) {
            Client kicked = channel.findClient(user);
            if(kicked==null)
                client.sendMessage(String.format("Could not find user '%s' in current channel. Try /list for list of current users", user));
            else{
                channel.removeClient(kicked);
                kicked.sendMessage(String.format("You were removed from %s by %s. Try /join [channel] to join a new channel or reconnect", channel.getName(), client.getHandle()));
            }
        }
        
    }

    public void delete(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            return;
        }
        String channel = args;
        
    }

    public void create(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a channel name");
            return;
        }
        String channel = args;


        if(server.findChannel(channel)!=null)
            client.sendMessage("Channel "+ channel+ " already exists");
        else
            server.createChannel(channel);
    }

    public void ping(Client client) {

    }

    public void list(Client client) {
        client.sendMessage(server.toString());
    }

    public void help(Client client) {
        String help = "Valid commands:\n" +
                "/join [channel]\n" +
                "/leave\n" +
                "/create [channel]\n" +
                "/kick [username]\n" +
                "/ping\n" +
                "/list\n" +
                "/help";
        client.sendMessage(help);
    }
}