package protocol.dto;
import enums.BotDifficulty;
import java.io.Serializable;
public record BotMatchRequest(BotDifficulty difficulty) implements Serializable {}
