package circuitsim.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class for components placed on the circuit board.
 * Handles positioning, sizing, connection points, and drawing helpers.
 */
public abstract class CircuitComponent implements PropertyOwner {
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);
    private static final float BASE_STROKE_WIDTH = 2f;
    private static final float MIN_STROKE_WIDTH = 1f;
    private static final float MIN_FONT_SIZE = 10f;
    private static final float CONNECTION_DOT_RATIO = 0.12f;
    private static final int MIN_CONNECTION_DOT_SIZE = 4;
    private static final int PROPERTY_TEXT_MARGIN = 2;
    private static final float PROPERTY_FONT_SCALE = 0.6f;
    private static final float TITLE_FONT_SCALE = 0.8f;

    private final long id;
    private final int connectionAmount;
    private final int baseWidth;
    protected int x;
    protected int y;
    protected int height;
    protected int width;
    private float aspectRatio;
    private final List<ConnectionPoint> connectionPoints = new ArrayList<>();
    private final List<ComponentProperty> properties = new ArrayList<>();
    private boolean showPropertyValues = false;
    private boolean showTitle = false;
    private String displayName;
    private int rotationQuarterTurns = 0;

    /**
     * @param x left position in pixels
     * @param y top position in pixels
     * @param height component height in pixels
     * @param width component width in pixels
     * @param connectionAmount maximum number of connection points
     */
    protected CircuitComponent(int x, int y, int height, int width, int connectionAmount) {
        this.id = ID_COUNTER.getAndIncrement();
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
        this.baseWidth = Math.max(1, width);
        this.aspectRatio = width / (float) height;
        this.connectionAmount = Math.max(0, connectionAmount);
        this.displayName = getClass().getSimpleName();
        if (includeDefaultProperties()) {
            properties.add(new BooleanProperty("Show Values", this::isShowingPropertyValues,
                    this::setShowPropertyValues, false));
            properties.add(new BooleanProperty("Show Title", this::isShowTitle, this::setShowTitle, false));
        }
    }

    /**
     * Determines whether default visibility properties are added.
     */
    protected boolean includeDefaultProperties() {
        return true;
    }

    /**
     * @return true if the provided point lies within this component's bounds
     */
    public boolean contains(int pointX, int pointY) {
        return getBounds().contains(pointX, pointY);
    }

    /**
     * Updates the component's upper-left position.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return current X position in pixels
     */
    public int getX() {
        return x;
    }

    /**
     * @return current Y position in pixels
     */
    public int getY() {
        return y;
    }

    /**
     * @return current width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return current height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Updates the component size, clamping to a minimum of 1 pixel.
     */
    public void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    /**
     * @return unique identifier for this component
     */
    public long getId() {
        return id;
    }

    /**
     * @return maximum number of connection points
     */
    public int getConnectionAmount() {
        return connectionAmount;
    }

    /**
     * @return immutable list of connection points
     */
    public List<ConnectionPoint> getConnectionPoints() {
        return Collections.unmodifiableList(connectionPoints);
    }

    /**
     * @return immutable list of properties exposed to the UI
     */
    @Override
    public List<ComponentProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    /**
     * @return true when the display name can be edited
     */
    @Override
    public boolean isTitleEditable() {
        return true;
    }

    /**
     * Adds a property entry to display and edit in the UI.
     */
    protected void addProperty(ComponentProperty property) {
        properties.add(property);
    }

    /**
     * Adds a connection point at relative coordinates (0..1).
     */
    protected void addConnectionPoint(float relativeX, float relativeY) {
        if (connectionPoints.size() >= connectionAmount) {
            return;
        }
        connectionPoints.add(new ConnectionPoint(this, relativeX, relativeY));
    }

    /**
     * @return snapped world-space X for a connection point
     */
    public int getConnectionPointWorldX(ConnectionPoint point) {
        java.awt.Point rotated = getConnectionPointWorldRaw(point);
        return circuitsim.ui.Grid.snap(rotated.x);
    }

    /**
     * @return snapped world-space Y for a connection point
     */
    public int getConnectionPointWorldY(ConnectionPoint point) {
        java.awt.Point rotated = getConnectionPointWorldRaw(point);
        return circuitsim.ui.Grid.snap(rotated.y);
    }

    /**
     * Connects the first available local point to the provided point.
     */
    public void addConnection(ConnectionPoint point) {
        if (point == null) {
            return;
        }
        for (ConnectionPoint localPoint : connectionPoints) {
            if (!localPoint.isConnected()) {
                localPoint.connect(point);
                return;
            }
        }
    }

    /**
     * Disconnects the provided point from this component, if present.
     */
    public void removeConnection(ConnectionPoint point) {
        for (ConnectionPoint localPoint : connectionPoints) {
            if (localPoint.getConnectedPoints().contains(point)) {
                localPoint.disconnect(point);
                return;
            }
        }
    }

    /**
     * Disconnects all points from this component.
     */
    public void disconnectAllConnections() {
        for (ConnectionPoint point : connectionPoints) {
            List<ConnectionPoint> connectedPoints = new ArrayList<>(point.getConnectedPoints());
            for (ConnectionPoint connected : connectedPoints) {
                point.disconnect(connected);
                connected.disconnect(point);
            }
        }
    }

    /**
     * Resizes the component while preserving the original aspect ratio.
     */
    public void resizeKeepingRatio(int targetWidth, int targetHeight) {
        float targetRatio = targetWidth / (float) targetHeight;
        if (targetRatio > aspectRatio) {
            width = Math.max(1, Math.round(targetHeight * aspectRatio));
            height = Math.max(1, targetHeight);
        } else {
            width = Math.max(1, targetWidth);
            height = Math.max(1, Math.round(targetWidth / aspectRatio));
        }
    }

    /**
     * @return bounding box in world coordinates
     */
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * @return diameter in pixels for connection dots
     */
    public int getConnectionDotSize() {
        return Math.max(MIN_CONNECTION_DOT_SIZE, Math.round(Math.min(width, height) * CONNECTION_DOT_RATIO));
    }

    /**
     * @return true when property values are shown on the canvas
     */
    public boolean isShowingPropertyValues() {
        return showPropertyValues;
    }

    /**
     * Toggles property value visibility.
     */
    public void setShowPropertyValues(boolean showPropertyValues) {
        this.showPropertyValues = showPropertyValues;
    }

    /**
     * @return true when the component title is shown on the canvas
     */
    public boolean isShowTitle() {
        return showTitle;
    }

    /**
     * Toggles title visibility.
     */
    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    /**
     * @return rotation in quarter turns
     */
    public int getRotationQuarterTurns() {
        return rotationQuarterTurns;
    }

    /**
     * Sets the rotation in quarter turns, respecting component constraints.
     *
     * @param rotationQuarterTurns desired rotation
     */
    public void setRotationQuarterTurns(int rotationQuarterTurns) {
        int normalized;
        if (allowFullRotation()) {
            normalized = rotationQuarterTurns % 4;
            if (normalized < 0) {
                normalized += 4;
            }
        } else {
            normalized = rotationQuarterTurns % 2;
            if (normalized < 0) {
                normalized += 2;
            }
        }
        this.rotationQuarterTurns = normalized;
    }

    /**
     * @return rotation in radians derived from quarter turns
     */
    public double getRotationRadians() {
        return Math.toRadians(rotationQuarterTurns * 90.0);
    }

    /**
     * Rotates the component by 90 degrees, preserving its center.
     */
    public void rotate90() {
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        int rotationLimit = allowFullRotation() ? 4 : 2;
        rotationQuarterTurns = (rotationQuarterTurns + 1) % rotationLimit;
        int oldWidth = width;
        width = height;
        height = oldWidth;
        x = (int) Math.round(centerX - (width / 2.0));
        y = (int) Math.round(centerY - (height / 2.0));
        if (aspectRatio != 0) {
            aspectRatio = 1f / aspectRatio;
        }
    }

    /**
     * @return true if the component can rotate in 90-degree steps (0-3).
     */
    protected boolean allowFullRotation() {
        return false;
    }

    /**
     * @return display name shown in the UI
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Updates the display name, falling back to the class name if empty.
     */
    @Override
    public void setDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            this.displayName = getClass().getSimpleName();
        } else {
            this.displayName = displayName.trim();
        }
    }

    /**
     * Draws the component, its connection points, title, and property values.
     */
    public final void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke originalStroke = g2.getStroke();
        Font originalFont = g2.getFont();
        Color originalColor = g2.getColor();
        java.awt.geom.AffineTransform originalTransform = g2.getTransform();
        float scale = width / (float) baseWidth;
        g2.setStroke(new BasicStroke(Math.max(MIN_STROKE_WIDTH, BASE_STROKE_WIDTH * scale)));
        g2.setFont(originalFont.deriveFont(Math.max(MIN_FONT_SIZE, originalFont.getSize2D() * scale)));
        int originalX = x;
        int originalY = y;
        int originalWidth = width;
        int originalHeight = height;
        boolean swapped = rotationQuarterTurns % 2 != 0;
        if (swapped) {
            double centerX = x + (width / 2.0);
            double centerY = y + (height / 2.0);
            width = originalHeight;
            height = originalWidth;
            x = (int) Math.round(centerX - (width / 2.0));
            y = (int) Math.round(centerY - (height / 2.0));
        }
        // Draw the component with the current rotation applied.
        applyRotationTransform(g2);
        drawComponent(g2);
        g2.setTransform(originalTransform);
        if (swapped) {
            x = originalX;
            y = originalY;
            width = originalWidth;
            height = originalHeight;
        }
        drawConnectionPoints(g2);
        drawTitle(g2);
        drawPropertyValues(g2);
        g2.setStroke(originalStroke);
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }

    /**
     * Rotates the graphics context around the component center.
     */
    private void applyRotationTransform(Graphics2D g2) {
        if (rotationQuarterTurns == 0) {
            return;
        }
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        g2.rotate(getRotationRadians(), centerX, centerY);
    }

    /**
     * Draws connection points after the component body is rendered.
     */
    private void drawConnectionPoints(Graphics2D g2) {
        int dotSize = getConnectionDotSize();
        int radius = dotSize / 2;
        g2.setColor(circuitsim.ui.Colors.CONNECTION_DOT);
        for (ConnectionPoint point : connectionPoints) {
            int centerX = getConnectionPointWorldX(point);
            int centerY = getConnectionPointWorldY(point);
            g2.fillOval(centerX - radius, centerY - radius, dotSize, dotSize);
        }
    }

    /**
     * Draws the component title above the component.
     */
    private void drawTitle(Graphics2D g2) {
        if (!showTitle || displayName == null || displayName.isEmpty()) {
            return;
        }
        Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Math.max(8f, originalFont.getSize2D() * TITLE_FONT_SCALE)));
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        int textY = y - metrics.getDescent();
        drawRotatedText(g2, displayName, x + PROPERTY_TEXT_MARGIN, textY);
        g2.setFont(originalFont);
    }

    /**
     * Draws property values under the component.
     */
    private void drawPropertyValues(Graphics2D g2) {
        if (!showPropertyValues) {
            return;
        }
        java.awt.Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Math.max(8f, originalFont.getSize2D() * PROPERTY_FONT_SCALE)));
        List<ComponentProperty> displayProperties = new ArrayList<>();
        for (ComponentProperty property : properties) {
            if (property.isDisplayable()) {
                displayProperties.add(property);
            }
        }
        if (displayProperties.isEmpty()) {
            g2.setFont(originalFont);
            return;
        }
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        int lineHeight = metrics.getHeight();
        int startY = y + height + lineHeight;
        int textX = x + PROPERTY_TEXT_MARGIN;
        for (int i = 0; i < displayProperties.size(); i++) {
            ComponentProperty property = displayProperties.get(i);
            String text = property.getName() + ": " + property.getDisplayValue();
            drawRotatedText(g2, text, textX, startY + (i * lineHeight));
        }
        g2.setFont(originalFont);
    }

    /**
     * Draws text, respecting component rotation.
     */
    private void drawRotatedText(Graphics2D g2, String text, int textX, int textY) {
        if (rotationQuarterTurns == 0) {
            g2.drawString(text, textX, textY);
            return;
        }
        double angle = getRotationRadians();
        double textAngle = angle;
        if (Math.abs(angle) > (Math.PI / 2)) {
            textAngle += Math.PI;
        }
        java.awt.geom.AffineTransform originalTransform = g2.getTransform();
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        g2.translate(centerX, centerY);
        g2.rotate(textAngle);
        g2.drawString(text, (float) (textX - centerX), (float) (textY - centerY));
        g2.setTransform(originalTransform);
    }

    /**
     * Computes the world-space position of a connection point without snapping.
     */
    private java.awt.Point getConnectionPointWorldRaw(ConnectionPoint point) {
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        int unrotatedWidth = (rotationQuarterTurns % 2 != 0) ? height : width;
        int unrotatedHeight = (rotationQuarterTurns % 2 != 0) ? width : height;
        double unrotatedX = centerX - (unrotatedWidth / 2.0);
        double unrotatedY = centerY - (unrotatedHeight / 2.0);
        double localX = unrotatedX + (unrotatedWidth * point.getRelativeX());
        double localY = unrotatedY + (unrotatedHeight * point.getRelativeY());
        if (rotationQuarterTurns == 0) {
            return new java.awt.Point((int) Math.round(localX), (int) Math.round(localY));
        }
        double angle = getRotationRadians();
        double dx = localX - centerX;
        double dy = localY - centerY;
        double rotatedX = (dx * Math.cos(angle)) - (dy * Math.sin(angle));
        double rotatedY = (dx * Math.sin(angle)) + (dy * Math.cos(angle));
        return new java.awt.Point((int) Math.round(centerX + rotatedX),
                (int) Math.round(centerY + rotatedY));
    }

    /**
     * Implemented by subclasses to paint the component body.
     */
    protected abstract void drawComponent(Graphics2D g2);
}
