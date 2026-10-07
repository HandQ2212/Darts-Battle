package protocol;

import java.io.Serializable;

public class Message implements Serializable {
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
