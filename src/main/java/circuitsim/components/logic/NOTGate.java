package circuitsim.components.logic;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;

/**
 * NOT (inverter) gate component with one input and one output.
 */
@BuiltinComponent(group = "Logic", paletteName = "NOT", groupOrder = 20, paletteOrder = 50)
public class NOTGate extends LogicGate {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 4;
    private static final int CONNECTION_AMOUNT = 2;

    public NOTGate(int x, int y) {
        super(x, y, CONNECTION_AMOUNT);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
        setDisplayName("NOT");
    }

    @Override
    protected int getMinimumSize() {
        return Grid.SIZE * 2;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    public int getInputCount() {
        return 1;
    }

    @Override
    public int getOutputCount() {
        return 1;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();
        float strokeWidth = 1f;
        if (originalStroke instanceof BasicStroke basicStroke) {
            strokeWidth = basicStroke.getLineWidth();
        }
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(Colors.COMPONENT_STROKE);

        GateGeometry geo = computeGeometry();
        int topY = geo.bodyY;
        int bottomY = geo.bodyY + geo.bodyHeight;
        int inputY = clamp(geo.inputY, topY, bottomY);

        g2.setColor(isInputPowered(0) ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.inputX, inputY, geo.bodyX, inputY);
        g2.setColor(Colors.COMPONENT_STROKE);

        Polygon triangle = new Polygon(
                new int[]{geo.bodyX, geo.bodyX, geo.bodyRight},
                new int[]{topY, bottomY, geo.centerY},
                3
        );
        g2.drawPolygon(triangle);

        // Inversion bubble at the tip (same sizing approach as NAND).
        g2.drawOval(geo.circleCenterX - geo.circleRadius, geo.centerY - geo.circleRadius,
                geo.circleRadius * 2, geo.circleRadius * 2);
        if (isOutputPowered()) {
            Color original = g2.getColor();
            g2.setColor(new Color(220, 60, 60, 160));
            int glowRadius = geo.circleRadius + 3;
            g2.fillOval(geo.circleCenterX - glowRadius, geo.centerY - glowRadius,
                    glowRadius * 2, glowRadius * 2);
            g2.setColor(original);
        }

        int outputStartX = geo.circleCenterX + geo.circleRadius;
        g2.setColor(isOutputPowered() ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(outputStartX, geo.centerY, geo.outputX, geo.centerY);
        g2.setColor(Colors.COMPONENT_STROKE);

        drawGateLabelInside(g2, geo);

        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
    }

    private GateGeometry computeGeometry() {
        List<ConnectionPoint> points = getConnectionPoints();
        int inputX = x;
        int inputY = y + (height / 2);
        int outputX = x + width;
        int outputY = y + (height / 2);
        if (points.size() >= 2) {
            java.awt.Point input = getSnappedConnectionPointInDrawSpace(points.get(0));
            java.awt.Point output = getSnappedConnectionPointInDrawSpace(points.get(1));
            inputX = input.x;
            inputY = input.y;
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
        return new GateGeometry(bodyX, bodyY, bodyWidth, bodyHeight, bodyRight, inputX, inputY,
                outputX, centerY, circleRadius, circleCenterX);
    }

    private void drawGateLabelInside(Graphics2D g2, GateGeometry geo) {
        String label = getDisplayName();
        if (label == null || label.isEmpty()) {
            return;
        }
        double bodyCenterX = geo.bodyX + (geo.bodyWidth / 2.0);
        drawOrthogonalStringCentered(g2, label, bodyCenterX, geo.centerY);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class GateGeometry {
        private final int bodyX;
        private final int bodyY;
        private final int bodyWidth;
        private final int bodyHeight;
        private final int bodyRight;
        private final int inputX;
        private final int inputY;
        private final int outputX;
        private final int centerY;
        private final int circleRadius;
        private final int circleCenterX;

        private GateGeometry(int bodyX, int bodyY, int bodyWidth, int bodyHeight, int bodyRight,
                             int inputX, int inputY, int outputX, int centerY,
                             int circleRadius, int circleCenterX) {
            this.bodyX = bodyX;
            this.bodyY = bodyY;
            this.bodyWidth = bodyWidth;
            this.bodyHeight = bodyHeight;
            this.bodyRight = bodyRight;
            this.inputX = inputX;
            this.inputY = inputY;
            this.outputX = outputX;
            this.centerY = centerY;
            this.circleRadius = circleRadius;
            this.circleCenterX = circleCenterX;
        }
    }
}
