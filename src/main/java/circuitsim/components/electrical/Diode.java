package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.core.TwoTerminalComponent;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import circuitsim.ui.TerminalSnap;
import java.awt.Graphics2D;

/**
 * Simple diode with a configurable forward voltage drop.
 */
@BuiltinComponent(group = "Passive", groupOrder = 30, paletteOrder = 25)
public class Diode extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_FORWARD_VOLTAGE = 0.7f;

    private float forwardVoltage;
    private float computedVoltage;
    private float computedAmpere;
    private float previousVoltage;

    public Diode(int x, int y) {
        this(x, y, DEFAULT_FORWARD_VOLTAGE);
    }

    public Diode(int x, int y, float forwardVoltage) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.forwardVoltage = forwardVoltage;
        addProperty(new FloatProperty("Forward Voltage (V)", this::getForwardVoltage, this::setForwardVoltage, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
    }

    public float getForwardVoltage() {
        return forwardVoltage;
    }

    public void setForwardVoltage(float forwardVoltage) {
        this.forwardVoltage = forwardVoltage;
    }

    public float getComputedVoltage() {
        return computedVoltage;
    }

    public void setComputedVoltage(float computedVoltage) {
        this.computedVoltage = computedVoltage;
    }

    public float getComputedAmpere() {
        return computedAmpere;
    }

    public void setComputedAmpere(float computedAmpere) {
        this.computedAmpere = computedAmpere;
    }

    public float getPreviousVoltage() {
        return previousVoltage;
    }

    public void setPreviousVoltage(float previousVoltage) {
        this.previousVoltage = previousVoltage;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().size() < 2) {
            return;
        }
        ConnectionPoint start = getConnectionPoints().get(0);
        ConnectionPoint end = getConnectionPoints().get(1);
        java.awt.Point snappedStart = TerminalSnap.getSnappedLocalTerminal(this, start);
        java.awt.Point snappedEnd = TerminalSnap.getSnappedLocalTerminal(this, end);
        int sx = snappedStart.x;
        int sy = snappedStart.y;
        int ex = snappedEnd.x;
        int ey = snappedEnd.y;

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

        double lead = Math.max(Grid.SIZE * 0.8, length * 0.22);
        double cathodeOffset = Math.max(Grid.SIZE * 0.35, length * 0.12);
        double centerX = (sx + ex) / 2.0;
        double centerY = (sy + ey) / 2.0;
        double triangleTipX = centerX + (ux * cathodeOffset);
        double triangleTipY = centerY + (uy * cathodeOffset);
        double triangleBaseX = centerX - (ux * (cathodeOffset * 2.0));
        double triangleBaseY = centerY - (uy * (cathodeOffset * 2.0));
        double cathodeX = centerX + (ux * cathodeOffset);
        double cathodeY = centerY + (uy * cathodeOffset);
        double halfWidth = Math.max(Grid.SIZE * 0.45, Math.min(width, height) * 0.35);

        double startLeadX = sx + (ux * lead);
        double startLeadY = sy + (uy * lead);
        double endLeadX = ex - (ux * lead);
        double endLeadY = ey - (uy * lead);

        g2.drawLine(sx, sy, (int) Math.round(startLeadX), (int) Math.round(startLeadY));
        g2.drawLine((int) Math.round(cathodeX), (int) Math.round(cathodeY), ex, ey);

        int[] xs = new int[] {
                (int) Math.round(triangleTipX),
                (int) Math.round(triangleBaseX + (nx * halfWidth)),
                (int) Math.round(triangleBaseX - (nx * halfWidth))
        };
        int[] ys = new int[] {
                (int) Math.round(triangleTipY),
                (int) Math.round(triangleBaseY + (ny * halfWidth)),
                (int) Math.round(triangleBaseY - (ny * halfWidth))
        };
        g2.drawPolygon(xs, ys, 3);
        g2.drawLine(
                (int) Math.round(cathodeX + (nx * halfWidth)),
                (int) Math.round(cathodeY + (ny * halfWidth)),
                (int) Math.round(cathodeX - (nx * halfWidth)),
                (int) Math.round(cathodeY - (ny * halfWidth)));
        g2.drawLine((int) Math.round(cathodeX), (int) Math.round(cathodeY), (int) Math.round(endLeadX), (int) Math.round(endLeadY));
    }
}
