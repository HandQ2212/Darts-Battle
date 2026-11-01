package config;

/** Shared presentation duration; the server also gates throws until this delay ends. */
public final class TurnRotation {
    private TurnRotation() {}
    public static final long DURATION_MILLIS = 2800;
}
