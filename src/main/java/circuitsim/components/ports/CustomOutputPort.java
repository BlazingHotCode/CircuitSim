package circuitsim.components.ports;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.components.wiring.*;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * Output port component for custom component editors.
 */
public class CustomOutputPort extends SingleTerminalComponent {
    private static final int DEFAULT_SIZE = Grid.SIZE * 2;
    private boolean activeIndicator;

    public CustomOutputPort(int x, int y) {
        super(x, y, DEFAULT_SIZE, DEFAULT_SIZE, 0f, 0.5f);
        setDisplayName("Output");
        addProperty(new StringProperty("Port Name", this::getDisplayName, this::setDisplayName, false, true));
    }

    @Override
    protected boolean includeDefaultProperties() {
        return false;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    public void updateSimulation(java.util.List<Wire> wires) {
        // No simulation state changes needed for output ports.
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        g2.setColor(Colors.COMPONENT_STROKE);
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int half = Math.max(4, width / 3);
        Polygon triangle = new Polygon();
        triangle.addPoint(centerX - half, centerY - half);
        triangle.addPoint(centerX - half, centerY + half);
        triangle.addPoint(centerX + half, centerY);
        g2.fillPolygon(triangle);
        if (activeIndicator) {
            g2.setColor(new Color(220, 120, 60));
            int dotSize = Math.max(4, width / 5);
            g2.fillOval(centerX - dotSize / 2, centerY - dotSize / 2, dotSize, dotSize);
        }
        g2.setColor(Colors.COMPONENT_STROKE);
        java.awt.Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Math.max(9f, originalFont.getSize2D() * 0.75f)));
        String label = getDisplayName();
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int textX = x - textWidth - 6;
        int textY = y + height / 2 + (g2.getFontMetrics().getAscent() / 2) - 6;
        g2.drawString(label, textX, textY);
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }

    /**
     * Updates the active indicator state.
     */
    public void setActiveIndicator(boolean activeIndicator) {
        this.activeIndicator = activeIndicator;
    }
}
