package circuitsim.components.electrical;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Battery component with voltage and internal resistance.
 */
@BuiltinComponent(group = "Sources", groupOrder = 10, paletteOrder = 10)
public class Battery extends TwoTerminalComponent {
    private static final float TOP_PAD_RATIO = 0.5f;
    private static final float BOTTOM_PAD_RATIO = 1f / 3f;
    private static final float TOTAL_HEIGHT_RATIO = 1f + TOP_PAD_RATIO + BOTTOM_PAD_RATIO;
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_VOLTAGE = 1.5f;
    private static final float DEFAULT_INTERNAL_RESISTANCE = 0.2f;

    private float voltage;
    private float internalResistance;
    private int internalNodeIndex = -1;
    private int positiveNodeIndex = -1;

    /**
     * Creates a battery with default settings.
     */
    public Battery(int x, int y) {
        this(x, y, DEFAULT_VOLTAGE, DEFAULT_INTERNAL_RESISTANCE);
    }

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param voltage initial voltage
     * @param internalResistance initial internal resistance
     */
    public Battery(int x, int y, float voltage, float internalResistance) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.voltage = voltage;
        this.internalResistance = internalResistance;
        addProperty(new FloatProperty("Voltage (V)", this::getVoltage, this::setVoltage, true));
        addProperty(new FloatProperty("Internal Resistance (Ω)",
                this::getInternalResistance, this::setInternalResistance, true));
    }

    /**
     * @return battery voltage
     */
    public float getVoltage() {
        return voltage;
    }

    /**
     * Sets the battery voltage.
     */
    public void setVoltage(float voltage) {
        this.voltage = voltage;
    }

    /**
     * @return internal resistance
     */
    public float getInternalResistance() {
        return internalResistance;
    }

    /**
     * Sets the internal resistance.
     */
    public void setInternalResistance(float internalResistance) {
        this.internalResistance = internalResistance;
    }

    /**
     * @return negative terminal connection point
     */
    public ConnectionPoint getNegativePoint() {
        return getLeftPoint();
    }

    /**
     * @return positive terminal connection point
     */
    public ConnectionPoint getPositivePoint() {
        return getRightPoint();
    }

    /**
     * @return index of the internal node used by the solver
     */
    public int getInternalNodeIndex() {
        return internalNodeIndex;
    }

    /**
     * Sets the internal node index used by the solver.
     */
    public void setInternalNodeIndex(int internalNodeIndex) {
        this.internalNodeIndex = internalNodeIndex;
    }

    /**
     * @return index of the positive node used by the solver
     */
    public int getPositiveNodeIndex() {
        return positiveNodeIndex;
    }

    /**
     * Sets the positive node index used by the solver.
     */
    public void setPositiveNodeIndex(int positiveNodeIndex) {
        this.positiveNodeIndex = positiveNodeIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int bodyHeight = Math.max(1, Math.round(height / TOTAL_HEIGHT_RATIO));
        int topPad = Math.round(bodyHeight * TOP_PAD_RATIO);
        int bodyY = y + topPad;
        int bodyX = x;
        int bodyWidth = width;
        g2.drawRect(bodyX, bodyY, bodyWidth, bodyHeight);
        FontMetrics metrics = g2.getFontMetrics();
        int textY = Math.max(y + metrics.getAscent(), y + Math.round(bodyHeight * 0.2f));
        g2.drawLine(bodyX + (bodyWidth / 3), bodyY - (bodyHeight / 3),
                bodyX + (bodyWidth / 3), bodyY + bodyHeight + (bodyHeight / 3));
        g2.drawLine(bodyX + ((bodyWidth * 2) / 3), bodyY - (bodyHeight / 6),
                bodyX + ((bodyWidth * 2) / 3), bodyY + bodyHeight + (bodyHeight / 6));
        int plusX = bodyX + (bodyWidth / 5);
        int minusMargin = Math.max(2, Math.round(bodyWidth * 0.03f));
        int minusX = bodyX + ((bodyWidth * 2) / 3) - metrics.stringWidth("-") - minusMargin;
        minusX = Math.max(bodyX + 2, minusX);
        g2.drawString("+", plusX, textY);
        g2.drawString("-", minusX, textY);
    }
}
