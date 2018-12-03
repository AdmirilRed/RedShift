import java.util.*;
import java.lang.*;
import java.io.*;

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