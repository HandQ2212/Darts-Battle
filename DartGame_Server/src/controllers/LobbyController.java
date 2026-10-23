package controllers;
import protocol.Message;
import services.*;
public class LobbyController {
    public void handleMessage(Client client, Message message) { LobbyService.getInstance().handle(client, message); }
}
