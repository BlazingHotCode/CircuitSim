package circuitsim.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Delegates mouse events to a {@link CircuitPanel} instance.
 */
final class CircuitMouseHandler extends MouseAdapter {
    private final CircuitPanel panel;

    CircuitMouseHandler(CircuitPanel panel) {
        this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        panel.handleMousePressed(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        panel.handleMouseClicked(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.handleMouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        panel.handleMouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        panel.handleMouseMoved(e);
    }
}
