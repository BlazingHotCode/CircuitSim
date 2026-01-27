package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.TwoTerminalComponent;
import circuitsim.components.properties.BooleanProperty;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Light bulb modeled as a constant-power load with burn-out behavior.
 */
@BuiltinComponent(group = "Passive", paletteName = "Light Bulb", groupOrder = 30, paletteOrder = 30)
public class LightBulb extends TwoTerminalComponent {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 2;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_RATED_POWER_WATT = 5f;

    private static final float BURNOUT_MULTIPLIER = 2.0f;
    private static final int BURNOUT_TICKS = 30;

    private float ratedPowerWatt;
    private boolean burnedOut;
    private int burnoutCounter;

    private float computedVoltage;
    private float computedAmpere;
    private float computedPowerWatt;
    private float computedResistance;

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public LightBulb(int x, int y) {
        this(x, y, DEFAULT_RATED_POWER_WATT);
    }

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param ratedPowerWatt rated wattage
     */
    public LightBulb(int x, int y, float ratedPowerWatt) {
        super(x, y, DEFAULT_HEIGHT, DEFAULT_WIDTH);
        this.ratedPowerWatt = ratedPowerWatt;
        addProperty(new FloatProperty("Rated Power (W)", this::getRatedPowerWatt, this::setRatedPowerWatt, true));
        addProperty(new BooleanProperty("Burned Out", this::isBurnedOut, this::setBurnedOut, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
        addProperty(new ComputedFloatProperty("Actual Power (W)", this::getComputedPowerWatt, false));
        addProperty(new ComputedFloatProperty("Effective Resistance (Ω)", this::getComputedResistance, false));
    }

    public float getRatedPowerWatt() {
        return ratedPowerWatt;
    }

    public void setRatedPowerWatt(float ratedPowerWatt) {
        this.ratedPowerWatt = ratedPowerWatt;
    }

    public boolean isBurnedOut() {
        return burnedOut;
    }

    public void setBurnedOut(boolean burnedOut) {
        this.burnedOut = burnedOut;
        if (!burnedOut) {
            burnoutCounter = 0;
        }
    }

    public void updateBurnout(float actualPowerWatt) {
        if (burnedOut) {
            return;
        }
        float rated = ratedPowerWatt;
        if (!(rated > 0f)) {
            burnoutCounter = 0;
            return;
        }
        float threshold = rated * BURNOUT_MULTIPLIER;
        if (actualPowerWatt > threshold) {
            burnoutCounter++;
            if (burnoutCounter >= BURNOUT_TICKS) {
                burnedOut = true;
            }
        } else {
            burnoutCounter = Math.max(0, burnoutCounter - 1);
        }
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

    public float getComputedPowerWatt() {
        return computedPowerWatt;
    }

    public void setComputedPowerWatt(float computedPowerWatt) {
        this.computedPowerWatt = computedPowerWatt;
    }

    public float getComputedResistance() {
        return computedResistance;
    }

    public void setComputedResistance(float computedResistance) {
        this.computedResistance = computedResistance;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color original = g2.getColor();
        int size = Math.max(6, Math.min(width, height) - 4);
        int cx = x + (width / 2);
        int cy = y + (height / 2);
        int left = cx - (size / 2);
        int top = cy - (size / 2);

        boolean lit = !burnedOut && computedPowerWatt > Math.max(0.1f, ratedPowerWatt * 0.2f);
        if (lit) {
            g2.setColor(new Color(240, 200, 80));
            int inset = Math.max(2, Math.round(size * 0.15f));
            int glowLift = Math.max(1, Math.round(size * 0.08f));
            g2.fillOval(left + inset, top + inset - glowLift, size - inset * 2, size - inset * 2);
        }

        g2.setColor(Colors.COMPONENT_STROKE);
        // Bulb glass (top dome)
        int bulbWidth = Math.max(6, Math.round(size * 0.8f));
        int bulbHeight = Math.max(6, Math.round(size * 0.75f));
        int bulbLeft = cx - (bulbWidth / 2);
        int bulbTop = top + Math.max(1, Math.round(size * 0.05f));
        g2.drawOval(bulbLeft, bulbTop, bulbWidth, bulbHeight);

        // Bulb base
        int baseWidth = Math.max(5, Math.round(bulbWidth * 0.45f));
        int baseHeight = Math.max(4, Math.round(size * 0.22f));
        int baseLeft = cx - (baseWidth / 2);
        int baseTop = bulbTop + bulbHeight - Math.max(1, Math.round(size * 0.08f));
        g2.drawRect(baseLeft, baseTop, baseWidth, baseHeight);
        int ridgeCount = 2;
        for (int i = 1; i <= ridgeCount; i++) {
            int ry = baseTop + (i * baseHeight) / (ridgeCount + 1);
            g2.drawLine(baseLeft, ry, baseLeft + baseWidth, ry);
        }

        // Filament: simple "M" shape inside the dome
        int filamentPadX = Math.max(2, Math.round(bulbWidth * 0.18f));
        int filamentPadY = Math.max(2, Math.round(bulbHeight * 0.28f));
        int fx1 = bulbLeft + filamentPadX;
        int fx4 = bulbLeft + bulbWidth - filamentPadX;
        int fy = bulbTop + bulbHeight - filamentPadY - Math.max(2, baseHeight / 2);
        int midX = (fx1 + fx4) / 2;
        int peakY = bulbTop + filamentPadY + Math.max(2, bulbHeight / 5);
        int fx2 = (fx1 + midX) / 2;
        int fx3 = (midX + fx4) / 2;
        g2.drawLine(fx1, fy, fx2, peakY);
        g2.drawLine(fx2, peakY, midX, fy);
        g2.drawLine(midX, fy, fx3, peakY);
        g2.drawLine(fx3, peakY, fx4, fy);

        // Burned-out indicator: small crack "X" on the glass
        if (burnedOut) {
            g2.setColor(new Color(180, 70, 70));
            int crackSize = Math.max(6, Math.round(bulbWidth * 0.35f));
            int crackLeft = cx - (crackSize / 2);
            int crackTop = bulbTop + Math.max(2, Math.round(bulbHeight * 0.18f));
            g2.drawLine(crackLeft, crackTop, crackLeft + crackSize, crackTop + crackSize);
            g2.drawLine(crackLeft, crackTop + crackSize, crackLeft + crackSize, crackTop);
        }
        g2.setColor(original);
    }
}
