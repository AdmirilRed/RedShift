import java.util.*;
public class Command {
    private RedShiftServer server;

    public Command(RedShiftServer server) {
        this.server = server;
    }

    public void parseCommand(String commandTxt) {
        List<String> commandList = commandTxt.split(" ");
        List<String> arguments = commandList.sublist(1, commandList.size());
        String command = commandList.get(0);
        switch(command) {
            case "/leave":
                leave();
                break;
            case "/join":
                join(arguments);
                break;
            case "/create":
                create(arguments);
                break;
            case "/kick":
                kick(arguments);
                break;
            case "/ping":
                ping();
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

    public void leave() {

    }

    public void join(List<String> args) {
        if(args.size() != 1) {
            //todo: invalid arg message
            return;
        }
        String channel = args.get(0);
    }

    public void kick(List<String> args) {
        if(args.size() != 1) {
            //todo: invalid arg message
            return;
        }
        String user = args.get(0);
    }

    public void delete(List<String> args) {
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

    public void create(List<String> args) {
        if(args.size() != 2) {
            //todo: invalid arg message
            return;
        }
        String channel = args.get(0);
        String password = args.get(1);

        if(server.validate(password)){
            server.create(channel);
        } else {
            //error message
        }
    }

    public void ping() {

    }

    public void list() {

    }

    public void help() {

    }
}