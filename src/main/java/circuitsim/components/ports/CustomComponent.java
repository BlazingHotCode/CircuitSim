package circuitsim.components.ports;

import circuitsim.components.core.*;
import circuitsim.custom.CustomComponentDefinition;
import circuitsim.custom.CustomComponentPort;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual shell for a custom component definition.
 */
public class CustomComponent extends CircuitComponent {
    private static final int BASE_WIDTH = Grid.SIZE * 4;
    private static final int BASE_HEIGHT = Grid.SIZE * 2;
    private static final int CORNER_RADIUS = 10;
    private static final int PORT_LABEL_MARGIN = 6;

    private final CustomComponentDefinition definition;
    private final List<CustomComponentPort> inputs;
    private final List<CustomComponentPort> outputs;
    private final List<ConnectionPoint> inputPoints = new ArrayList<>();
    private final List<ConnectionPoint> outputPoints = new ArrayList<>();

    public CustomComponent(int x, int y, CustomComponentDefinition definition) {
        super(x, y, BASE_HEIGHT, BASE_WIDTH,
                Math.max(0, portCount(definition)), false);
        this.definition = definition;
        this.inputs = definition == null ? java.util.Collections.emptyList() : definition.getInputs();
        this.outputs = definition == null ? java.util.Collections.emptyList() : definition.getOutputs();
        setDisplayName(definition == null ? "Custom Component" : definition.getName());
        int minHeight = calculateMinHeight();
        if (minHeight > getHeight()) {
            setSize(getWidth(), minHeight);
        }
        buildConnectionPoints();
    }

    /**
     * @return the backing definition
     */
    public CustomComponentDefinition getDefinition() {
        return definition;
    }

    /**
     * @return true if the provided point is an input port
     */
    @Override
    public boolean isInputPoint(ConnectionPoint point) {
        return inputPoints.contains(point);
    }

    /**
     * @return true if the provided point is an output port
     */
    @Override
    public boolean isOutputPoint(ConnectionPoint point) {
        return outputPoints.contains(point);
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Color originalColor = g2.getColor();
        g2.setColor(Colors.COMPONENT_STROKE);
        g2.drawRoundRect(x, y, width, height, CORNER_RADIUS, CORNER_RADIUS);
        g2.setColor(originalColor);
    }

    @Override
    protected void drawConnectionPoints(Graphics2D g2) {
        Color originalColor = g2.getColor();
        g2.setColor(Colors.CONNECTION_DOT);
        drawInputMarkers(g2);
        drawOutputMarkers(g2);
        drawCenteredTitle(g2);
        drawPortLabels(g2);
        g2.setColor(originalColor);
    }

    private void drawCenteredTitle(Graphics2D g2) {
        Font originalFont = g2.getFont();
        Font font = originalFont.deriveFont(Math.max(10f, originalFont.getSize2D() * 0.9f));
        g2.setFont(font);
        String title = getDisplayName();
        int textWidth = g2.getFontMetrics().stringWidth(title);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height + g2.getFontMetrics().getAscent()) / 2 - 2;
        g2.drawString(title, textX, textY);
        g2.setFont(originalFont);
    }

    private void drawInputMarkers(Graphics2D g2) {
        int markerSize = getPortMarkerSize();
        int half = markerSize / 2;
        for (ConnectionPoint point : inputPoints) {
            int centerX = getConnectionPointWorldX(point);
            int centerY = getConnectionPointWorldY(point);
            g2.fillRect(centerX - half, centerY - half, markerSize, markerSize);
        }
    }

    private void drawOutputMarkers(Graphics2D g2) {
        int markerSize = getPortMarkerSize();
        int half = markerSize / 2;
        double rotation = getRotationRadians();
        for (ConnectionPoint point : outputPoints) {
            int centerX = getConnectionPointWorldX(point);
            int centerY = getConnectionPointWorldY(point);
            Polygon triangle = new Polygon();
            triangle.addPoint(centerX - half, centerY - half);
            triangle.addPoint(centerX - half, centerY + half);
            triangle.addPoint(centerX + half, centerY);
            java.awt.geom.AffineTransform originalTransform = g2.getTransform();
            if (rotation != 0.0) {
                g2.rotate(rotation, centerX, centerY);
            }
            g2.fillPolygon(triangle);
            g2.setTransform(originalTransform);
        }
    }

    private void drawPortLabels(Graphics2D g2) {
        Font originalFont = g2.getFont();
        Font font = originalFont.deriveFont(Math.max(9f, originalFont.getSize2D() * 0.75f));
        g2.setFont(font);
        int labelOffset = (getPortMarkerSize() / 2) + 4;
        for (int i = 0; i < inputPoints.size(); i++) {
            ConnectionPoint point = inputPoints.get(i);
            String label = inputs.get(i).getName();
            int textWidth = g2.getFontMetrics().stringWidth(label);
            int textX = getConnectionPointWorldX(point) - PORT_LABEL_MARGIN - textWidth;
            int textY = getConnectionPointWorldY(point) - labelOffset;
            g2.drawString(label, textX, textY);
        }
        for (int i = 0; i < outputPoints.size(); i++) {
            ConnectionPoint point = outputPoints.get(i);
            String label = outputs.get(i).getName();
            int textX = getConnectionPointWorldX(point) + PORT_LABEL_MARGIN;
            int textY = getConnectionPointWorldY(point) - labelOffset;
            g2.drawString(label, textX, textY);
        }
        g2.setFont(originalFont);
    }

    private int getPortMarkerSize() {
        return Math.max(6, Math.round(Math.min(width, height) * 0.2f));
    }

    private void buildConnectionPoints() {
        inputPoints.clear();
        outputPoints.clear();
        int inputCount = inputs.size();
        int outputCount = outputs.size();
        for (int i = 0; i < inputCount; i++) {
            float relativeY = (i + 1) / (float) (inputCount + 1);
            addConnectionPoint(0f, relativeY);
            inputPoints.add(getConnectionPoints().get(getConnectionPoints().size() - 1));
        }
        for (int i = 0; i < outputCount; i++) {
            float relativeY = (i + 1) / (float) (outputCount + 1);
            addConnectionPoint(1f, relativeY);
            outputPoints.add(getConnectionPoints().get(getConnectionPoints().size() - 1));
        }
    }

    private int calculateMinHeight() {
        int portCount = Math.max(inputs.size(), outputs.size());
        return Math.max(BASE_HEIGHT, Grid.SIZE * (portCount + 1));
    }

    private static int portCount(CustomComponentDefinition definition) {
        if (definition == null) {
            return 0;
        }
        return definition.getInputs().size() + definition.getOutputs().size();
    }
}
