package services;
import enums.PlayerStatus;
import protocol.*;
import protocol.dto.GameMessages.OnlineUser;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    private static final ClientManager INSTANCE = new ClientManager();
    private final ConcurrentHashMap<Long, Client> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PlayerStatus> statuses = new ConcurrentHashMap<>();
    private ClientManager() {}
    public static ClientManager getInstance() { return INSTANCE; }
    public boolean addClient(Client client) {
        if (clients.putIfAbsent(client.getLoggedInUserId(), client) != null) return false;
        statuses.put(client.getLoggedInUserId(), PlayerStatus.IDLE); return true;
    }
    public boolean removeClient(Client client) {
        long id = client.getLoggedInUserId();
        if (!clients.remove(id, client)) return false;
        statuses.remove(id); return true;
    }
    public Client getClient(long id) { return clients.get(id); }
    public Client findByName(String name) {
        return clients.values().stream().filter(c -> c.getLoggedInUsername().equals(name)).findFirst().orElse(null);
    }
    public void setStatus(long id, PlayerStatus status) {
        statuses.computeIfPresent(id, (key, old) -> status);
    }
    public PlayerStatus getStatus(long id) { return statuses.get(id); }
    public void sendToUser(long id, Message m) { Client c = clients.get(id); if (c != null) c.send(m); }
    public ArrayList<OnlineUser> onlineList() {
        ArrayList<OnlineUser> result = new ArrayList<>();
        clients.forEach((id, c) -> result.add(new OnlineUser(id, c.getLoggedInUsername(), statuses.getOrDefault(id, PlayerStatus.IDLE))));
        result.sort(Comparator.comparing(OnlineUser::username)); return result;
    }
    public void broadcastOnlineList() {
        Message m = Message.of(MessageType.ONLINE_LIST, onlineList()); clients.values().forEach(c -> c.send(m));
    }
}
