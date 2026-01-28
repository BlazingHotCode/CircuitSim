package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;

/**
 * Utility for mapping snapped world terminals back into the component's local drawing space.
 */
public final class TerminalSnap {
    private TerminalSnap() {
    }

    public static java.awt.Point getSnappedLocalTerminal(CircuitComponent owner, ConnectionPoint point) {
        if (owner == null || point == null) {
            return new java.awt.Point(0, 0);
        }
        double centerX = owner.getX() + (owner.getWidth() / 2.0);
        double centerY = owner.getY() + (owner.getHeight() / 2.0);
        double angle = owner.getRotationRadians();

        double localX = owner.getX() + (owner.getWidth() * point.getRelativeX());
        double localY = owner.getY() + (owner.getHeight() * point.getRelativeY());
        if (angle == 0.0) {
            return new java.awt.Point(Grid.snap((int) Math.round(localX)),
                    Grid.snap((int) Math.round(localY)));
        }

        double dx = localX - centerX;
        double dy = localY - centerY;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double worldX = centerX + ((dx * cos) - (dy * sin));
        double worldY = centerY + ((dx * sin) + (dy * cos));
        int snappedWorldX = Grid.snap((int) Math.round(worldX));
        int snappedWorldY = Grid.snap((int) Math.round(worldY));

        double wdx = snappedWorldX - centerX;
        double wdy = snappedWorldY - centerY;
        double invX = centerX + ((wdx * cos) + (wdy * sin));
        double invY = centerY + ((-wdx * sin) + (wdy * cos));
        return new java.awt.Point((int) Math.round(invX), (int) Math.round(invY));
    }
}

