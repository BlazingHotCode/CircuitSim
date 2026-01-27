package circuitsim.components.electrical;

import circuitsim.components.core.*;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * User-toggleable switch component.
 */
@BuiltinComponent(group = "Controls", paletteName = "Switch (User)", groupOrder = 50, paletteOrder = 10)
public class Switch extends TwoTerminalComponent implements SwitchLike {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 2;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;

    private boolean closed;
    private float computedAmpere;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public Switch(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, false);
        this.closed = false;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Sets whether the switch is closed.
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    /**
     * @return computed current for display
     */
    public float getComputedAmpere() {
        return computedAmpere;
    }

    @Override
    public void setComputedAmpere(float computedAmpere) {
        this.computedAmpere = computedAmpere;
    }

    /**
     * Toggles the switch state.
     */
    public void toggle() {
        closed = !closed;
    }

    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().size() < 2) {
            return;
        }
        ConnectionPoint start = getConnectionPoints().get(0);
        ConnectionPoint end = getConnectionPoints().get(1);
        // Draw using local points that map to the snapped terminal positions in world space.
        java.awt.Point snappedStart = getSnappedLocalTerminal(start);
        java.awt.Point snappedEnd = getSnappedLocalTerminal(end);
        int sx = snappedStart.x;
        int sy = snappedStart.y;
        int ex = snappedEnd.x;
        int ey = snappedEnd.y;
        if (closed) {
            g2.drawLine(sx, sy, ex, ey);
        } else {
            double dx = ex - sx;
            double dy = ey - sy;
            double length = Math.hypot(dx, dy);
            if (length < 1) {
                return;
            }
            double ux = dx / length;
            double uy = dy / length;
            double nx = -uy;
            double ny = ux;
            int gap = Math.max(6, Math.min(width, height) / 4);
            double midX = (sx + ex) / 2.0;
            double midY = (sy + ey) / 2.0;
            double leftX = midX - (ux * gap);
            double leftY = midY - (uy * gap);
            double rightX = midX + (ux * gap);
            double rightY = midY + (uy * gap);
            double leverOffset = Math.max(6, height / 4.0);
            double leverX = midX + (ux * (gap * 0.5)) - (nx * leverOffset);
            double leverY = midY + (uy * (gap * 0.5)) - (ny * leverOffset);
            g2.drawLine(sx, sy, (int) Math.round(leftX), (int) Math.round(leftY));
            g2.drawLine(ex, ey, (int) Math.round(rightX), (int) Math.round(rightY));
            g2.drawLine((int) Math.round(rightX), (int) Math.round(rightY),
                    (int) Math.round(leverX), (int) Math.round(leverY));
        }
        String label = "SW";
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + Math.max(4, width / 10);
        int textY = y + metrics.getAscent() + 2;
        g2.drawString(label, textX, textY);
    }

    private java.awt.Point getSnappedLocalTerminal(ConnectionPoint point) {
        if (point == null) {
            return new java.awt.Point(x, y);
        }
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        double angle = getRotationRadians();

        double localX = x + (width * point.getRelativeX());
        double localY = y + (height * point.getRelativeY());
        if (angle == 0.0) {
            return new java.awt.Point(Grid.snap((int) Math.round(localX)),
                    Grid.snap((int) Math.round(localY)));
        }

        // Forward-rotate local point, snap in world space, then inverse-rotate back into local space.
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
