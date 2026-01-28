package circuitsim.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Draws a small coordinate HUD in the bottom-right of the canvas.
 */
public final class CoordinatesOverlay {
    private static final int MARGIN = 10;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 5;
    private static final int ARC = 10;
    private static final float BG_ALPHA = 0.75f;
    private static final float FONT_SCALE = 0.9f;
    private static final float MIN_FONT_SIZE = 12f;

    /**
     * Draws the overlay in screen space.
     *
     * @param g2 drawing context (must be in screen space)
     * @param canvas canvas component used for sizing/coordinate conversion
     * @param propertiesPanel properties panel to avoid overlapping (may be null)
     * @param snappedWorldX snapped world X
     * @param snappedWorldY snapped world Y
     */
    public void draw(Graphics2D g2, JComponent canvas, JComponent propertiesPanel,
                     int snappedWorldX, int snappedWorldY) {
        if (g2 == null || canvas == null) {
            return;
        }
        String text = snappedWorldX + ", " + snappedWorldY;

        Font originalFont = g2.getFont();
        Color originalColor = g2.getColor();
        Composite originalComposite = g2.getComposite();

        Font font = originalFont.deriveFont(Math.max(MIN_FONT_SIZE, originalFont.getSize2D() * FONT_SCALE));
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();

        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getAscent();
        int boxW = textWidth + (PAD_X * 2);
        int boxH = textHeight + (PAD_Y * 2);

        int rightEdge = canvas.getWidth() - MARGIN;
        if (propertiesPanel != null && propertiesPanel.isShowing()) {
            java.awt.Point propTopLeft = SwingUtilities.convertPoint(propertiesPanel, 0, 0, canvas);
            if (propTopLeft != null && propTopLeft.x < rightEdge) {
                rightEdge = Math.min(rightEdge, propTopLeft.x - MARGIN);
            }
        }
        int boxX = Math.max(MARGIN, rightEdge - boxW);
        int boxY = Math.max(MARGIN, canvas.getHeight() - MARGIN - boxH);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, BG_ALPHA));
        g2.setColor(Colors.PROPERTIES_BG);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, ARC, ARC);
        g2.setComposite(originalComposite);
        g2.setColor(Colors.PROPERTIES_TEXT);
        g2.drawString(text, boxX + PAD_X, boxY + PAD_Y + metrics.getAscent());

        g2.setFont(originalFont);
        g2.setColor(originalColor);
        g2.setComposite(originalComposite);
    }
}

