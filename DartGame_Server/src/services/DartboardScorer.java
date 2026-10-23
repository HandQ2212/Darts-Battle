package services;
import config.DartboardLayout;
import enums.HitArea;

public class DartboardScorer {
    public HitArea calculateHitArea(double x, double y, int rotation) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Tọa độ không hợp lệ.");
        double d = Math.hypot(x, y);
        if (d > DartboardLayout.OUTBOARD_RADIUS) return HitArea.MISS;
        if (d <= DartboardLayout.BULLSEYE_RADIUS) return HitArea.BULLSEYE;
        if (d <= DartboardLayout.OUTER_BULL_RADIUS) return HitArea.OUTER_BULL;
        if (d >= DartboardLayout.DOUBLE_INNER_RADIUS) return HitArea.DOUBLE;
        if (d >= DartboardLayout.TRIPLE_INNER_RADIUS && d <= DartboardLayout.TRIPLE_OUTER_RADIUS) return HitArea.TRIPLE;
        return HitArea.SINGLE;
    }
    public int calculateDartScore(double x, double y, int rotation) {
        HitArea area = calculateHitArea(x, y, rotation);
        if (area == HitArea.MISS) return 0;
        if (area == HitArea.BULLSEYE) return 50;
        if (area == HitArea.OUTER_BULL) return 25;
        double angle = (Math.toDegrees(Math.atan2(y, x)) + 90 - rotation + 9) % 360;
        if (angle < 0) angle += 360;
        int base = DartboardLayout.BOARD_ORDER[(int)(angle / 18)];
        return base * (area == HitArea.DOUBLE ? 2 : area == HitArea.TRIPLE ? 3 : 1);
    }
}
