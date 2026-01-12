package circuitsim.components;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Graphics2D;

/**
 * Ground reference component.
 */
public class Ground extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 2;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 1;
    private boolean activeIndicator;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public Ground(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        addConnectionPoint(0.5f, 0f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean includeDefaultProperties() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean allowFullRotation() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().isEmpty()) {
            return;
        }
        ConnectionPoint point = getConnectionPoints().get(0);
        int x0 = point.getX();
        int y0 = point.getY();
        int lineHeight = Math.max(6, height / 6);
        int baseY = y0 + lineHeight;
        int widthTop = Math.max(10, width / 2);
        int widthMid = Math.max(8, width / 3);
        int widthBottom = Math.max(6, width / 4);
        g2.drawLine(x0, y0, x0, baseY);
        g2.drawLine(x0 - widthTop / 2, baseY, x0 + widthTop / 2, baseY);
        int midY = baseY + lineHeight / 2;
        g2.drawLine(x0 - widthMid / 2, midY, x0 + widthMid / 2, midY);
        int bottomY = midY + lineHeight / 2;
        g2.drawLine(x0 - widthBottom / 2, bottomY, x0 + widthBottom / 2, bottomY);
        if (activeIndicator) {
            int dotSize = Math.max(4, Math.min(width, height) / 5);
            g2.fillOval(x0 - (dotSize / 2), bottomY + 2, dotSize, dotSize);
        }
    }

    /**
     * Updates the active indicator state.
     */
    public void setActiveIndicator(boolean activeIndicator) {
        this.activeIndicator = activeIndicator;
    }
}
