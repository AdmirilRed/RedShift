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

            case "/join":
                join(client, arguments);
                break;
            case "/create":
                create(client, arguments);
                break;
            case "/delete":
                delete(client, arguments);
                break;
            case "/kick":
                kick(client, arguments);
                break;
            case "/list":
                list(client);
                break;
            case "/serenade":
                Channel chan = client.getCurrentChannel();
                if(chan!=null)
                    chan.broadcast("Country rooooad\n" +
                        "take me hooooome\n" +
                        "to the plaaaaaaaaaace\n" +
                        "where i beloooonngg");
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

    public void join(Client client, String args) {
        if(args==null) {
            client.sendMessage("Please specify a channel to join.");
            return;
        }
        String channel = args;
        Channel next = server.findChannel(channel);
        if(next!= null)
            client.joinChannel(next);
        else
            client.sendMessage("Could not find channel "+channel+".");
    }

    public void kick(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a user.");
            return;
        }
        Channel channel = client.getCurrentChannel();
        String user = args;
        if(channel!=null) {
            Client kicked = channel.findClient(user);
            if(kicked==null)
                client.sendMessage(String.format("Could not find user '%s' in current channel. Try /list for list of current users.", user));
            else{
                channel.removeClient(kicked);
                kicked.sendMessage(String.format("You were removed from %s by %s. Try /join [channel] to join a new channel or reconnect.", channel.getName(), client.getHandle()));
            }
        }
        
    }

    public void delete(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a channel to delete.");
            return;
        }
        String channelName = args;
        Channel channel = server.findChannel(channelName);
        if(channel==null)
            client.sendMessage(String.format("Channel %s cannot be deleted because it does not exist.", channelName));
        else{
            if(channel == server.getDefaultChannel()) {
                client.sendMessage("Cannot delete default channel.");
                return;
            }
            channel.broadcast(String.format("Channel %s has been deleted by %s.", channelName, client.getHandle()));
            server.deleteChannel(channel);
        }
    }

    public void create(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a channel name");
            return;
        }
        String channel = args;


        if(server.findChannel(channel)!=null)
            client.sendMessage("Channel "+channel+ " already exists.");
        else
            server.createChannel(channel);
            client.sendMessage("Channel "+channel+" created.");
    }


    public void list(Client client) {
        client.sendMessage(server.toString());
    }

    public void help(Client client) {
        String help = "Valid commands:\n" +
                "/join [channel]\n" +
                "/create [channel]\n" +
                "/delete [channel]\n" +
                "/kick [username]\n" +
                "/serenade\n" +
                "/list\n" +
                "/help";
        client.sendMessage(help);
    }
}