package services;

import models.Room;
import enums.ActorType;
import enums.MatchMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userRoomMap = new ConcurrentHashMap<>();

    private RoomManager() {}
    public static RoomManager getInstance() { return instance; }

    public synchronized Room createPvpRoom(long p1, long p2) {
        if (p1 == p2 || p2 <= 0 || findRoomOfUser(p2) != null) throw new IllegalArgumentException("Đối thủ đang bận hoặc không hợp lệ.");
        if (p1 <= 0 || findRoomOfUser(p1) != null) throw new IllegalArgumentException("Người chơi đang bận.");
        String matchId = UUID.randomUUID().toString();
        Room room = new Room(matchId, p1, p2, ActorType.HUMAN, MatchMode.PVP);
        rooms.put(matchId, room);
        userRoomMap.put(p1, matchId);
        userRoomMap.put(p2, matchId);
        return room;
    }

    public synchronized Room createPveRoom(long p1) {
        if (p1 <= 0 || findRoomOfUser(p1) != null) throw new IllegalArgumentException("Người chơi đang bận.");
        String matchId = UUID.randomUUID().toString();
        Room room = new Room(matchId, p1, null, ActorType.BOT, MatchMode.PVE);
        rooms.put(matchId, room);
        userRoomMap.put(p1, matchId);
        return room;
    }

    public Room getRoom(String matchId) {
        return matchId == null ? null : rooms.get(matchId);
    }

    public java.util.List<Room> allRooms() { return new java.util.ArrayList<>(rooms.values()); }

    public Room findRoomOfUser(long userId) {
        String matchId = userRoomMap.get(userId);
        if (matchId != null) {
            return rooms.get(matchId);
        }
        return null;
    }

    public synchronized void removeRoom(String matchId) {
        Room room = rooms.remove(matchId);
        if (room != null) {
            userRoomMap.remove(room.getPlayer1Id(), matchId);
            if (room.getPlayer2Id() != null) {
                userRoomMap.remove(room.getPlayer2Id(), matchId);
            }
        }
    }
}
