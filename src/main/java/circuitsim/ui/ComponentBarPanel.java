package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ComponentRegistry;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Sliding component bar with grouped dropdowns.
 */
public class ComponentBarPanel extends JPanel {
    private static final int ANIMATION_DELAY_MS = 20;
    private static final int ANIMATION_STEP = 10;
    private static final int HIDE_DELAY_MS = 160;
    private static final int PREVIEW_WIDTH = 64;
    private static final int PREVIEW_HEIGHT = 32;
    private static final int ENTRY_HEIGHT = 44;
    private static final int DRAG_THRESHOLD = 6;
    private final CircuitPanel circuitPanel;
    private final List<GroupLabel> groupLabels = new ArrayList<>();
    private JPopupMenu activeMenu;
    private Timer hideTimer;
    private Timer animationTimer;
    private int currentY;
    private int targetY;
    private int shownY;
    private int hiddenY;
    private boolean barShown = true;

    /**
     * @param circuitPanel panel used for placement and preview
     */
    public ComponentBarPanel(CircuitPanel circuitPanel) {
        this.circuitPanel = circuitPanel;
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBackground(Colors.COMPONENT_BAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.COMPONENT_BAR_BORDER));
        setOpaque(true);
        buildGroups();
    }

    /**
     * Updates the bar width and anchors it to the top padding.
     */
    public void applyLayout(int width, int topPadding) {
        Dimension pref = getPreferredSize();
        setSize(width, pref.height);
        shownY = topPadding;
        hiddenY = -pref.height;
        if (animationTimer == null || !animationTimer.isRunning()) {
            currentY = barShown ? shownY : hiddenY;
        }
        setLocation(0, currentY);
    }

    /**
     * Toggles the bar with a slide animation.
     */
    public void toggleVisibility() {
        barShown = !barShown;
        hideActiveMenu();
        targetY = barShown ? shownY : hiddenY;
        startAnimation();
    }

    private void buildGroups() {
        for (ComponentRegistry.Group group : ComponentRegistry.getGroups()) {
            GroupLabel label = new GroupLabel(group);
            groupLabels.add(label);
            add(label);
        }
    }

    /**
     * Rebuilds the group labels from the registry.
     */
    public void refreshGroups() {
        hideActiveMenu();
        removeAll();
        groupLabels.clear();
        buildGroups();
        revalidate();
        repaint();
    }

    private void hideActiveMenu() {
        if (activeMenu != null) {
            activeMenu.setVisible(false);
            activeMenu = null;
        }
    }

    private void startAnimation() {
        if (animationTimer == null) {
            animationTimer = new Timer(ANIMATION_DELAY_MS, event -> stepAnimation());
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    private void stepAnimation() {
        if (currentY == targetY) {
            animationTimer.stop();
            return;
        }
        int delta = targetY > currentY ? ANIMATION_STEP : -ANIMATION_STEP;
        int nextY = currentY + delta;
        if ((delta > 0 && nextY > targetY) || (delta < 0 && nextY < targetY)) {
            nextY = targetY;
        }
        currentY = nextY;
        setLocation(0, currentY);
        repaint();
    }

    private void scheduleHide(JPopupMenu menu, JComponent label) {
        if (hideTimer != null) {
            hideTimer.stop();
        }
        hideTimer = new Timer(HIDE_DELAY_MS, event -> {
            if (!isPointerInside(menu) && !isPointerInside(label)) {
                menu.setVisible(false);
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void cancelHide() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
    }

    private boolean isPointerInside(Component component) {
        if (component == null || !component.isShowing()) {
            return false;
        }
        java.awt.PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }
        Point pointer = pointerInfo.getLocation();
        Point origin = component.getLocationOnScreen();
        Rectangle bounds = new Rectangle(origin.x, origin.y, component.getWidth(), component.getHeight());
        return bounds.contains(pointer);
    }

    private class GroupLabel extends JLabel {
        private final JPopupMenu menu;

        private GroupLabel(ComponentRegistry.Group group) {
            super(group.getName());
            setOpaque(true);
            setForeground(Colors.COMPONENT_BAR_TEXT);
            setBackground(Colors.COMPONENT_BAR_BG);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            menu = buildMenu(group, this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(Colors.COMPONENT_BAR_HOVER);
                    showMenu();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(Colors.COMPONENT_BAR_BG);
                    scheduleHide(menu, GroupLabel.this);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showMenu();
                }
            });
        }

        private void showMenu() {
            cancelHide();
            if (activeMenu != null && activeMenu != menu) {
                activeMenu.setVisible(false);
            }
            activeMenu = menu;
            if (!menu.isVisible()) {
                menu.show(this, 0, getHeight());
            }
        }
    }

    private JPopupMenu buildMenu(ComponentRegistry.Group group, JComponent label) {
        JPopupMenu menu = new JPopupMenu();
        menu.setLayout(new javax.swing.BoxLayout(menu, javax.swing.BoxLayout.Y_AXIS));
        menu.setBackground(Colors.COMPONENT_DROPDOWN_BG);
        menu.setBorder(BorderFactory.createLineBorder(Colors.COMPONENT_DROPDOWN_BORDER));
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelHide();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                scheduleHide(menu, label);
            }
        });
        if ("Custom".equals(group.getName())) {
            menu.add(buildCustomHeader(menu));
        }
        for (ComponentRegistry.Entry entry : group.getEntries()) {
            menu.add(new ComponentEntryPanel(entry));
        }
        return menu;
    }

    private JComponent buildCustomHeader(JPopupMenu menu) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.COMPONENT_DROPDOWN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JButton newButton = new JButton("New Custom Component");
        newButton.setFocusPainted(false);
        newButton.setBackground(Colors.COMPONENT_BAR_BG);
        newButton.setForeground(Colors.COMPONENT_BAR_TEXT);
        newButton.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        newButton.addActionListener(event -> {
            menu.setVisible(false);
            circuitPanel.requestCreateCustomComponent();
        });
        header.add(newButton, BorderLayout.CENTER);
        return header;
    }

    private class ComponentEntryPanel extends JPanel {
        private final ComponentRegistry.Entry entry;
        private Point pressScreenPoint;
        private boolean dragging;

        private ComponentEntryPanel(ComponentRegistry.Entry entry) {
            this.entry = entry;
            setLayout(new BorderLayout(8, 0));
            setBackground(Colors.COMPONENT_DROPDOWN_BG);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            ComponentPreview preview = new ComponentPreview(entry);
            JLabel name = new JLabel(entry.getName());
            name.setForeground(Colors.COMPONENT_ENTRY_TEXT);
            add(preview, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            if (entry.isCustom()) {
                add(buildCustomActions(entry), BorderLayout.EAST);
            }
            setPreferredSize(new Dimension(200, ENTRY_HEIGHT));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(Colors.COMPONENT_ENTRY_HOVER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(Colors.COMPONENT_DROPDOWN_BG);
                    if (dragging) {
                        return;
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressScreenPoint = e.getLocationOnScreen();
                    dragging = false;
                    cancelHide();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (pressScreenPoint == null) {
                        return;
                    }
                    if (entry.isCustom() && isClickOnAction(e.getPoint())) {
                        pressScreenPoint = null;
                        dragging = false;
                        return;
                    }
                    Point releasePoint = e.getLocationOnScreen();
                    if (dragging) {
                        circuitPanel.placeComponentAtScreenPoint(entry, releasePoint);
                        circuitPanel.endPlacementMode();
                        hideActiveMenu();
                    } else {
                        circuitPanel.beginPlacementMode(entry);
                        circuitPanel.updatePlacementFromScreenPoint(releasePoint);
                        hideActiveMenu();
                    }
                    pressScreenPoint = null;
                    dragging = false;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (pressScreenPoint == null) {
                        return;
                    }
                    Point dragPoint = e.getLocationOnScreen();
                    if (!dragging && pressScreenPoint.distance(dragPoint) >= DRAG_THRESHOLD) {
                        dragging = true;
                        circuitPanel.beginPlacementMode(entry);
                    }
                    if (dragging) {
                        circuitPanel.updatePlacementFromScreenPoint(dragPoint);
                    }
                }
            });
        }

        private boolean isClickOnAction(Point point) {
            Component target = getComponentAt(point);
            if (target == null) {
                return false;
            }
            return target instanceof JButton
                    || SwingUtilities.getAncestorOfClass(JButton.class, target) != null;
        }

        private JComponent buildCustomActions(ComponentRegistry.Entry entry) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            panel.setOpaque(false);
            JButton editButton = new JButton("Edit");
            styleActionButton(editButton);
            editButton.addActionListener(event -> {
                circuitPanel.requestEditCustomComponent(entry.getCustomId());
            });
            JButton deleteButton = new JButton("Del");
            styleActionButton(deleteButton);
            deleteButton.addActionListener(event -> {
                circuitPanel.requestDeleteCustomComponent(entry.getCustomId());
            });
            panel.add(editButton);
            panel.add(deleteButton);
            return panel;
        }

        private void styleActionButton(JButton button) {
            button.setFocusPainted(false);
            button.setBackground(Colors.COMPONENT_BAR_BG);
            button.setForeground(Colors.COMPONENT_ENTRY_TEXT);
            button.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            button.setFocusable(false);
        }
    }

    private static class ComponentPreview extends JComponent {
        private final CircuitComponent component;

        private ComponentPreview(ComponentRegistry.Entry entry) {
            this.component = entry.create(0, 0);
            setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            setMinimumSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int availableWidth = Math.max(1, getWidth() - 6);
            int availableHeight = Math.max(1, getHeight() - 6);
            int compWidth = Math.max(1, component.getWidth());
            int compHeight = Math.max(1, component.getHeight());
            double scale = Math.min(availableWidth / (double) compWidth, availableHeight / (double) compHeight);
            double drawWidth = compWidth * scale;
            double drawHeight = compHeight * scale;
            double offsetX = (getWidth() - drawWidth) / 2.0;
            double offsetY = (getHeight() - drawHeight) / 2.0;
            java.awt.geom.AffineTransform original = g2.getTransform();
            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);
            component.draw(g2);
            g2.setTransform(original);
        }
    }
}
