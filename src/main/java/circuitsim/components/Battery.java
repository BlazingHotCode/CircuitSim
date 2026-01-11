package circuitsim.components;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;

public class Battery extends CircuitComponent {
    private static final float TOP_PAD_RATIO = 0.5f;
    private static final float BOTTOM_PAD_RATIO = 1f / 3f;
    private static final float TOTAL_HEIGHT_RATIO = 1f + TOP_PAD_RATIO + BOTTOM_PAD_RATIO;
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 2;
    private static final float DEFAULT_VOLTAGE = 1.5f;
    private static final float DEFAULT_INTERNAL_RESISTANCE = 0.2f;

    private float voltage;
    private float internalResistance;
    private int internalNodeIndex = -1;
    private int positiveNodeIndex = -1;

    public Battery(int x, int y) {
        this(x, y, DEFAULT_VOLTAGE, DEFAULT_INTERNAL_RESISTANCE);
    }

    public Battery(int x, int y, float voltage, float internalResistance) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        this.voltage = voltage;
        this.internalResistance = internalResistance;
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        addProperty(new FloatProperty("Voltage (V)", this::getVoltage, this::setVoltage, true));
        addProperty(new FloatProperty("Internal Resistance (Ω)",
                this::getInternalResistance, this::setInternalResistance, true));
    }

    public float getVoltage() {
        return voltage;
    }

    public void setVoltage(float voltage) {
        this.voltage = voltage;
    }

    public float getInternalResistance() {
        return internalResistance;
    }

    public void setInternalResistance(float internalResistance) {
        this.internalResistance = internalResistance;
    }

    public ConnectionPoint getNegativePoint() {
        if (getConnectionPoints().isEmpty()) {
            return null;
        }
        return getConnectionPoints().get(0);
    }

    public ConnectionPoint getPositivePoint() {
        if (getConnectionPoints().size() < 2) {
            return null;
        }
        return getConnectionPoints().get(1);
    }

    public int getInternalNodeIndex() {
        return internalNodeIndex;
    }

    public void setInternalNodeIndex(int internalNodeIndex) {
        this.internalNodeIndex = internalNodeIndex;
    }

    public int getPositiveNodeIndex() {
        return positiveNodeIndex;
    }

    public void setPositiveNodeIndex(int positiveNodeIndex) {
        this.positiveNodeIndex = positiveNodeIndex;
    }

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
