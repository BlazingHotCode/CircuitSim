package circuitsim.components;

import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Voltmeter extends CircuitComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 3;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final int CONNECTION_AMOUNT = 2;
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String UNIT_LABEL = "V";

    private float computedVoltage;

    public Voltmeter(int x, int y) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH, CONNECTION_AMOUNT);
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
    }

    public float getComputedVoltage() {
        return computedVoltage;
    }

    public void setComputedVoltage(float computedVoltage) {
        this.computedVoltage = computedVoltage;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        g2.setColor(Colors.COMPONENT_STROKE);
        int arc = Math.max(8, Math.min(width, height) / 3);
        g2.drawRoundRect(x, y, width, height, arc, arc);
        String label = VALUE_FORMAT.format(computedVoltage) + " " + UNIT_LABEL;
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + Math.max(6, width / 10);
        int textY = y + (height + metrics.getAscent() - metrics.getDescent()) / 2;
        g2.drawString(label, textX, textY);
    }
}
