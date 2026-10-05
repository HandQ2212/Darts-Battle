package services;

import config.DartboardLayout;
import enums.HitArea;

public class DartboardScorer {

    public HitArea calculateHitArea(double x, double y, int rotationDegree) {
        double distance = Math.sqrt(x * x + y * y);

        if (distance > DartboardLayout.OUTBOARD_RADIUS) {
            return HitArea.MISS;
        }
        if (distance <= DartboardLayout.BULLSEYE_RADIUS) {
            return HitArea.BULLSEYE;
        }
        if (distance <= DartboardLayout.OUTER_BULL_RADIUS) {
            return HitArea.OUTER_BULL;
        }
        if (distance > DartboardLayout.DOUBLE_INNER_RADIUS && distance <= DartboardLayout.DOUBLE_OUTER_RADIUS) {
            return HitArea.DOUBLE;
        }
        if (distance > DartboardLayout.TRIPLE_INNER_RADIUS && distance <= DartboardLayout.TRIPLE_OUTER_RADIUS) {
            return HitArea.TRIPLE;
        }
        return HitArea.SINGLE;
    }

    public int calculateDartScore(double x, double y, int rotationDegree) {
        HitArea hitArea = calculateHitArea(x, y, rotationDegree);
        if (hitArea == HitArea.MISS) return 0;
        if (hitArea == HitArea.BULLSEYE) return 50;
        if (hitArea == HitArea.OUTER_BULL) return 25;

        // Calculate angle
        double angle = Math.toDegrees(Math.atan2(y, x));
        if (angle < 0) {
            angle += 360.0;
        }
        
        // Apply rotation (if the board is rotated, the physical angle of the slice shifts)
        // rotationDegree is the clockwise rotation of the board
        double adjustedAngle = angle - rotationDegree;
        while (adjustedAngle < 0) adjustedAngle += 360.0;
        while (adjustedAngle >= 360.0) adjustedAngle -= 360.0;

        // The 20 segment is centered at 90 degrees (top) in standard math coordinates,
        // but let's assume 0 degrees is the right (positive X axis), which is the 6 segment in standard layout.
        // Actually, it's standard to map angles to segments. Each segment is 360/20 = 18 degrees.
        // Usually, 20 is at the top (90 degrees). So 20 covers 81 to 99 degrees.
        // Let's map adjustedAngle to the 20 slices.
        // Top is 90 degrees.
        double shiftedAngle = (adjustedAngle + 9.0 - 90.0) % 360.0;
        if (shiftedAngle < 0) shiftedAngle += 360.0;
        
        // BOARD_ORDER starts from 20 and goes clockwise.
        // But standard math angle (counter-clockwise) means index should increase counter-clockwise.
        // Let's just calculate index from angle directly.
        // In standard dartboard: 20 is at top, going clockwise: 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5
        // If angle is counter-clockwise, the order from top (90 deg) is: 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10, 6, 13, 4, 18, 1
        
        int[] ccwOrder = {20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10, 6, 13, 4, 18, 1};
        
        int sliceIndex = (int)(shiftedAngle / 18.0) % 20;
        int baseScore = ccwOrder[sliceIndex];

        if (hitArea == HitArea.DOUBLE) return baseScore * 2;
        if (hitArea == HitArea.TRIPLE) return baseScore * 3;
        
        return baseScore;
    }
}
