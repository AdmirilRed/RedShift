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
            case "/ping":
                ping(client);
                break;
            case "/list":
                list();
                break;
            case "/help":
            case "?":
                help();
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
            return;
        }
        String user = args.get(0);
    }

    public void delete(Client client, List<String> args) {
        if(args.size() != 2) {
            //todo: invalid arg message
            return;
        }
        String channel = args.get(0);
        String password = args.get(1);
        if(server.validate(password)){
            //server.delete(channel);
        } else {
            //error message
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

    public void ping(Client client) {

    }

    public void list(Client client) {
        client.sendMessage(server);
    }

    public void help() {

    }
}