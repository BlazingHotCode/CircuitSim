package circuitsim.ui;

import java.awt.Graphics2D;

/**
 * Tracks pan/zoom state and provides screen-to-world transforms.
 */
final class ViewTransform {
    static final double MIN_ZOOM = 0.5;
    static final double MAX_ZOOM = 2.5;
    static final double ZOOM_STEP = 0.1;

    private int offsetX;
    private int offsetY;
    private double zoomFactor = 1.0;

    void apply(Graphics2D g2) {
        g2.translate(offsetX, offsetY);
        g2.scale(zoomFactor, zoomFactor);
    }

    int toWorldX(int screenX) {
        return (int) Math.round((screenX - offsetX) / zoomFactor);
    }

    int toWorldY(int screenY) {
        return (int) Math.round((screenY - offsetY) / zoomFactor);
    }

    void zoomAt(int screenX, int screenY, double delta, Runnable repaint) {
        double nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomFactor + delta));
        if (Math.abs(nextZoom - zoomFactor) < 0.0001) {
            return;
        }
        double worldX = (screenX - offsetX) / zoomFactor;
        double worldY = (screenY - offsetY) / zoomFactor;
        zoomFactor = nextZoom;
        offsetX = (int) Math.round(screenX - (worldX * zoomFactor));
        offsetY = (int) Math.round(screenY - (worldY * zoomFactor));
        repaint.run();
    }

    void reset(Runnable repaint) {
        zoomFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint.run();
    }

    int getOffsetX() {
        return offsetX;
    }

    int getOffsetY() {
        return offsetY;
    }

    double getZoomFactor() {
        return zoomFactor;
    }

    void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}

