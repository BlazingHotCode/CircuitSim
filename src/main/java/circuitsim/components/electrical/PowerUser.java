package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.TwoTerminalComponent;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Constant-power load component.
 */
@BuiltinComponent(group = "Passive", paletteName = "Power User", groupOrder = 30, paletteOrder = 20)
public class PowerUser extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 2;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_POWER_WATT = 1f;

    private float targetPowerWatt;
    private float computedVoltage;
    private float computedAmpere;
    private float computedPowerWatt;
    private float computedResistance;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public PowerUser(int x, int y) {
        this(x, y, DEFAULT_POWER_WATT);
    }

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param targetPowerWatt desired power draw in watts
     */
    public PowerUser(int x, int y, float targetPowerWatt) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.targetPowerWatt = targetPowerWatt;
        addProperty(new FloatProperty("Power (W)", this::getTargetPowerWatt, this::setTargetPowerWatt, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
        addProperty(new ComputedFloatProperty("Actual Power (W)", this::getComputedPowerWatt, false));
        addProperty(new ComputedFloatProperty("Effective Resistance (Ω)", this::getComputedResistance, false));
    }

    /**
     * @return target wattage draw
     */
    public float getTargetPowerWatt() {
        return targetPowerWatt;
    }

    /**
     * Sets target wattage draw.
     */
    public void setTargetPowerWatt(float targetPowerWatt) {
        this.targetPowerWatt = targetPowerWatt;
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

    public float getComputedPowerWatt() {
        return computedPowerWatt;
    }

    public void setComputedPowerWatt(float computedPowerWatt) {
        this.computedPowerWatt = computedPowerWatt;
    }

    public float getComputedResistance() {
        return computedResistance;
    }

    public void setComputedResistance(float computedResistance) {
        this.computedResistance = computedResistance;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int size = Math.max(6, Math.min(width, height) - 4);
        int cx = x + (width / 2);
        int cy = y + (height / 2);
        int left = cx - (size / 2);
        int top = cy - (size / 2);
        g2.drawOval(left, top, size, size);

        int pad = Math.max(3, Math.round(size * 0.2f));
        int x1 = left + pad;
        int y1 = top + pad;
        int x2 = left + size - pad;
        int y2 = top + size - pad;
        g2.drawLine(x1, y1, x2, y2);
        g2.drawLine(x1, y2, x2, y1);
    }
}
