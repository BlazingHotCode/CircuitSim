package circuitsim.components;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Ammeter extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 2;
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String UNIT_LABEL = "A";

    private float computedAmpere;

    public Ammeter(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
    }

    public float getComputedAmpere() {
        return computedAmpere;
    }

    public void setComputedAmpere(float computedAmpere) {
        this.computedAmpere = computedAmpere;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int arc = Math.max(8, Math.min(width, height) / 3);
        g2.drawRoundRect(x, y, width, height, arc, arc);
        String label = VALUE_FORMAT.format(computedAmpere) + " " + UNIT_LABEL;
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + Math.max(6, width / 10);
        int textY = y + (height + metrics.getAscent() - metrics.getDescent()) / 2;
        g2.drawString(label, textX, textY);
    }
}
