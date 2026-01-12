package circuitsim.ui;

import circuitsim.components.Ammeter;
import circuitsim.components.CircuitComponent;
import circuitsim.components.ComponentRegistry;
import circuitsim.components.ConnectionPoint;
import circuitsim.components.Voltmeter;
import circuitsim.components.WireNode;
import circuitsim.components.Wire;
import circuitsim.physics.CircuitPhysics;
import circuitsim.ui.ShortCircuitPopup;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class CircuitPanel extends JPanel {
    private final List<CircuitComponent> components = new ArrayList<>();
    private final ComponentPropertiesPanel propertiesPanel;
    private final List<Wire> wires = new ArrayList<>();
    private final ShortCircuitPopup shortCircuitPopup = new ShortCircuitPopup();
    private boolean lastShortCircuit = false;
    private CircuitComponent draggedComponent;
    private CircuitComponent selectedComponent;
    private Wire selectedWire;
    private WireNode newWireStartNode;
    private boolean creatingWire;
    private Wire draggingWire;
    private int wireStartAX;
    private int wireStartAY;
    private int wireStartBX;
    private int wireStartBY;
    private int wireDragStartX;
    private int wireDragStartY;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean resizing;
    private int wireDragX;
    private int wireDragY;
    private static final int RESIZE_HANDLE_SIZE = 10;
    private static final int MIN_COMPONENT_SIZE = 20;
    private static final int ROTATE_HANDLE_SIZE = 16;
    private static final int WIRE_ENDPOINT_RADIUS = 8;

    public CircuitPanel(ComponentPropertiesPanel propertiesPanel) {
        this.propertiesPanel = propertiesPanel;
        setBackground(Colors.CANVAS_BG);
        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setFocusable(true);
        add(shortCircuitPopup);
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                ConnectionPoint hitPoint = findConnectionPointAt(e.getX(), e.getY());
                if (hitPoint != null) {
                    setSelectedComponent(hitPoint.getOwner());
                    selectedWire = null;
                    int startX = selectedComponent.getConnectionPointWorldX(hitPoint);
                    int startY = selectedComponent.getConnectionPointWorldY(hitPoint);
                    newWireStartNode = getOrCreateNodeAt(startX, startY);
                    creatingWire = true;
                    wireDragX = startX;
                    wireDragY = startY;
                    repaint();
                    return;
                }
                WireEndpointHit endpointHit = findWireEndpointAt(e.getX(), e.getY());
                if (endpointHit != null) {
                    newWireStartNode = endpointHit.node;
                    creatingWire = true;
                    selectedWire = endpointHit.wire;
                    setSelectedComponent(null);
                    wireDragX = newWireStartNode.getX();
                    wireDragY = newWireStartNode.getY();
                    repaint();
                    return;
                }
                WireHit wireHit = findWireAt(e.getX(), e.getY());
                if (wireHit != null) {
                    draggingWire = wireHit.wire;
                    selectedWire = wireHit.wire;
                    setSelectedComponent(null);
                    wireDragStartX = Grid.snap(e.getX());
                    wireDragStartY = Grid.snap(e.getY());
                    wireStartAX = draggingWire.getStart().getX();
                    wireStartAY = draggingWire.getStart().getY();
                    wireStartBX = draggingWire.getEnd().getX();
                    wireStartBY = draggingWire.getEnd().getY();
                    detachSharedEndpoints(draggingWire);
                    repaint();
                    return;
                }
                for (int i = components.size() - 1; i >= 0; i--) {
                    CircuitComponent component = components.get(i);
                    if (component.contains(e.getX(), e.getY())) {
                        setSelectedComponent(component);
                        selectedWire = null;
                        if (isInRotateHandle(component, e.getX(), e.getY())) {
                            component.rotate90();
                            repaint();
                            return;
                        }
                        draggedComponent = component;
                        resizing = isInResizeHandle(component, e.getX(), e.getY());
                        if (!resizing) {
                            dragOffsetX = e.getX() - component.getX();
                            dragOffsetY = e.getY() - component.getY();
                        }
                        components.remove(i);
                        components.add(component);
                        repaint();
                        return;
                    }
                }
                if (selectedComponent != null && isInRotateHandle(selectedComponent, e.getX(), e.getY())) {
                    selectedComponent.rotate90();
                    repaint();
                    return;
                }
                setSelectedComponent(null);
                selectedWire = null;
                draggedComponent = null;
                resizing = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (removeWireAt(e.getX(), e.getY())) {
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (creatingWire) {
                    wireDragX = Grid.snap(e.getX());
                    wireDragY = Grid.snap(e.getY());
                    repaint();
                    return;
                }
                if (draggingWire != null) {
                    int dx = Grid.snap(e.getX()) - wireDragStartX;
                    int dy = Grid.snap(e.getY()) - wireDragStartY;
                    draggingWire.getStart().setPosition(wireStartAX + dx, wireStartAY + dy);
                    draggingWire.getEnd().setPosition(wireStartBX + dx, wireStartBY + dy);
                    repaint();
                    return;
                }
                if (draggedComponent != null) {
                    if (resizing) {
                        int targetWidth = Math.max(MIN_COMPONENT_SIZE,
                                Grid.snap(e.getX()) - draggedComponent.getX());
                        int targetHeight = Math.max(MIN_COMPONENT_SIZE,
                                Grid.snap(e.getY()) - draggedComponent.getY());
                        draggedComponent.resizeKeepingRatio(targetWidth, targetHeight);
                        int snappedWidth = Grid.snapSize(draggedComponent.getWidth());
                        int snappedHeight = Grid.snapSize(draggedComponent.getHeight());
                        draggedComponent.setSize(snappedWidth, snappedHeight);
                    } else {
                        int snappedX = Grid.snap(e.getX() - dragOffsetX);
                        int snappedY = Grid.snap(e.getY() - dragOffsetY);
                        draggedComponent.setPosition(snappedX, snappedY);
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                if (creatingWire) {
                    int endX = Grid.snap(e.getX());
                    int endY = Grid.snap(e.getY());
                    WireNode endNode = getOrCreateNodeAt(endX, endY);
                    if (newWireStartNode != null && endNode != null
                            && (newWireStartNode.getX() != endNode.getX()
                            || newWireStartNode.getY() != endNode.getY())) {
                        wires.add(new Wire(newWireStartNode, endNode));
                    }
                    creatingWire = false;
                    newWireStartNode = null;
                    repaint();
                    return;
                }
                if (draggingWire != null) {
                    draggingWire = null;
                    repaint();
                    return;
                }
                draggedComponent = null;
                resizing = false;
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        configureDeleteKeyBindings();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid((Graphics2D) g);
        drawWires((Graphics2D) g);
        drawWirePreview((Graphics2D) g);
        for (CircuitComponent component : components) {
            component.draw(g);
        }
        if (selectedComponent != null) {
            g.setColor(Colors.SELECTION);
            java.awt.Rectangle bounds = selectedComponent.getBounds();
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            int handleX = bounds.x + bounds.width - RESIZE_HANDLE_SIZE;
            int handleY = bounds.y + bounds.height - RESIZE_HANDLE_SIZE;
            g.fillRect(handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
            drawRotateHandle((Graphics2D) g, bounds);
        }
    }

    private void drawWirePreview(Graphics2D g2) {
        if (!creatingWire || newWireStartNode == null) {
            return;
        }
        Color originalColor = g2.getColor();
        g2.setColor(Colors.WIRE);
        g2.drawLine(newWireStartNode.getX(), newWireStartNode.getY(), wireDragX, wireDragY);
        g2.setColor(originalColor);
    }

    private void drawWires(Graphics2D g2) {
        boolean shortCircuit = CircuitPhysics.update(components, wires);
        if (shortCircuit != lastShortCircuit) {
            if (shortCircuit) {
                shortCircuitPopup.showPopup();
            } else {
                shortCircuitPopup.hidePopup();
            }
            lastShortCircuit = shortCircuit;
        }
        for (Wire wire : wires) {
            wire.setShortCircuit(shortCircuit);
        }
        for (Wire wire : wires) {
            wire.draw(g2);
        }
    }

    private boolean isInResizeHandle(CircuitComponent component, int mouseX, int mouseY) {
        java.awt.Rectangle bounds = component.getBounds();
        int handleX = bounds.x + bounds.width - RESIZE_HANDLE_SIZE;
        int handleY = bounds.y + bounds.height - RESIZE_HANDLE_SIZE;
        return mouseX >= handleX && mouseX <= handleX + RESIZE_HANDLE_SIZE
                && mouseY >= handleY && mouseY <= handleY + RESIZE_HANDLE_SIZE;
    }

    private boolean isInRotateHandle(CircuitComponent component, int mouseX, int mouseY) {
        java.awt.Rectangle bounds = component.getBounds();
        int handleX = bounds.x + bounds.width - ROTATE_HANDLE_SIZE;
        int handleY = bounds.y - ROTATE_HANDLE_SIZE;
        return mouseX >= handleX && mouseX <= handleX + ROTATE_HANDLE_SIZE
                && mouseY >= handleY && mouseY <= handleY + ROTATE_HANDLE_SIZE;
    }

    private void drawRotateHandle(Graphics2D g2, java.awt.Rectangle bounds) {
        int x = bounds.x + bounds.width - ROTATE_HANDLE_SIZE;
        int y = bounds.y - ROTATE_HANDLE_SIZE;
        Color original = g2.getColor();
        g2.setColor(Colors.ROTATE_HANDLE);
        g2.drawArc(x + 2, y + 2, ROTATE_HANDLE_SIZE - 4, ROTATE_HANDLE_SIZE - 4, 45, 270);
        int arrowX = x + ROTATE_HANDLE_SIZE - 6;
        int arrowY = y + 4;
        g2.drawLine(arrowX, arrowY, arrowX - 5, arrowY + 2);
        g2.drawLine(arrowX, arrowY, arrowX - 2, arrowY + 7);
        g2.setColor(original);
    }

    private void drawGrid(Graphics2D g2) {
        Color original = g2.getColor();
        g2.setColor(Colors.GRID_LINE);
        int width = getWidth();
        int height = getHeight();
        for (int x = 0; x <= width; x += Grid.SIZE) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y += Grid.SIZE) {
            g2.drawLine(0, y, width, y);
        }
        g2.setColor(original);
    }

    private void showContextMenu(MouseEvent e) {
        WireHit wireHit = findWireAt(e.getX(), e.getY());
        if (wireHit != null) {
            selectedWire = wireHit.wire;
            setSelectedComponent(null);
            JPopupMenu menu = new JPopupMenu();
            JCheckBoxMenuItem showDataItem = new JCheckBoxMenuItem("Show Data", wireHit.wire.isShowData());
            showDataItem.addActionListener(event -> {
                wireHit.wire.setShowData(showDataItem.isSelected());
                repaint();
            });
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(event -> {
                removeWire(wireHit.wire);
                repaint();
            });
            menu.add(showDataItem);
            menu.add(deleteItem);
            menu.show(this, e.getX(), e.getY());
            return;
        }
        CircuitComponent component = findComponentAtPoint(e.getX(), e.getY());
        if (component == null) {
            JPopupMenu menu = new JPopupMenu();
            JMenu addMenu = new JMenu("Add");
            for (ComponentRegistry.Entry entry : ComponentRegistry.getEntries()) {
                JMenuItem item = new JMenuItem(entry.getName());
                item.addActionListener(event -> {
                    components.add(entry.create(Grid.snap(e.getX()), Grid.snap(e.getY())));
                    repaint();
                });
                addMenu.add(item);
            }
            menu.add(addMenu);
            menu.show(this, e.getX(), e.getY());
            return;
        }
        setSelectedComponent(component);
        selectedWire = null;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(event -> {
            component.disconnectAllConnections();
            components.remove(component);
            if (selectedComponent == component) {
                selectedComponent = null;
                propertiesPanel.setOwner(null);
            }
            repaint();
        });
        menu.add(deleteItem);
        menu.show(this, e.getX(), e.getY());
    }

    private CircuitComponent findComponentAtPoint(int mouseX, int mouseY) {
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            if (component.contains(mouseX, mouseY)) {
                return component;
            }
        }
        return null;
    }

    private boolean removeWireAt(int mouseX, int mouseY) {
        WireHit hit = findWireAt(mouseX, mouseY);
        if (hit == null) {
            return false;
        }
        return removeWire(hit.wire);
    }

    private boolean removeWire(Wire wire) {
        if (wire == null) {
            return false;
        }
        if (selectedWire == wire) {
            selectedWire = null;
        }
        wire.detach();
        wires.remove(wire);
        return true;
    }

    private void deleteSelected() {
        if (selectedComponent != null) {
            CircuitComponent component = selectedComponent;
            component.disconnectAllConnections();
            components.remove(component);
            setSelectedComponent(null);
            repaint();
            return;
        }
        if (selectedWire != null) {
            removeWire(selectedWire);
            repaint();
        }
    }

    private void configureDeleteKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelection");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteSelection");
        actionMap.put("deleteSelection", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelected();
            }
        });
    }

    private void setSelectedComponent(CircuitComponent component) {
        selectedComponent = component;
        if (component == null || component instanceof Ammeter || component instanceof Voltmeter) {
            propertiesPanel.setOwner(null);
        } else {
            propertiesPanel.setOwner(component);
        }
    }

    private WireHit findWireAt(int mouseX, int mouseY) {
        double maxDistance = 6.0;
        if (wires.isEmpty()) {
            return null;
        }
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (distanceToSegment(mouseX, mouseY, start.getX(), start.getY(), end.getX(), end.getY())
                    <= maxDistance) {
                return new WireHit(wire);
            }
        }
        return null;
    }

    private WireEndpointHit findWireEndpointAt(int mouseX, int mouseY) {
        int radiusSq = WIRE_ENDPOINT_RADIUS * WIRE_ENDPOINT_RADIUS;
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start != null) {
                int dx = mouseX - start.getX();
                int dy = mouseY - start.getY();
                if ((dx * dx) + (dy * dy) <= radiusSq) {
                    return new WireEndpointHit(wire, start);
                }
            }
            if (end != null) {
                int dx = mouseX - end.getX();
                int dy = mouseY - end.getY();
                if ((dx * dx) + (dy * dy) <= radiusSq) {
                    return new WireEndpointHit(wire, end);
                }
            }
        }
        return null;
    }

    private WireNode getOrCreateNodeAt(int x, int y) {
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start != null && start.getX() == x && start.getY() == y) {
                return start;
            }
            if (end != null && end.getX() == x && end.getY() == y) {
                return end;
            }
        }
        return new WireNode(x, y);
    }

    private void detachSharedEndpoints(Wire wire) {
        WireNode start = wire.getStart();
        WireNode end = wire.getEnd();
        if (start != null && start.getWireCount() > 1) {
            WireNode newStart = new WireNode(start.getX(), start.getY());
            wire.setStart(newStart);
        }
        if (end != null && end.getWireCount() > 1) {
            WireNode newEnd = new WireNode(end.getX(), end.getY());
            wire.setEnd(newEnd);
        }
    }

    private static class WireHit {
        private final Wire wire;

        private WireHit(Wire wire) {
            this.wire = wire;
        }
    }

    private static class WireEndpointHit {
        private final Wire wire;
        private final WireNode node;

        private WireEndpointHit(Wire wire, WireNode node) {
            this.wire = wire;
            this.node = node;
        }
    }

    private double distanceToSegment(int px, int py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = x1 + (t * dx);
        double projY = y1 + (t * dy);
        return Math.hypot(px - projX, py - projY);
    }

    private ConnectionPoint findConnectionPointAt(int mouseX, int mouseY) {
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            int radius = component.getConnectionDotSize() / 2;
            for (ConnectionPoint point : component.getConnectionPoints()) {
                int centerX = component.getConnectionPointWorldX(point);
                int centerY = component.getConnectionPointWorldY(point);
                int dx = mouseX - centerX;
                int dy = mouseY - centerY;
                if ((dx * dx) + (dy * dy) <= radius * radius) {
                    return point;
                }
            }
        }
        return null;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int padding = 12;
        int popupWidth = shortCircuitPopup.getPreferredSize().width;
        int popupHeight = shortCircuitPopup.getPreferredSize().height;
        shortCircuitPopup.setBounds(padding, getHeight() - popupHeight - padding, popupWidth, popupHeight);
    }
}
