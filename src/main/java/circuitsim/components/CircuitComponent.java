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
        properties.add(new BooleanProperty("Show Values", this::isShowingPropertyValues,
                this::setShowPropertyValues, false));
        properties.add(new BooleanProperty("Show Title", this::isShowTitle, this::setShowTitle, false));
    }

    public boolean contains(int pointX, int pointY) {
        return getBounds().contains(pointX, pointY);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public long getId() {
        return id;
    }

    public int getConnectionAmount() {
        return connectionAmount;
    }

    public List<ConnectionPoint> getConnectionPoints() {
        return Collections.unmodifiableList(connectionPoints);
    }

    @Override
    public List<ComponentProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public boolean isTitleEditable() {
        return true;
    }

    protected void addProperty(ComponentProperty property) {
        properties.add(property);
    }

    protected void addConnectionPoint(float relativeX, float relativeY) {
        if (connectionPoints.size() >= connectionAmount) {
            return;
        }
        connectionPoints.add(new ConnectionPoint(this, relativeX, relativeY));
    }

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

    public void removeConnection(ConnectionPoint point) {
        for (ConnectionPoint localPoint : connectionPoints) {
            if (localPoint.getConnectedPoints().contains(point)) {
                localPoint.disconnect(point);
                return;
            }
        }
    }

    public void disconnectAllConnections() {
        for (ConnectionPoint point : connectionPoints) {
            List<ConnectionPoint> connectedPoints = new ArrayList<>(point.getConnectedPoints());
            for (ConnectionPoint connected : connectedPoints) {
                point.disconnect(connected);
                connected.disconnect(point);
            }
        }
    }

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

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getConnectionDotSize() {
        return Math.max(MIN_CONNECTION_DOT_SIZE, Math.round(Math.min(width, height) * CONNECTION_DOT_RATIO));
    }

    public boolean isShowingPropertyValues() {
        return showPropertyValues;
    }

    public void setShowPropertyValues(boolean showPropertyValues) {
        this.showPropertyValues = showPropertyValues;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public int getRotationQuarterTurns() {
        return rotationQuarterTurns;
    }

    public double getRotationRadians() {
        return Math.toRadians(rotationQuarterTurns * 90.0);
    }

    public void rotate90() {
        rotationQuarterTurns = (rotationQuarterTurns + 1) % 4;
        int oldWidth = width;
        width = height;
        height = oldWidth;
        if (aspectRatio != 0) {
            aspectRatio = 1f / aspectRatio;
        }
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            this.displayName = getClass().getSimpleName();
        } else {
            this.displayName = displayName.trim();
        }
    }

    public final void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke originalStroke = g2.getStroke();
        Font originalFont = g2.getFont();
        Color originalColor = g2.getColor();
        java.awt.geom.AffineTransform originalTransform = g2.getTransform();
        float scale = width / (float) baseWidth;
        g2.setStroke(new BasicStroke(Math.max(MIN_STROKE_WIDTH, BASE_STROKE_WIDTH * scale)));
        g2.setFont(originalFont.deriveFont(Math.max(MIN_FONT_SIZE, originalFont.getSize2D() * scale)));
        applyRotationTransform(g2);
        drawComponent(g2);
        g2.setTransform(originalTransform);
        drawConnectionPoints(g2);
        drawTitle(g2);
        drawPropertyValues(g2);
        g2.setStroke(originalStroke);
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }

    private void applyRotationTransform(Graphics2D g2) {
        if (rotationQuarterTurns == 0) {
            return;
        }
        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        g2.rotate(getRotationRadians(), centerX, centerY);
    }

    private void drawConnectionPoints(Graphics2D g2) {
        int dotSize = getConnectionDotSize();
        int radius = dotSize / 2;
        g2.setColor(circuitsim.ui.Colors.CONNECTION_DOT);
        for (ConnectionPoint point : connectionPoints) {
            int centerX = point.getX();
            int centerY = point.getY();
            g2.fillOval(centerX - radius, centerY - radius, dotSize, dotSize);
        }
    }

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

    protected abstract void drawComponent(Graphics2D g2);
}
