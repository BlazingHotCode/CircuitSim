package circuitsim.components.ports;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.components.wiring.*;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Input port component for custom component editors.
 */
@BuiltinComponent(group = "IO", paletteName = "Input", groupOrder = 70, paletteOrder = 10)
public class CustomInputPort extends SingleTerminalComponent implements SwitchLike {
    private static final int DEFAULT_SIZE = Grid.SIZE * 2;
    private boolean active;

    public CustomInputPort(int x, int y) {
        super(x, y, DEFAULT_SIZE, DEFAULT_SIZE, 1f, 0.5f, false);
        setDisplayName("Input");
        addProperty(new StringProperty("Port Name", this::getDisplayName, this::setDisplayName, false, true));
        addProperty(new BooleanProperty("Active", this::isActive, this::setActive, false));
    }

    /**
     * @return true when the input is toggled on
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Updates the test toggle state.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isClosed() {
        return active;
    }

    @Override
    public void setComputedAmpere(float computedAmpere) {
        // No-op for now; custom input uses active toggle for testing.
    }

    @Override
    public void updateSimulation(java.util.List<Wire> wires) {
        // Custom input ports don't need special simulation updates
        // But the interface requires this method to be implemented
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        g2.setColor(Colors.COMPONENT_STROKE);
        g2.drawRect(x, y, width, height);
        if (active) {
            g2.setColor(new Color(220, 120, 60));
            int inset = Math.max(2, width / 6);
            g2.fillRect(x + inset, y + inset, width - inset * 2, height - inset * 2);
        }
        int indicatorSize = Math.max(4, width / 5);
        int indicatorX = x + width - indicatorSize - 2;
        int indicatorY = y + 2;
        if (active) {
            g2.setColor(new Color(80, 200, 120));
            g2.fillOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
        } else {
            g2.setColor(new Color(120, 120, 130));
            g2.drawOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
        }
        g2.setColor(Colors.COMPONENT_STROKE);
        java.awt.Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Math.max(9f, originalFont.getSize2D() * 0.75f)));
        String label = getDisplayName();
        int textX = x + width + 6;
        int textY = y + height / 2 + (g2.getFontMetrics().getAscent() / 2) - 6;
        g2.drawString(label, textX, textY);
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }
}
