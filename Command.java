import java.util.*;
public class Command {
    private RedShiftServer server;

    public Command(RedShiftServer server) {
        this.server = server;
    }

    public void parseCommand(String commandTxt, Client client) {
        List<String> commandList = commandTxt.split(" ");
        List<String> arguments = commandList.sublist(1, commandList.size());
        String command = commandList.get(0);
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
            case "/list":
                list();
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
            case default:
                help(client);
                break;
        }
    }

    public void isCommand(String message) {
        if(message!=null && message.startsWith("/"))
            return true;
        return false;
    }

    public void leave(Client client) {

    }

    public void join(Client client, List<String> args) {
        if(args.size() != 1) {
            client.send("Please specify a channel to join");
            return;
        }
        String channel = args.get(0);
        Channel next = server.findChannel(channel);
        if(next!= null)
            client.joinChannel(next);
        else
            client.sendMessage("Could not find channel "+channel);
    }

    public void kick(Client client, List<String> args) {
        if(args.size() != 1) {
            //todo: invalid arg message
            client.send("Please specify a user");
            return;
        }
        Channel channel = client.getCurrentChannel();
        String user = args.get(0);
        if(channel!=null) {
            Client kicked = channel.findClient(user);
            if(kicked==null)
                client.sendMessage(String.format("Could not find user '%s' in current channel. Try /list for list of current users"), user);
            else{
                channel.removeClient(kicked);
                kicked.sendMessage(String.format("You were removed from %s by %s. Try /join [channel] to join a new channel or reconnect"), channel.getName(), client.getHandle());
            }
        }
        String user = args.get(0);
    }

    public void delete(Client client, String args) {
        if(args==null) {
            //todo: invalid arg message
            client.sendMessage("Please specify a channel to delete");
            return;
        }
        String channelName = args;
        Channel channel = server.findChannel(channelName);
        if(channel==null)
            client.sendMessage(String.format("Channel %s cannot be deleted because it does not exist", channelName));
        else{
            if(channel == server.getDefaultChannel()) {
                client.sendMessage("Cannot delete default channel");
                return;
            }
            channel.broadcast(String.format("Channel %s has been deleted by %s", channelName, client.getHandle()));
            server.deleteChannel(channel);
        }


    }

    public void create(Client client, List<String> args) {
        if(args.size() != 1) {
            //todo: invalid arg message
            client.sendMessage("Please specify a channel name");
            return;
        }
        String channel = args.get(0);


        if(server.findChannel(channel)!=null)
            client.sendMessage("Channel "+ channel+ " already exists");
        else
            server.createChannel(channel);
    }


    public void list(Client client) {
        client.sendMessage(server);
    }

    public void help(Client client) {
        String help = "Valid commands:\n" +
                "/join [channel]\n" +
                "/leave\n" +
                "/create [channel]\n" +
                "/kick [username]\n" +
                "/serenade\n" +
                "/list\n" +
                "/help";
        client.sendMessage(help);
    }
}