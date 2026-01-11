package circuitsim.components;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;

public class Resistor extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 2;
    private static final float DEFAULT_OHMS = 1f;

    private float resistance;
    private float computedVoltage;
    private float computedAmpere;

    public Resistor(int x, int y) {
        this(x, y, DEFAULT_OHMS);
    }

    public Resistor(int x, int y, float resistance) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        this.resistance = resistance;
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        addProperty(new FloatProperty("Resistance (Ω)", this::getResistance, this::setResistance, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
    }

    public float getResistance() {
        return resistance;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
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

    @Override
    public int getConnectionDotSize() {
        return Math.max(6, super.getConnectionDotSize());
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().size() < 2) {
            return;
        }
        ConnectionPoint start = getConnectionPoints().get(0);
        ConnectionPoint end = getConnectionPoints().get(1);
        int sx = start.getX();
        int sy = start.getY();
        int ex = end.getX();
        int ey = end.getY();

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

        int zigzagCount = 6;
        double lead = Math.max(Grid.SIZE / 2.0, length * 0.15);
        double zigStart = lead;
        double zigEnd = length - lead;
        double step = (zigEnd - zigStart) / zigzagCount;
        double maxAmplitude = (Math.min(width, height) / 2.0) - 4;
        double amplitude = Math.max(Grid.SIZE * 0.4, Math.min(width, height) * 0.4);
        amplitude = Math.max(0, Math.min(amplitude, maxAmplitude));

        double lastX = sx + (ux * zigStart);
        double lastY = sy + (uy * zigStart);
        g2.drawLine(sx, sy, (int) Math.round(lastX), (int) Math.round(lastY));
        int direction = -1;
        for (int i = 1; i <= zigzagCount; i++) {
            double t = zigStart + (step * i);
            double offset = amplitude * direction;
            double zx = sx + (ux * t) + (nx * offset);
            double zy = sy + (uy * t) + (ny * offset);
            g2.drawLine((int) Math.round(lastX), (int) Math.round(lastY),
                    (int) Math.round(zx), (int) Math.round(zy));
            lastX = zx;
            lastY = zy;
            direction *= -1;
        }
        g2.drawLine((int) Math.round(lastX), (int) Math.round(lastY), ex, ey);
    }
}
