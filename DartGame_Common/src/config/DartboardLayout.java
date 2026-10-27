package config;
/** Screen coordinates relative to center; positive Y points down, positive rotation is clockwise. */
public final class DartboardLayout {
    private DartboardLayout() {}
    public static final int[] BOARD_ORDER = {20,1,18,4,13,6,10,15,2,17,3,19,7,16,8,11,14,9,12,5};
    public static final double CENTER_X = 0, CENTER_Y = 0;
    public static final double OUTBOARD_RADIUS = 160, DOUBLE_OUTER_RADIUS = 160, DOUBLE_INNER_RADIUS = 140;
    public static final double TRIPLE_OUTER_RADIUS = 100, TRIPLE_INNER_RADIUS = 80;
    public static final double OUTER_BULL_RADIUS = 25, BULLSEYE_RADIUS = 12;
}
