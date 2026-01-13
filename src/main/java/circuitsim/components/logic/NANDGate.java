package circuitsim.components.logic;

import circuitsim.components.core.*;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

/**
 * NAND gate component with two inputs and one output.
 * Implements boolean NAND logic: output is LOW only when both inputs are HIGH.
 * EXTENSIBLE: Inherits from modular LogicGate framework.
 */
public class NANDGate extends LogicGate {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 4;
    private static final int CONNECTION_AMOUNT = 3;

    public NANDGate(int x, int y) {
        // Use modular LogicGate constructor (2 inputs + 1 output = 3 connections)
        super(x, y, CONNECTION_AMOUNT);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        addConnectionPoint(0f, 1f / 3f);  // Input A
        addConnectionPoint(0f, 2f / 3f);  // Input B
        addConnectionPoint(1f, 0.5f);   // Output
        setDisplayName("NAND");
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        g2.setColor(Colors.COMPONENT_STROKE);
        Stroke originalStroke = g2.getStroke();
        float strokeWidth = 1f;
        if (originalStroke instanceof BasicStroke) {
            strokeWidth = ((BasicStroke) originalStroke).getLineWidth();
        }
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        
        if (getConnectionPoints().size() < 3) {
            g2.setColor(originalColor);
            return;
        }
        
        // Draw complete gate structure using modular framework
        drawGateBody(g2);
        drawInversionCircle(g2);
        drawOutputSection(g2);
        drawGateLabelInside(g2);
        
        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
    }
    
    /**
     * Draws NAND gate body using modular framework.
     * REUSABLE: Other logic gates can inherit this method.
     */
    @Override
    protected void drawGateBody(Graphics2D g2) {
        GateGeometry geo = computeGeometry();
        int topY = geo.centerY - (geo.bodyHeight / 2);
        int bottomY = topY + geo.bodyHeight;
        int inputAY = clamp(geo.inputAY, topY, bottomY);
        int inputBY = clamp(geo.inputBY, topY, bottomY);

        g2.drawLine(geo.bodyX, topY, geo.bodyX, bottomY);
        g2.setColor(isInputPowered(0) ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.inputAX, inputAY, geo.bodyX, inputAY);
        g2.setColor(isInputPowered(1) ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.inputBX, inputBY, geo.bodyX, inputBY);
        g2.setColor(Colors.COMPONENT_STROKE);

        int arcRight = geo.bodyRight;
        int arcWidth = Math.min(geo.bodyHeight, Math.max(1, 2 * (arcRight - geo.bodyX)));
        int arcX = arcRight - arcWidth;
        int arcTopX = arcX + (arcWidth / 2);
        g2.drawLine(geo.bodyX, topY, arcTopX, topY);
        g2.drawLine(geo.bodyX, bottomY, arcTopX, bottomY);
        g2.drawArc(arcX, topY, arcWidth, geo.bodyHeight, -90, 180);
    }
    
    /**
     * Draws NAND gate inversion circle using modular framework.
     * REUSABLE: Other logic gates without inversion can inherit.
     */
    @Override
    protected void drawInversionCircle(Graphics2D g2) {
        GateGeometry geo = computeGeometry();
        int circleRadius = geo.circleRadius;
        int centerX = geo.circleCenterX;
        int centerY = geo.centerY;

        // Draw circle outline only (diagram style)
        g2.drawOval(centerX - circleRadius, centerY - circleRadius, 
                    circleRadius * 2, circleRadius * 2);
        if (isOutputPowered()) {
            Color original = g2.getColor();
            g2.setColor(new Color(220, 60, 60, 160));
            int glowRadius = circleRadius + 3;
            g2.fillOval(centerX - glowRadius, centerY - glowRadius,
                    glowRadius * 2, glowRadius * 2);
            g2.setColor(original);
        }
    }
    
