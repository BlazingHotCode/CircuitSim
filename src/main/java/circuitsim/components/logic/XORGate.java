package circuitsim.components.logic;

import circuitsim.components.core.ConnectionPoint;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.QuadCurve2D;
import java.util.List;

/**
 * XOR gate component with two inputs and one output.
 */
public class XORGate extends LogicGate {
    private static final int DEFAULT_WIDTH = Grid.SIZE * 4;
    private static final int DEFAULT_HEIGHT = Grid.SIZE * 4;
    private static final int CONNECTION_AMOUNT = 3;

    public XORGate(int x, int y) {
        super(x, y, CONNECTION_AMOUNT);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        addConnectionPoint(0f, 1f / 3f);
        addConnectionPoint(0f, 2f / 3f);
        addConnectionPoint(1f, 0.5f);
        setDisplayName("XOR");
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();
        float strokeWidth = 1f;
        if (originalStroke instanceof BasicStroke) {
            strokeWidth = ((BasicStroke) originalStroke).getLineWidth();
        }
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(Colors.COMPONENT_STROKE);

        GateGeometry geo = computeGeometry();
        int topY = geo.centerY - (geo.bodyHeight / 2);
        int bottomY = topY + geo.bodyHeight;
        int inputAY = clamp(geo.inputAY, topY, bottomY);
        int inputBY = clamp(geo.inputBY, topY, bottomY);

        int backArcWidth = (int) (geo.bodyWidth / 3);
        int backArcX = geo.bodyX - (backArcWidth / 2);
        int xorOffset = Math.max(4, backArcWidth / 5);
        int xorBackArcX = backArcX - xorOffset;

        int inputEndTop = (int) Math.round(getBackArcXAtY(xorBackArcX, backArcWidth, topY, geo.bodyHeight, inputAY));
        int inputEndBottom = (int) Math.round(getBackArcXAtY(xorBackArcX, backArcWidth, topY, geo.bodyHeight, inputBY));

        g2.setColor(isInputPowered(0) ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.inputAX, inputAY, inputEndTop, inputAY);
        g2.setColor(isInputPowered(1) ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.inputBX, inputBY, inputEndBottom, inputBY);
        g2.setColor(Colors.COMPONENT_STROKE);

        // XOR is an OR gate with an additional parallel "back" arc.
        g2.drawArc(xorBackArcX, topY, backArcWidth, geo.bodyHeight, -90, 180);
        g2.drawArc(backArcX, topY, backArcWidth, geo.bodyHeight, -90, 180);

        float controlX = geo.bodyX + (geo.bodyWidth * 0.6f);
        QuadCurve2D.Float topCurve = new QuadCurve2D.Float(
                geo.bodyX, topY,
                controlX, topY,
                geo.bodyRight, geo.centerY
        );
        QuadCurve2D.Float bottomCurve = new QuadCurve2D.Float(
                geo.bodyX, bottomY,
                controlX, bottomY,
                geo.bodyRight, geo.centerY
        );
        g2.draw(topCurve);
        g2.draw(bottomCurve);

        g2.setColor(isOutputPowered() ? getPoweredColor() : Colors.COMPONENT_STROKE);
        g2.drawLine(geo.bodyRight, geo.centerY, geo.outputX, geo.centerY);
        g2.setColor(Colors.COMPONENT_STROKE);

        drawGateLabelInside(g2, geo);

        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
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
        int bodyHeight = Math.max(12, height - (margin * 2));
        int bodyY = clamp(outputY - (bodyHeight / 2), y + margin,
                Math.max(y + margin, y + height - margin - bodyHeight));
        int bodyX = x + margin;
        int outputLead = Math.max(4, width / 6);
        int bodyRight = Math.max(bodyX + 12, outputX - outputLead);
        int bodyWidth = Math.max(1, bodyRight - bodyX);
        int centerY = outputY;
        return new GateGeometry(bodyX, bodyY, bodyWidth, bodyHeight, bodyRight,
                inputAX, inputAY, inputBX, inputBY, outputX, centerY);
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

    private double getBackArcXAtY(int arcX, int arcWidth, int arcY, int arcHeight, int y) {
        double a = arcWidth / 2.0;
        double b = arcHeight / 2.0;
        double cx = arcX + a;
        double cy = arcY + b;
        double dy = y - cy;
        double inside = 1.0 - ((dy * dy) / (b * b));
        if (inside < 0.0) {
            inside = 0.0;
        }
        double dx = a * Math.sqrt(inside);
        return cx + dx;
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

        private GateGeometry(int bodyX, int bodyY, int bodyWidth, int bodyHeight, int bodyRight,
                             int inputAX, int inputAY, int inputBX, int inputBY,
                             int outputX, int centerY) {
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
        }
    }
}
