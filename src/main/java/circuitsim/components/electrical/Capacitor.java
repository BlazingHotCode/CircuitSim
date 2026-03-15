package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.core.TwoTerminalComponent;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import circuitsim.ui.TerminalSnap;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Capacitor component for the DC solver.
 */
@BuiltinComponent(group = "Passive", groupOrder = 30, paletteOrder = 35)
public class Capacitor extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_CAPACITANCE_FARAD = 0.001f;
    private static final int CHARGE_BUTTON_SIZE = 14;

    private float capacitanceFarad;
    private float computedVoltage;
    private float computedAmpere;
    private float previousVoltage;
    private boolean showingStoredCharge;

    public Capacitor(int x, int y) {
        this(x, y, DEFAULT_CAPACITANCE_FARAD);
    }

    public Capacitor(int x, int y, float capacitanceFarad) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.capacitanceFarad = capacitanceFarad;
        addProperty(new FloatProperty("Capacitance (F)", this::getCapacitanceFarad, this::setCapacitanceFarad, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
        addProperty(new ComputedFloatProperty("Stored Charge (C)", this::getStoredChargeCoulomb, false));
    }

    public float getCapacitanceFarad() {
        return capacitanceFarad;
    }

    public void setCapacitanceFarad(float capacitanceFarad) {
        this.capacitanceFarad = capacitanceFarad;
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

    public float getStoredChargeCoulomb() {
        return Math.max(0f, capacitanceFarad) * Math.max(0f, computedVoltage);
    }

    public float getPreviousVoltage() {
        return previousVoltage;
    }

    public void setPreviousVoltage(float previousVoltage) {
        this.previousVoltage = previousVoltage;
    }

    public boolean isShowingStoredCharge() {
        return showingStoredCharge;
    }

    public void toggleStoredChargeDisplay() {
        showingStoredCharge = !showingStoredCharge;
    }

    public boolean isStoredChargeButtonHit(int worldX, int worldY) {
        int buttonX = getStoredChargeButtonX();
        int buttonY = getStoredChargeButtonY();
        return worldX >= buttonX && worldX <= buttonX + CHARGE_BUTTON_SIZE
                && worldY >= buttonY && worldY <= buttonY + CHARGE_BUTTON_SIZE;
    }

    private int getStoredChargeButtonX() {
        return x + ((width - CHARGE_BUTTON_SIZE) / 2);
    }

    private int getStoredChargeButtonY() {
        return y + 2;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        Font originalFont = g2.getFont();
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().size() < 2) {
            return;
        }
        ConnectionPoint start = getConnectionPoints().get(0);
        ConnectionPoint end = getConnectionPoints().get(1);
        java.awt.Point snappedStart = TerminalSnap.getSnappedLocalTerminal(this, start);
        java.awt.Point snappedEnd = TerminalSnap.getSnappedLocalTerminal(this, end);
        int sx = snappedStart.x;
        int sy = snappedStart.y;
        int ex = snappedEnd.x;
        int ey = snappedEnd.y;

        double dx = ex - sx;
        double dy = ey - sy;
        double length = Math.hypot(dx, dy);
        if (length < 1) {
            return;
        }
        double ux = dx / length;
        double uy = dy / length;
        double nx = -uy;
        double ny = ux;

        double centerX = (sx + ex) / 2.0;
        double centerY = (sy + ey) / 2.0;
        double plateGap = Math.max(Grid.SIZE * 0.35, length * 0.08);
        double halfPlate = Math.max(Grid.SIZE * 0.45, Math.min(width, height) * 0.35);
        double leftPlateX = centerX - (ux * plateGap);
        double leftPlateY = centerY - (uy * plateGap);
        double rightPlateX = centerX + (ux * plateGap);
        double rightPlateY = centerY + (uy * plateGap);

        g2.drawLine(sx, sy, (int) Math.round(leftPlateX), (int) Math.round(leftPlateY));
        g2.drawLine(ex, ey, (int) Math.round(rightPlateX), (int) Math.round(rightPlateY));
        g2.drawLine(
                (int) Math.round(leftPlateX + (nx * halfPlate)),
                (int) Math.round(leftPlateY + (ny * halfPlate)),
                (int) Math.round(leftPlateX - (nx * halfPlate)),
                (int) Math.round(leftPlateY - (ny * halfPlate)));
        g2.drawLine(
                (int) Math.round(rightPlateX + (nx * halfPlate)),
                (int) Math.round(rightPlateY + (ny * halfPlate)),
                (int) Math.round(rightPlateX - (nx * halfPlate)),
                (int) Math.round(rightPlateY - (ny * halfPlate)));

        int buttonX = getStoredChargeButtonX();
        int buttonY = getStoredChargeButtonY();
        if (showingStoredCharge) {
            g2.setColor(new Color(210, 170, 80));
            g2.fillOval(buttonX, buttonY, CHARGE_BUTTON_SIZE, CHARGE_BUTTON_SIZE);
            g2.setColor(Colors.COMPONENT_STROKE);
        } else {
            g2.drawOval(buttonX, buttonY, CHARGE_BUTTON_SIZE, CHARGE_BUTTON_SIZE);
        }
        g2.setFont(originalFont.deriveFont(Font.BOLD, Math.max(8f, originalFont.getSize2D() * 0.65f)));
        FontMetrics buttonMetrics = g2.getFontMetrics();
        String buttonLabel = "Q";
        int labelX = buttonX + (CHARGE_BUTTON_SIZE - buttonMetrics.stringWidth(buttonLabel)) / 2;
        int labelY = buttonY + ((CHARGE_BUTTON_SIZE - buttonMetrics.getHeight()) / 2) + buttonMetrics.getAscent();
        g2.drawString(buttonLabel, labelX, labelY);

        if (showingStoredCharge) {
            g2.setFont(originalFont.deriveFont(Math.max(8f, originalFont.getSize2D() * 0.75f)));
            FontMetrics valueMetrics = g2.getFontMetrics();
            String valueText = String.format(java.util.Locale.US, "Q=%.3g C", getStoredChargeCoulomb());
            int textX = x + Math.max(2, (width - valueMetrics.stringWidth(valueText)) / 2);
            int textY = y + height + valueMetrics.getAscent() + 4;
            g2.drawString(valueText, textX, textY);
        }
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }
}
