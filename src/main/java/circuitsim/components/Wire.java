package circuitsim.components;

import circuitsim.ui.Colors;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Wire {
    private WireNode start;
    private WireNode end;
    private static final float STROKE_WIDTH = 3f;
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private boolean showData = false;
    private float computedVoltage = 0f;
    private float computedAmpere = 0f;
    private boolean shortCircuit = false;
    private WireColor color;
    private Wire startAnchorWire;
    private Wire endAnchorWire;

    public Wire(WireNode start, WireNode end) {
        this(start, end, WireColor.WHITE);
    }

    public Wire(WireNode start, WireNode end, WireColor color) {
        this.start = start;
        this.end = end;
        this.start.addWire(this);
        this.end.addWire(this);
        this.color = color == null ? WireColor.WHITE : color;
    }

    public void draw(Graphics2D g2) {
        if (start == null || end == null) {
            return;
        }
        drawAt(g2, start.getX(), start.getY(), end.getX(), end.getY());
    }

    public void drawAt(Graphics2D g2, int startX, int startY, int endX, int endY) {
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();
        g2.setColor(color.getColor());
        g2.setStroke(new BasicStroke(STROKE_WIDTH));
        g2.drawLine(startX, startY, endX, endY);
        drawDataLabel(g2, startX, startY, endX, endY);
        g2.setColor(originalColor);
        g2.setStroke(originalStroke);
    }

    public WireNode getStart() {
        return start;
    }

    public WireNode getEnd() {
        return end;
    }

    public void setStart(WireNode start) {
        if (this.start != null) {
            this.start.removeWire(this);
        }
        this.start = start;
        if (this.start != null) {
            this.start.addWire(this);
        }
    }

    public void setEnd(WireNode end) {
        if (this.end != null) {
            this.end.removeWire(this);
        }
        this.end = end;
        if (this.end != null) {
            this.end.addWire(this);
        }
    }

    public void detach() {
        if (start != null) {
            start.removeWire(this);
        }
        if (end != null) {
            end.removeWire(this);
        }
    }

    public boolean isShowData() {
        return showData;
    }

    public void setShowData(boolean showData) {
        this.showData = showData;
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

    public boolean isShortCircuit() {
        return shortCircuit;
    }

    public void setShortCircuit(boolean shortCircuit) {
        this.shortCircuit = shortCircuit;
    }

    public Wire getStartAnchorWire() {
        return startAnchorWire;
    }

    public void setStartAnchorWire(Wire startAnchorWire) {
        this.startAnchorWire = startAnchorWire;
    }

    public Wire getEndAnchorWire() {
        return endAnchorWire;
    }

    public void setEndAnchorWire(Wire endAnchorWire) {
        this.endAnchorWire = endAnchorWire;
    }

    public WireColor getWireColor() {
        return color;
    }

    public void setWireColor(WireColor color) {
        this.color = color == null ? WireColor.WHITE : color;
    }

    public Color getColor() {
        return color.getColor();
    }

    public static float getStrokeWidth() {
        return STROKE_WIDTH;
    }

    public void moveBy(int dx, int dy) {
        if (start != null) {
            start.setPosition(start.getX() + dx, start.getY() + dy);
        }
        if (end != null) {
            end.setPosition(end.getX() + dx, end.getY() + dy);
        }
    }

    private void drawDataLabel(Graphics2D g2, int startX, int startY, int endX, int endY) {
        if (!showData) {
            return;
        }
        String label;
        if (shortCircuit) {
            label = "∞";
        } else {
            label = VALUE_FORMAT.format(computedAmpere) + "A";
        }
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;
        double angle = Math.atan2(endY - startY, endX - startX);
        boolean flipText = Math.abs(angle) > (Math.PI / 2);
        double textAngle = flipText ? angle + Math.PI : angle;
        AffineTransform originalTransform = g2.getTransform();
        Color originalColor = g2.getColor();
        g2.setColor(shortCircuit ? Colors.SHORT_LABEL : Colors.WIRE_LABEL);
        g2.translate(midX, midY);
        g2.rotate(textAngle);
        java.awt.Font originalFont = g2.getFont();
        if (shortCircuit) {
            g2.setFont(originalFont.deriveFont(originalFont.getSize2D() * 1.4f));
        }
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int textOffsetY = flipText ? 12 : -4;
        g2.drawString(label, -textWidth / 2, textOffsetY);
        if (shortCircuit) {
            g2.setFont(originalFont);
        }
        g2.setTransform(originalTransform);
        g2.setColor(originalColor);
    }
}
