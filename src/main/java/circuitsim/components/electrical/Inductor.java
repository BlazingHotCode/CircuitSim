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
 * Inductor component with transient current behavior.
 */
@BuiltinComponent(group = "Passive", groupOrder = 30, paletteOrder = 40)
public class Inductor extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_INDUCTANCE_HENRY = 0.01f;

    private float inductanceHenry;
    private float computedVoltage;
    private float computedAmpere;
    private float previousCurrent;

    public Inductor(int x, int y) {
        this(x, y, DEFAULT_INDUCTANCE_HENRY);
    }

    public Inductor(int x, int y, float inductanceHenry) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.inductanceHenry = inductanceHenry;
        addProperty(new FloatProperty("Inductance (H)", this::getInductanceHenry, this::setInductanceHenry, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
        addProperty(new ComputedFloatProperty("Stored Energy (J)", this::getStoredEnergyJoule, false));
    }

    public float getInductanceHenry() {
        return inductanceHenry;
    }

    public void setInductanceHenry(float inductanceHenry) {
        this.inductanceHenry = inductanceHenry;
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

    public float getPreviousCurrent() {
        return previousCurrent;
    }

    public void setPreviousCurrent(float previousCurrent) {
        this.previousCurrent = previousCurrent;
    }

    public float getStoredEnergyJoule() {
        float inductance = Math.max(0f, inductanceHenry);
        float current = Math.abs(computedAmpere);
        return 0.5f * inductance * current * current;
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

        double lead = Math.max(Grid.SIZE * 0.55, length * 0.14);
        double coilStart = lead;
        double coilEnd = length - lead;
        int loops = 4;
        double loopSpan = Math.max(Grid.SIZE * 0.45, (coilEnd - coilStart) / loops);
        double radius = loopSpan / 2.0;
        double amplitude = Math.max(Grid.SIZE * 0.35, Math.min(width, height) * 0.28);

        double leadStartX = sx + (ux * coilStart);
        double leadStartY = sy + (uy * coilStart);
        double leadEndX = sx + (ux * (coilStart + (loopSpan * loops)));
        double leadEndY = sy + (uy * (coilStart + (loopSpan * loops)));
        g2.drawLine(sx, sy, (int) Math.round(leadStartX), (int) Math.round(leadStartY));
        g2.drawLine((int) Math.round(leadEndX), (int) Math.round(leadEndY), ex, ey);

        for (int i = 0; i < loops; i++) {
            double center = coilStart + radius + (i * loopSpan);
            int arcX = (int) Math.round(sx + (ux * (center - radius)) - (nx * amplitude));
            int arcY = (int) Math.round(sy + (uy * (center - radius)) - (ny * amplitude));
            int arcW = (int) Math.round(loopSpan);
            int arcH = (int) Math.round(amplitude * 2.0);
            double angle = Math.toDegrees(Math.atan2(uy, ux));
            g2.rotate(angle, sx + (ux * center), sy + (uy * center));
            g2.drawArc(arcX, arcY, arcW, arcH, 0, 180);
            g2.rotate(-angle, sx + (ux * center), sy + (uy * center));
        }
    }
}
