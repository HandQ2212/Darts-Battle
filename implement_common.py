import os

files = {
    "DartGame_Common/src/protocol/Message.java": """package protocol;

public class Message {
    private MessageType type;
    private String requestId;
    private Object payload;

    public Message() {
    }

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public static Message of(MessageType type, Object payload) {
        return new Message(type, payload);
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
""",
    "DartGame_Common/src/protocol/dto/LoginRequest.java": """package protocol.dto;

public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
""",
    "DartGame_Common/src/protocol/dto/ThrowCoordinateRequest.java": """package protocol.dto;

public class ThrowCoordinateRequest {
    private String matchId;
    private long turnId;
    private double x;
    private double y;

    public ThrowCoordinateRequest() {}

    public ThrowCoordinateRequest(String matchId, long turnId, double x, double y) {
        this.matchId = matchId;
        this.turnId = turnId;
        this.x = x;
        this.y = y;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public long getTurnId() {
        return turnId;
    }

    public void setTurnId(long turnId) {
        this.turnId = turnId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
""",
    "DartGame_Common/src/protocol/dto/ThrowResolvedResponse.java": """package protocol.dto;

import enums.ActorType;
import enums.HitArea;

public class ThrowResolvedResponse {
    private String matchId;
    private long turnId;
    private String actorName;
    private ActorType actorType;
    private int dartIndex;
    private double x;
    private double y;
    private int rotationDegree;
    private HitArea hitArea;
    private int dartScore;
    private int remainingScoreAfter;
    private boolean isBust;
    private boolean isTurnFinished;
    private boolean isMatchFinished;

    public ThrowResolvedResponse() {}

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public long getTurnId() { return turnId; }
    public void setTurnId(long turnId) { this.turnId = turnId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }

    public int getDartIndex() { return dartIndex; }
    public void setDartIndex(int dartIndex) { this.dartIndex = dartIndex; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public int getRotationDegree() { return rotationDegree; }
    public void setRotationDegree(int rotationDegree) { this.rotationDegree = rotationDegree; }

    public HitArea getHitArea() { return hitArea; }
    public void setHitArea(HitArea hitArea) { this.hitArea = hitArea; }

    public int getDartScore() { return dartScore; }
    public void setDartScore(int dartScore) { this.dartScore = dartScore; }

    public int getRemainingScoreAfter() { return remainingScoreAfter; }
    public void setRemainingScoreAfter(int remainingScoreAfter) { this.remainingScoreAfter = remainingScoreAfter; }

    public boolean isBust() { return isBust; }
    public void setBust(boolean bust) { isBust = bust; }

    public boolean isTurnFinished() { return isTurnFinished; }
    public void setTurnFinished(boolean turnFinished) { isTurnFinished = turnFinished; }

    public boolean isMatchFinished() { return isMatchFinished; }
    public void setMatchFinished(boolean matchFinished) { isMatchFinished = matchFinished; }
}
""",
    "DartGame_Common/src/config/DartboardLayout.java": """package config;

public class DartboardLayout {
    public static final int[] BOARD_ORDER = {20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5};
    
    // Default constants for dartboard dimensions (can be adjusted)
    public static final double CENTER_X = 0;
    public static final double CENTER_Y = 0;
    public static final double OUTBOARD_RADIUS = 170.0;
    public static final double DOUBLE_OUTER_RADIUS = 170.0;
    public static final double DOUBLE_INNER_RADIUS = 162.0;
    public static final double TRIPLE_OUTER_RADIUS = 107.0;
    public static final double TRIPLE_INNER_RADIUS = 99.0;
    public static final double OUTER_BULL_RADIUS = 31.8;
    public static final double BULLSEYE_RADIUS = 12.7;
}
"""
}

for filepath, content in files.items():
    with open(filepath, 'w') as f:
        f.write(content)

print("Common module implemented.")
