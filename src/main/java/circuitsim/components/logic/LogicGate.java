package circuitsim.components.logic;

import circuitsim.components.core.*;

import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * MODULAR ABSTRACT SUPERCLASS: Provides extensible framework for all logic gates.
 * SINGLE RESPONSIBILITY: Each method has one clear purpose.
 * 
 * KEY MODULARITY BENEFITS:
 * - 🏗️ True Modularity: Each component type is self-contained
 * - 🔄 Easy Extension: Adding gates requires minimal code changes
 * - 🛡 Type Safety: Interface contracts prevent breaking changes
 * - 📐 Reusable Components: Common code eliminates duplication
 * - 🎯 Production Ready: Framework scales to any number of logic gates
 * 
 * SINGLE RESPONSIBILITY DESIGN PATTERNS:
 * - Each method does one thing well
 * - Template methods for customization
 * - Factory pattern for instantiation
 * - Modular constructor inheritance
 */
public abstract class LogicGate extends CircuitComponent {
    
    private boolean[] inputPowered = new boolean[0];
    private boolean outputPowered;

    protected LogicGate(int x, int y, int baseWidth, int baseHeight, int connectionCount) {
        super(x, y, baseHeight, baseWidth, connectionCount, false);
    }

    protected LogicGate(int x, int y, int connectionCount) {
        this(x, y, Grid.SIZE * 3, Grid.SIZE * 3, connectionCount);
    }
    
    /**
     * FACTORY PATTERN: All logic gates use this constructor pattern.
     * MODULAR: Just inherit constructor and override logic methods.
     */
    protected LogicGate(int x, int y) {
        this(x, y, 3); // Default: 2 inputs + 1 output = 3 connections
    }
    
    /**
     * MODULAR: Template method for adding connection points.
     * Override to create custom input/output layouts.
     */
    protected void addLogicConnectionPoints() {
        // Default: 2 inputs + 1 output configuration
        addConnectionPoint(0f, 0.25f);  // Input A
        addConnectionPoint(0f, 0.75f);  // Input B  
        addConnectionPoint(1f, 0.5f);   // Output
    }

    /**
     * MODULAR: Template method for custom gate appearance.
     * Override to create specific gate visuals.
     */
    protected void drawGateBody(java.awt.Graphics2D g2) {
        // Default: no gate body.
    }
    
    /**
     * MODULAR: Template method for custom output drawing.
     * Override for specific gate output visual customization.
     */
    protected void drawOutputSection(java.awt.Graphics2D g2) {
        // Default: no output section.
    }
    
    /**
     * MODULAR: Reusable drawing utilities for all logic gates.
     * Eliminates code duplication across gate types.
     */
    protected final void drawGateLabel(java.awt.Graphics2D g2) {
        java.awt.Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(java.lang.Math.max(7f, originalFont.getSize2D() * 0.6f)));
        String label = getDisplayName();
        
        int textX = x + (width - g2.getFontMetrics().stringWidth(label)) / 2;
        int textY = y + height - 4;
        
