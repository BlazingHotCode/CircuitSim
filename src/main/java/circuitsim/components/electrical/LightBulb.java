package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.TwoTerminalComponent;
import circuitsim.components.properties.BooleanProperty;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
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

        float rated = Math.max(0.001f, ratedPowerWatt);
        float brightness = burnedOut ? 0f : (computedPowerWatt / rated);
        brightness = Math.max(0f, Math.min(2f, brightness));
        boolean lit = brightness > 0.05f;
        if (lit) {
            float intensity = Math.min(1f, brightness);
            float over = Math.max(0f, brightness - 1f);
            int glowR = 255;
            int glowG = Math.min(255, Math.round(200 + (55 * over)));
            int glowB = Math.min(255, Math.round(90 + (140 * over)));
            Color glowColor = new Color(glowR, glowG, glowB);
            Composite originalComposite = g2.getComposite();
            int glowLift = Math.max(1, Math.round(size * 0.08f));

            // Outer soft glow.
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.max(0f, Math.min(1f, 0.08f + (0.18f * intensity)))));
            g2.setColor(glowColor);
            int outerInset = Math.max(1, Math.round(size * (0.05f - (0.02f * intensity))));
            g2.fillOval(left + outerInset, top + outerInset - glowLift, size - outerInset * 2, size - outerInset * 2);

            // Inner glow.
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.max(0f, Math.min(1f, 0.14f + (0.30f * intensity)))));
            int inset = Math.max(2, Math.round(size * 0.15f));
            g2.fillOval(left + inset, top + inset - glowLift, size - inset * 2, size - inset * 2);

            // Core brightness.
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.max(0f, Math.min(1f, 0.10f + (0.35f * intensity)))));
            int coreInset = Math.max(inset + 1, Math.round(size * 0.26f));
            g2.fillOval(left + coreInset, top + coreInset - glowLift,
                    size - coreInset * 2, size - coreInset * 2);

            g2.setComposite(originalComposite);
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
        if (lit) {
            g2.setColor(new Color(235, 170, 70));
        }
        g2.drawLine(fx1, fy, fx2, peakY);
        g2.drawLine(fx2, peakY, midX, fy);
        g2.drawLine(midX, fy, fx3, peakY);
        g2.drawLine(fx3, peakY, fx4, fy);
        g2.setColor(Colors.COMPONENT_STROKE);

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
