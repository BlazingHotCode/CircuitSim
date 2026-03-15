package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import circuitsim.ui.TerminalSnap;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.List;

/**
 * Simplified NPN transistor.
 */
@BuiltinComponent(group = "Active", paletteName = "NPN Transistor", groupOrder = 35, paletteOrder = 10)
public class NpnTransistor extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 3;
    private static final float DEFAULT_GAIN = 100f;

    private float gain;
    private float computedCollectorEmitterVoltage;
    private float computedCollectorCurrent;
    private float computedBaseEmitterVoltage;
    private float previousBaseEmitterVoltage;

    public NpnTransistor(int x, int y) {
        this(x, y, DEFAULT_GAIN);
    }

    public NpnTransistor(int x, int y, float gain) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, 3);
        this.gain = gain;
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.25f);
        addConnectionPoint(1f, 0.75f);
        addProperty(new FloatProperty("Gain (beta)", this::getGain, this::setGain, true));
        addProperty(new ComputedFloatProperty("Vce (V)", this::getComputedCollectorEmitterVoltage, false));
        addProperty(new ComputedFloatProperty("Ic (A)", this::getComputedCollectorCurrent, false));
        addProperty(new ComputedFloatProperty("Vbe (V)", this::getComputedBaseEmitterVoltage, false));
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getComputedCollectorEmitterVoltage() {
        return computedCollectorEmitterVoltage;
    }

    public void setComputedCollectorEmitterVoltage(float computedCollectorEmitterVoltage) {
        this.computedCollectorEmitterVoltage = computedCollectorEmitterVoltage;
    }

    public float getComputedCollectorCurrent() {
        return computedCollectorCurrent;
    }

    public void setComputedCollectorCurrent(float computedCollectorCurrent) {
        this.computedCollectorCurrent = computedCollectorCurrent;
    }

    public float getComputedBaseEmitterVoltage() {
        return computedBaseEmitterVoltage;
    }

    public void setComputedBaseEmitterVoltage(float computedBaseEmitterVoltage) {
        this.computedBaseEmitterVoltage = computedBaseEmitterVoltage;
    }

    public float getPreviousBaseEmitterVoltage() {
        return previousBaseEmitterVoltage;
    }

    public void setPreviousBaseEmitterVoltage(float previousBaseEmitterVoltage) {
        this.previousBaseEmitterVoltage = previousBaseEmitterVoltage;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        List<ConnectionPoint> points = getConnectionPoints();
        if (points.size() < 3) {
            return;
        }
        Point baseTerminal = TerminalSnap.getSnappedLocalTerminal(this, points.get(0));
        Point collectorTerminal = TerminalSnap.getSnappedLocalTerminal(this, points.get(1));
        Point emitterTerminal = TerminalSnap.getSnappedLocalTerminal(this, points.get(2));

        int centerY = (collectorTerminal.y + emitterTerminal.y) / 2;
        int bodyLeftX = baseTerminal.x + Math.max(Grid.SIZE / 4, width / 6);
        int branchMeetX = bodyLeftX + Math.max(Grid.SIZE / 3, width / 3);
        int collectorMeetY = centerY - Math.max(Grid.SIZE / 5, height / 8);
        int emitterMeetY = centerY + Math.max(Grid.SIZE / 5, height / 8);
        int radius = Math.max(8, Math.min(width, height) / 3);
        int circleCenterX = (bodyLeftX + branchMeetX) / 2;
        int circleCenterY = centerY;
        g2.drawOval(circleCenterX - radius, circleCenterY - radius, radius * 2, radius * 2);
        g2.drawLine(baseTerminal.x, baseTerminal.y, bodyLeftX, centerY);
        g2.drawLine(bodyLeftX, centerY - Math.max(Grid.SIZE / 3, height / 5), bodyLeftX,
                centerY + Math.max(Grid.SIZE / 3, height / 5));
        g2.drawLine(bodyLeftX, centerY, branchMeetX, collectorMeetY);
        g2.drawLine(branchMeetX, collectorMeetY, collectorTerminal.x, collectorTerminal.y);
        g2.drawLine(bodyLeftX, centerY, branchMeetX, emitterMeetY);
        g2.drawLine(branchMeetX, emitterMeetY, emitterTerminal.x, emitterTerminal.y);

        int arrowSize = Math.max(6, width / 7);
        double emitterDx = emitterTerminal.x - branchMeetX;
        double emitterDy = emitterTerminal.y - emitterMeetY;
        double emitterLength = Math.hypot(emitterDx, emitterDy);
        if (emitterLength < 1.0) {
            return;
        }
        double emitterUx = emitterDx / emitterLength;
        double emitterUy = emitterDy / emitterLength;
        double emitterNx = -emitterUy;
        double emitterNy = emitterUx;
        int arrowTipX = (int) Math.round(emitterMeetX(branchMeetX, emitterTerminal.x, 0.72));
        int arrowTipY = (int) Math.round(emitterMeetY(emitterMeetY, emitterTerminal.y, 0.72));
        int arrowBaseCenterX = (int) Math.round(emitterMeetX(branchMeetX, emitterTerminal.x, 0.48));
        int arrowBaseCenterY = (int) Math.round(emitterMeetY(emitterMeetY, emitterTerminal.y, 0.48));
        Polygon arrow = new Polygon();
        arrow.addPoint(arrowTipX, arrowTipY);
        arrow.addPoint(
                (int) Math.round(arrowBaseCenterX + (emitterNx * (arrowSize * 0.5))),
                (int) Math.round(arrowBaseCenterY + (emitterNy * (arrowSize * 0.5))));
        arrow.addPoint(
                (int) Math.round(arrowBaseCenterX - (emitterNx * (arrowSize * 0.5))),
                (int) Math.round(arrowBaseCenterY - (emitterNy * (arrowSize * 0.5))));
        g2.fillPolygon(arrow);
    }

    private static double emitterMeetX(int start, int end, double t) {
        return start + ((end - start) * t);
    }

    private static double emitterMeetY(int start, int end, double t) {
        return start + ((end - start) * t);
    }
}