        g2.drawString(label, textX, textY);
        g2.setFont(originalFont);
    }
    
    /**
     * MODULAR: Template method for adding connection points.
     * Override to create custom input/output layouts.
     */
    @Override
    protected void addConnectionPoint(float relativeX, float relativeY) {
        // Default: Use standard connection point addition
        super.addConnectionPoint(relativeX, relativeY);
    }
    
    /**
     * MODULAR: Template method for input validation and processing.
     * Override to create custom input selection logic.
     */
    @Override
    public boolean isInputConnection(int connectionIndex) {
        // Default: First connections are inputs (2, AND=2, OR=2, NOT=1, etc.)
        return connectionIndex >= 0 && connectionIndex < getInputCount();
    }
    
    /**
     * MODULAR: Template method for output validation.
     * Override to create custom output selection logic.
     */
    protected boolean isOutputConnection(int connectionIndex) {
        return connectionIndex >= getInputCount()
                && connectionIndex < (getInputCount() + getOutputCount());
    }

    @Override
    public boolean isInputPoint(ConnectionPoint point) {
        int index = getConnectionPointIndex(point);
        return index >= 0 && isInputConnection(index);
    }

    @Override
    public boolean isOutputPoint(ConnectionPoint point) {
        int index = getConnectionPointIndex(point);
        return index >= 0 && isOutputConnection(index);
    }

    public ConnectionPoint getOutputPoint() {
        for (ConnectionPoint point : getConnectionPoints()) {
            if (isOutputPoint(point)) {
                return point;
            }
        }
        return null;
    }

    /**
     * MODULAR: Template method for input counting.
     * Override to create custom input count behavior.
     */
    @Override
    public int getInputCount() {
        // Default: First connections are inputs (2 inputs, 1 output)
        return 2;
    }
    
    /**
     * MODULAR: Template method for output counting.
     * Override to create custom output count behavior.
     */
    @Override
    public int getOutputCount() {
        // Default: First connections are inputs (2 inputs, 1 output)
        return 1;
    }

    /**
     * Minimum size for logic gates unless overridden by a specific gate.
     */
    protected int getMinimumSize() {
        return Grid.SIZE * 4;
    }

    @Override
    public void setSize(int width, int height) {
        int min = getMinimumSize();
        super.setSize(Math.max(width, min), Math.max(height, min));
    }

    @Override
    protected int getMinimumWidth() {
        // Logic gates manage their own minimum size via getMinimumSize().
        return 1;
    }

    @Override
    protected int getMinimumHeight() {
        // Logic gates manage their own minimum size via getMinimumSize().
        return 1;
    }

    public void setInputPowered(int index, boolean powered) {
        if (index < 0) {
            return;
        }
        int inputCount = Math.max(0, getInputCount());
        if (inputPowered.length < inputCount) {
            boolean[] resized = new boolean[inputCount];
            System.arraycopy(inputPowered, 0, resized, 0, inputPowered.length);
            inputPowered = resized;
        }
        if (index >= inputPowered.length) {
            return;
        }
        inputPowered[index] = powered;
    }

    protected boolean isInputPowered(int index) {
        return index >= 0 && index < inputPowered.length && inputPowered[index];
    }

    public void setOutputPowered(boolean powered) {
        outputPowered = powered;
    }

    public boolean isOutputPowered() {
        return outputPowered;
    }

    /**
     * Optional inversion circle (used by NAND/NOR).
     */
    protected void drawInversionCircle(java.awt.Graphics2D g2) {
        // Default: no inversion circle
    }

    protected Color getPoweredColor() {
        return new Color(220, 60, 60);
    }

    protected final void drawUprightString(Graphics2D g2, String text, int textX, int textY) {
        if (getRotationQuarterTurns() == 0) {
            g2.drawString(text, textX, textY);
            return;
        }
        java.awt.geom.AffineTransform rotated = g2.getTransform();
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        double angle = getRotationRadians();

        java.awt.geom.Point2D devicePoint = rotated.transform(
                new java.awt.geom.Point2D.Double(textX, textY),
                null
        );

        java.awt.geom.AffineTransform unrotated = new java.awt.geom.AffineTransform(rotated);
        unrotated.rotate(-angle, centerX, centerY);

        java.awt.geom.Point2D uprightPoint;
        try {
            uprightPoint = unrotated.createInverse().transform(devicePoint, null);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            // Fallback: just draw at the original position.
            g2.drawString(text, textX, textY);
            return;
        }

        g2.setTransform(unrotated);
        g2.drawString(text, (int) Math.round(uprightPoint.getX()), (int) Math.round(uprightPoint.getY()));
        g2.setTransform(rotated);
    }

    /**
     * Draws text "locked" to the component position (so it moves with the gate), but restricts
     * the orientation to either 0° or 90° so it never appears upside-down.
     */
    protected final void drawOrthogonalString(Graphics2D g2, String text, int textX, int textY) {
        int desiredQuarterTurns = getRotationQuarterTurns() % 2;
        if (desiredQuarterTurns < 0) {
            desiredQuarterTurns += 2;
        }
        double currentAngle = getRotationRadians();
        double desiredAngle = Math.toRadians(desiredQuarterTurns * 90.0);
        if (Math.abs(currentAngle - desiredAngle) < 1e-9) {
            g2.drawString(text, textX, textY);
            return;
        }

        java.awt.geom.AffineTransform rotated = g2.getTransform();
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);

        java.awt.geom.Point2D devicePoint = rotated.transform(
                new java.awt.geom.Point2D.Double(textX, textY),
                null
        );

        java.awt.geom.AffineTransform target = new java.awt.geom.AffineTransform(rotated);
        target.rotate(-currentAngle, centerX, centerY);
        target.rotate(desiredAngle, centerX, centerY);

        java.awt.geom.Point2D targetPoint;
        try {
            targetPoint = target.createInverse().transform(devicePoint, null);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            g2.drawString(text, textX, textY);
            return;
        }

        g2.setTransform(target);
        g2.drawString(text, (int) Math.round(targetPoint.getX()), (int) Math.round(targetPoint.getY()));
        g2.setTransform(rotated);
    }

    /**
     * Same as {@link #drawOrthogonalString(Graphics2D, String, int, int)}, but keeps the text's
     * bounding-box centered on the provided point. This prevents shifts at 180°/270° rotations
     * caused by baseline/anchor changes when forcing 0°/90° text orientation.
     */
    protected final void drawOrthogonalStringCentered(Graphics2D g2, String text, double centerX, double centerY) {
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        double baselineOffsetY = (metrics.getAscent() - metrics.getDescent()) / 2.0;

        int desiredQuarterTurns = getRotationQuarterTurns() % 2;
        if (desiredQuarterTurns < 0) {
            desiredQuarterTurns += 2;
        }
        double currentAngle = getRotationRadians();
        double desiredAngle = Math.toRadians(desiredQuarterTurns * 90.0);

        java.awt.geom.AffineTransform rotated = g2.getTransform();
        double compCenterX = x + (width / 2.0);
        double compCenterY = y + (height / 2.0);

        java.awt.geom.Point2D deviceCenter = rotated.transform(
                new java.awt.geom.Point2D.Double(centerX, centerY),
                null
        );

        java.awt.geom.AffineTransform target = new java.awt.geom.AffineTransform(rotated);
        target.rotate(-currentAngle, compCenterX, compCenterY);
        target.rotate(desiredAngle, compCenterX, compCenterY);

        java.awt.geom.Point2D targetCenter;
        try {
            targetCenter = target.createInverse().transform(deviceCenter, null);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            g2.drawString(text,
                    (int) Math.round(centerX - (textWidth / 2.0)),
                    (int) Math.round(centerY + baselineOffsetY));
            return;
        }

        g2.setTransform(target);
        g2.drawString(text,
                (int) Math.round(targetCenter.getX() - (textWidth / 2.0)),
                (int) Math.round(targetCenter.getY() + baselineOffsetY));
        g2.setTransform(rotated);
    }

    /**
     * Returns a point in the gate's draw space (the coordinates used inside {@link #drawComponent})
     * that will land exactly on the snapped world-space connection point after the component's
     * rotation transform is applied.
     *
     * This avoids "not lining up when rotated" issues caused by snapping before rotation.
     */
    protected final Point getSnappedConnectionPointInDrawSpace(ConnectionPoint point) {
        if (point == null) {
            return new Point(x, y);
        }
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);

        double localX = x + (width * point.getRelativeX());
        double localY = y + (height * point.getRelativeY());

        double angle = getRotationRadians();
        double dx = localX - centerX;
        double dy = localY - centerY;
        double worldX = (dx * Math.cos(angle)) - (dy * Math.sin(angle));
        double worldY = (dx * Math.sin(angle)) + (dy * Math.cos(angle));
        int snappedWorldX = Grid.snap((int) Math.round(centerX + worldX));
        int snappedWorldY = Grid.snap((int) Math.round(centerY + worldY));

        if (getRotationQuarterTurns() == 0) {
            return new Point(snappedWorldX, snappedWorldY);
        }

        double sdx = snappedWorldX - centerX;
        double sdy = snappedWorldY - centerY;
        // Inverse rotation.
        double invLocalX = (sdx * Math.cos(angle)) + (sdy * Math.sin(angle));
        double invLocalY = (-sdx * Math.sin(angle)) + (sdy * Math.cos(angle));
        return new Point((int) Math.round(centerX + invLocalX), (int) Math.round(centerY + invLocalY));
    }
}
