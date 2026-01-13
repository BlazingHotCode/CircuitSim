package circuitsim.components.instruments;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.components.wiring.*;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Ammeter component that displays computed current.
 */
public class Ammeter extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String UNIT_LABEL = "A";

    private float computedAmpere;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public Ammeter(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
    }

    /**
     * @return computed current
     */
    public float getComputedAmpere() {
        return computedAmpere;
    }

    /**
     * Sets computed current.
     */
    public void setComputedAmpere(float computedAmpere) {
        this.computedAmpere = computedAmpere;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int arc = Math.max(8, Math.min(width, height) / 3);
        g2.drawRoundRect(x, y, width, height, arc, arc);
        String label = VALUE_FORMAT.format(computedAmpere) + " " + UNIT_LABEL;
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
