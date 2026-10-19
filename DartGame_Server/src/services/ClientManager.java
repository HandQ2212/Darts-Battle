package services;

import enums.PlayerStatus;
import protocol.Message;
import protocol.MessageType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ClientManager {
    private static final ClientManager instance = new ClientManager();
    private final ConcurrentHashMap<Long, Client> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PlayerStatus> statuses = new ConcurrentHashMap<>();

    private ClientManager() {}
    public static ClientManager getInstance() { return instance; }

    public void addClient(Client client) {
        if (client.getLoggedInUserId() != -1) {
            clients.put(client.getLoggedInUserId(), client);
            statuses.put(client.getLoggedInUserId(), PlayerStatus.IDLE);
            broadcastOnlineList();
        }
    }

    public void removeClient(long userId) {
        clients.remove(userId);
        statuses.remove(userId);
        broadcastOnlineList();
    }

    public Client getClient(long userId) {
        return clients.get(userId);
    }

    public void setStatus(long userId, PlayerStatus status) {
        if (statuses.containsKey(userId)) {
            statuses.put(userId, status);
            broadcastOnlineList();
        }
    }

    public PlayerStatus getStatus(long userId) {
        return statuses.get(userId);
    }

    public void sendToUser(long userId, Message message) {
        Client client = clients.get(userId);
        if (client != null) {
            client.send(message);
        }
    }

    public void broadcastOnlineList() {
        // Implementation to build online list and broadcast
    }
}