    /**
     * Draws output line using modular framework.
     * REUSABLE: Other logic gates can inherit this method.
     */
    @Override
    protected void drawOutputSection(Graphics2D g2) {
        GateGeometry geo = computeGeometry();
        int circleRadius = geo.circleRadius;
        int centerX = geo.circleCenterX;
        int centerY = geo.centerY;
        int lineStartX = centerX + circleRadius;
        int lineEndX = Math.max(lineStartX, geo.outputX);
        g2.setColor(isOutputPowered() ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(lineStartX, centerY, lineEndX, centerY);
        g2.setColor(Colors.COMPONENT_STROKE);
    }
    
    /**
     * NAND-specific logic for input validation and processing.
     * EXTENSIBLE: Pattern for other logic gates.
     */
    @Override
    public boolean isInputConnection(int connectionIndex) {
        return connectionIndex < 2; // NAND has 2 inputs
    }
    
    @Override
    public boolean isInputPoint(ConnectionPoint point) {
        // First two connections (index 0,1) are inputs
        int index = getConnectionPointIndex(point);
        return index < 2;
    }
    
    @Override
    protected boolean isOutputConnection(int connectionIndex) {
        return connectionIndex == 2; // Third connection (index 2) is output
    }
    
    @Override
    public boolean isOutputPoint(ConnectionPoint point) {
        int index = getConnectionPointIndex(point);
        return index == 2; // Output is third connection point
    }

    private GateGeometry computeGeometry() {
        List<ConnectionPoint> points = getConnectionPoints();
        int inputAX = x;
        int inputAY = y + (height / 3);
        int inputBX = x;
        int inputBY = y + ((height * 2) / 3);
        int outputX = x + width;
        int outputY = y + (height / 2);
        if (points.size() >= 3) {
            java.awt.Point inputA = getSnappedConnectionPointInDrawSpace(points.get(0));
            java.awt.Point inputB = getSnappedConnectionPointInDrawSpace(points.get(1));
            java.awt.Point output = getSnappedConnectionPointInDrawSpace(points.get(2));
            inputAX = inputA.x;
            inputAY = inputA.y;
            inputBX = inputB.x;
            inputBY = inputB.y;
            outputX = output.x;
            outputY = output.y;
        }
        int margin = Math.max(6, Math.min(width, height) / 6);
        int bodyX = x + margin;
        int bodyHeight = Math.max(12, height - (margin * 2));
        int minBodyY = y + margin;
        int maxBodyY = Math.max(minBodyY, y + height - margin - bodyHeight);
        int desiredBodyY = outputY - (bodyHeight / 2);
        int bodyY = clamp(desiredBodyY, minBodyY, maxBodyY);
        int minBodyWidth = 12;
        int circleRadius = Math.max(2, Math.min(width, height) / 14);
        circleRadius = Math.min(circleRadius, Math.max(2, bodyHeight / 8));
        int maxCircleRadius = Math.max(2, (outputX - (bodyX + minBodyWidth)) / 2);
        circleRadius = Math.max(2, Math.min(circleRadius, maxCircleRadius));
        int bodyRight = outputX - (circleRadius * 2);
        int bodyWidth = Math.max(1, bodyRight - bodyX);
        int centerY = outputY;
        int circleCenterX = bodyRight + circleRadius;
        return new GateGeometry(bodyX, bodyY, bodyWidth, bodyHeight, bodyRight,
                inputAX, inputAY, inputBX, inputBY, outputX, centerY, circleRadius, circleCenterX);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawGateLabelInside(Graphics2D g2) {
        GateGeometry geo = computeGeometry();
        String label = getDisplayName();
        if (label == null || label.isEmpty()) {
            return;
        }
        double bodyCenterX = geo.bodyX + (geo.bodyWidth / 2.0);
        drawOrthogonalStringCentered(g2, label, bodyCenterX, geo.centerY);
    }

    private static final class GateGeometry {
        private final int bodyX;
        private final int bodyY;
        private final int bodyWidth;
        private final int bodyHeight;
        private final int bodyRight;
        private final int inputAX;
        private final int inputAY;
        private final int inputBX;
        private final int inputBY;
        private final int outputX;
        private final int centerY;
        private final int circleRadius;
        private final int circleCenterX;

        private GateGeometry(int bodyX, int bodyY, int bodyWidth, int bodyHeight, int bodyRight,
                             int inputAX, int inputAY, int inputBX, int inputBY,
                             int outputX, int centerY, int circleRadius, int circleCenterX) {
            this.bodyX = bodyX;
            this.bodyY = bodyY;
            this.bodyWidth = bodyWidth;
            this.bodyHeight = bodyHeight;
            this.bodyRight = bodyRight;
            this.inputAX = inputAX;
            this.inputAY = inputAY;
            this.inputBX = inputBX;
            this.inputBY = inputBY;
            this.outputX = outputX;
            this.centerY = centerY;
            this.circleRadius = circleRadius;
            this.circleCenterX = circleCenterX;
        }
    }
}
