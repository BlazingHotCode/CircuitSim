package circuitsim.components;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Voltmeter component that displays computed voltage.
 */
public class Voltmeter extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 2;
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String UNIT_LABEL = "V";

    private float computedVoltage;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public Voltmeter(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
    }

    /**
     * @return computed voltage
     */
    public float getComputedVoltage() {
        return computedVoltage;
    }

    /**
     * Sets computed voltage.
     */
    public void setComputedVoltage(float computedVoltage) {
        this.computedVoltage = computedVoltage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int arc = Math.max(8, Math.min(width, height) / 3);
        g2.drawRoundRect(x, y, width, height, arc, arc);
        String label = VALUE_FORMAT.format(computedVoltage) + " " + UNIT_LABEL;
        FontMetrics metrics = g2.getFontMetrics();
        int textWidth = metrics.stringWidth(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height + metrics.getAscent() - metrics.getDescent()) / 2;
        java.awt.geom.AffineTransform rotatedTransform = g2.getTransform();
        double angle = getRotationRadians();
        if (angle == 0) {
            g2.drawString(label, textX, textY);
            return;
        }
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        java.awt.Point textPoint = rotatePoint(textX, textY, centerX, centerY, angle);
        java.awt.geom.AffineTransform uprightTransform = new java.awt.geom.AffineTransform(rotatedTransform);
        uprightTransform.rotate(-angle, centerX, centerY);
        g2.setTransform(uprightTransform);
        g2.drawString(label, textPoint.x, textPoint.y);
        g2.setTransform(rotatedTransform);
    }

    /**
     * Rotates a point around a center by the provided angle.
     */
    private java.awt.Point rotatePoint(double x, double y, double centerX, double centerY, double angle) {
        double dx = x - centerX;
        double dy = y - centerY;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rotatedX = (dx * cos) - (dy * sin);
        double rotatedY = (dx * sin) + (dy * cos);
        return new java.awt.Point((int) Math.round(centerX + rotatedX),
                (int) Math.round(centerY + rotatedY));
    }
}
