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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final List<CircuitComponent> selectedComponents = new ArrayList<>();
    private final List<Wire> selectedWires = new ArrayList<>();
    private final Map<CircuitComponent, Integer> selectionBaseRotations = new HashMap<>();
    private final ShortCircuitPopup shortCircuitPopup = new ShortCircuitPopup();
    private boolean lastShortCircuit = false;
    private CircuitComponent draggedComponent;
    private CircuitComponent selectedComponent;
    private Wire selectedWire;
    private WireNode newWireStartNode;
    private boolean creatingWire;
    private boolean lockedWire;
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
    private boolean selectingArea;
    private int selectionStartX;
    private int selectionStartY;
    private int selectionEndX;
    private int selectionEndY;
    private boolean draggingSelection;
    private int selectionDragStartX;
    private int selectionDragStartY;
    private int selectionRotationTurns;

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
                boolean toggleSelection = e.isControlDown();
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                if (creatingWire && lockedWire) {
                    int endX = Grid.snap(e.getX());
                    int endY = Grid.snap(e.getY());
                    WireNode endNode = getOrCreateNodeAt(endX, endY);
                    if (newWireStartNode != null && endNode != null
                            && (newWireStartNode.getX() != endNode.getX()
                            || newWireStartNode.getY() != endNode.getY())) {
                        wires.add(new Wire(newWireStartNode, endNode));
                    }
                    creatingWire = false;
                    lockedWire = false;
                    newWireStartNode = null;
                    repaint();
                    return;
                }
                ConnectionPoint hitPoint = findConnectionPointAt(e.getX(), e.getY());
                if (hitPoint != null) {
                    if (toggleSelection) {
                        toggleComponentSelection(hitPoint.getOwner());
                        repaint();
                        return;
                    }
                    selectComponent(hitPoint.getOwner());
                    int startX = selectedComponent.getConnectionPointWorldX(hitPoint);
                    int startY = selectedComponent.getConnectionPointWorldY(hitPoint);
                    newWireStartNode = getOrCreateNodeAt(startX, startY);
                    creatingWire = true;
                    lockedWire = e.isShiftDown();
                    wireDragX = startX;
                    wireDragY = startY;
                    repaint();
                    return;
                }
                WireEndpointHit endpointHit = findWireEndpointAt(e.getX(), e.getY());
                if (endpointHit != null) {
                    if (toggleSelection) {
                        toggleWireSelection(endpointHit.wire);
                        repaint();
                        return;
                    }
                    newWireStartNode = endpointHit.node;
                    creatingWire = true;
                    lockedWire = e.isShiftDown();
                    selectWire(endpointHit.wire);
                    wireDragX = newWireStartNode.getX();
                    wireDragY = newWireStartNode.getY();
                    repaint();
                    return;
                }
                WireHit wireHit = findWireAt(e.getX(), e.getY());
                if (wireHit != null) {
                    if (toggleSelection) {
                        toggleWireSelection(wireHit.wire);
                        repaint();
                        return;
                    }
                    if (isMultiSelection() && selectedWires.contains(wireHit.wire)) {
                        beginSelectionDrag(e);
                        return;
                    }
                    draggingWire = wireHit.wire;
                    selectWire(wireHit.wire);
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
                        if (toggleSelection) {
                            toggleComponentSelection(component);
                            repaint();
                            return;
                        }
                        if (isMultiSelection() && selectedComponents.contains(component)) {
                            beginSelectionDrag(e);
                            return;
                        }
                        selectComponent(component);
                        if (isInRotateHandle(component, e.getX(), e.getY())) {
                            rotateSelectionGroup();
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
                if (!isMultiSelection() && selectedComponent != null
                        && isInRotateHandle(selectedComponent, e.getX(), e.getY())) {
                    rotateSelectionGroup();
                    repaint();
                    return;
                }
                if (toggleSelection) {
                    return;
                }
                clearSelection();
                draggedComponent = null;
                resizing = false;
                selectingArea = true;
                selectionStartX = e.getX();
                selectionStartY = e.getY();
                selectionEndX = e.getX();
                selectionEndY = e.getY();
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
                if (selectingArea) {
                    selectionEndX = e.getX();
                    selectionEndY = e.getY();
                    repaint();
                    return;
                }
                if (draggingSelection) {
                    dragSelectionTo(e.getX(), e.getY());
                    return;
                }
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
                if (lockedWire) {
                    return;
                }
                if (selectingArea) {
                    selectingArea = false;
                    selectionEndX = e.getX();
                    selectionEndY = e.getY();
                    updateSelectionFromArea();
                    repaint();
                    return;
                }
                if (draggingSelection) {
                    draggingSelection = false;
                    repaint();
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

            @Override
            public void mouseMoved(MouseEvent e) {
                if (creatingWire) {
                    wireDragX = Grid.snap(e.getX());
                    wireDragY = Grid.snap(e.getY());
                    repaint();
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        configureDeleteKeyBindings();
        configureRotateKeyBindings();
        configureMoveKeyBindings();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid((Graphics2D) g);
        drawWires((Graphics2D) g);
        drawWirePreview((Graphics2D) g);
        drawSelectedWires((Graphics2D) g);
        for (CircuitComponent component : components) {
            component.draw(g);
        }
        if (!selectedComponents.isEmpty()) {
            boolean drawHandles = selectedComponents.size() == 1 && selectedWires.isEmpty();
            for (CircuitComponent component : selectedComponents) {
                drawSelection((Graphics2D) g, component, drawHandles);
            }
        }
        if (selectingArea) {
            drawSelectionArea((Graphics2D) g);
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

    private void drawSelection(Graphics2D g2, CircuitComponent component, boolean drawHandles) {
        java.awt.Color originalColor = g2.getColor();
        g2.setColor(Colors.SELECTION);
        java.awt.Rectangle bounds = component.getBounds();
        g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        if (drawHandles) {
            int handleX = bounds.x + bounds.width - RESIZE_HANDLE_SIZE;
            int handleY = bounds.y + bounds.height - RESIZE_HANDLE_SIZE;
            g2.fillRect(handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
            drawRotateHandle(g2, bounds);
        }
        g2.setColor(originalColor);
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

    private void drawSelectedWires(Graphics2D g2) {
        if (selectedWires.isEmpty()) {
            return;
        }
        java.awt.Color originalColor = g2.getColor();
        java.awt.Stroke originalStroke = g2.getStroke();
        g2.setColor(Colors.SELECTION);
        g2.setStroke(new java.awt.BasicStroke(5f));
        for (Wire wire : selectedWires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            g2.drawLine(wire.getStart().getX(), wire.getStart().getY(),
                    wire.getEnd().getX(), wire.getEnd().getY());
        }
        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
    }

    private void drawSelectionArea(Graphics2D g2) {
        java.awt.Color original = g2.getColor();
        g2.setColor(Colors.SELECTION);
        java.awt.Rectangle area = getSelectionRectangle();
        g2.drawRect(area.x, area.y, area.width, area.height);
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
            selectWire(wireHit.wire);
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
        selectComponent(component);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(event -> {
            component.disconnectAllConnections();
            components.remove(component);
            clearSelection();
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
        selectedWires.remove(wire);
        if (selectedWire == wire) {
            selectedWire = null;
        }
        wire.detach();
        wires.remove(wire);
        updatePropertiesPanel();
        return true;
    }

    private void deleteSelected() {
        if (selectedComponents.isEmpty() && selectedWires.isEmpty()) {
            return;
        }
        List<CircuitComponent> toDeleteComponents = new ArrayList<>(selectedComponents);
        List<Wire> toDeleteWires = new ArrayList<>(selectedWires);
        for (CircuitComponent component : toDeleteComponents) {
            component.disconnectAllConnections();
            components.remove(component);
        }
        for (Wire wire : toDeleteWires) {
            removeWire(wire);
        }
        clearSelection();
        repaint();
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

    private void configureRotateKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotateSelection");
        actionMap.put("rotateSelection", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!selectedComponents.isEmpty()) {
                    rotateSelectionGroup();
                    repaint();
                }
            }
        });
    }

    private void configureMoveKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveSelectionLeft");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveSelectionRight");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveSelectionUp");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveSelectionDown");
        actionMap.put("moveSelectionLeft", new MoveSelectionAction(-Grid.SIZE, 0));
        actionMap.put("moveSelectionRight", new MoveSelectionAction(Grid.SIZE, 0));
        actionMap.put("moveSelectionUp", new MoveSelectionAction(0, -Grid.SIZE));
        actionMap.put("moveSelectionDown", new MoveSelectionAction(0, Grid.SIZE));
    }

    private class MoveSelectionAction extends javax.swing.AbstractAction {
        private final int dx;
        private final int dy;

        private MoveSelectionAction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (!selectedComponents.isEmpty() || !selectedWires.isEmpty()) {
                moveSelectionBy(dx, dy);
                repaint();
            }
        }
    }

    private void selectComponent(CircuitComponent component) {
        selectedComponents.clear();
        selectedWires.clear();
        if (component != null) {
            selectedComponents.add(component);
        }
        selectedComponent = component;
        selectedWire = null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void selectWire(Wire wire) {
        selectedComponents.clear();
        selectedWires.clear();
        if (wire != null) {
            selectedWires.add(wire);
        }
        selectedWire = wire;
        selectedComponent = null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void setMultiSelection(List<CircuitComponent> components, List<Wire> wires) {
        selectedComponents.clear();
        selectedComponents.addAll(components);
        selectedWires.clear();
        selectedWires.addAll(wires);
        selectedComponent = (selectedComponents.size() == 1 && selectedWires.isEmpty())
                ? selectedComponents.get(0)
                : null;
        selectedWire = (selectedWires.size() == 1 && selectedComponents.isEmpty())
                ? selectedWires.get(0)
                : null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void toggleComponentSelection(CircuitComponent component) {
        if (component == null) {
            return;
        }
        if (selectedComponents.contains(component)) {
            selectedComponents.remove(component);
        } else {
            selectedComponents.add(component);
        }
        selectedWires.removeIf(wire -> wire == null);
        selectedComponent = (selectedComponents.size() == 1 && selectedWires.isEmpty())
                ? selectedComponents.get(0)
                : null;
        selectedWire = (selectedWires.size() == 1 && selectedComponents.isEmpty())
                ? selectedWires.get(0)
                : null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void toggleWireSelection(Wire wire) {
        if (wire == null) {
            return;
        }
        if (selectedWires.contains(wire)) {
            selectedWires.remove(wire);
        } else {
            selectedWires.add(wire);
        }
        selectedComponents.removeIf(component -> component == null);
        selectedComponent = (selectedComponents.size() == 1 && selectedWires.isEmpty())
                ? selectedComponents.get(0)
                : null;
        selectedWire = (selectedWires.size() == 1 && selectedComponents.isEmpty())
                ? selectedWires.get(0)
                : null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void clearSelection() {
        selectedComponents.clear();
        selectedWires.clear();
        selectedComponent = null;
        selectedWire = null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    private void updatePropertiesPanel() {
        if (selectedComponents.size() == 1 && selectedWires.isEmpty()) {
            CircuitComponent component = selectedComponents.get(0);
            if (component == null || component instanceof Ammeter || component instanceof Voltmeter) {
                propertiesPanel.setOwner(null);
            } else {
                propertiesPanel.setOwner(component);
            }
        } else {
            propertiesPanel.setOwner(null);
        }
    }

    private boolean isMultiSelection() {
        return selectedComponents.size() + selectedWires.size() > 1;
    }

    private void beginSelectionDrag(MouseEvent e) {
        draggingSelection = true;
        selectionDragStartX = Grid.snap(e.getX());
        selectionDragStartY = Grid.snap(e.getY());
        draggedComponent = null;
        draggingWire = null;
        resizing = false;
        for (Wire wire : selectedWires) {
            detachSharedEndpoints(wire);
        }
    }

    private void dragSelectionTo(int mouseX, int mouseY) {
        int snappedX = Grid.snap(mouseX);
        int snappedY = Grid.snap(mouseY);
        int dx = snappedX - selectionDragStartX;
        int dy = snappedY - selectionDragStartY;
        if (dx == 0 && dy == 0) {
            return;
        }
        moveSelectionBy(dx, dy);
        selectionDragStartX = snappedX;
        selectionDragStartY = snappedY;
        repaint();
    }

    private void moveSelectionBy(int dx, int dy) {
        for (CircuitComponent component : selectedComponents) {
            component.setPosition(component.getX() + dx, component.getY() + dy);
        }
        for (Wire wire : selectedWires) {
            wire.moveBy(dx, dy);
        }
    }

    private void rotateSelectionGroup() {
        if (selectedComponents.isEmpty() && selectedWires.isEmpty()) {
            return;
        }
        java.awt.Rectangle bounds = getSelectionBounds();
        if (bounds == null) {
            return;
        }
        selectionRotationTurns = (selectionRotationTurns + 1) % 4;
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();
        Set<WireNode> nodesToRotate = prepareWireNodesForRotation();
        for (CircuitComponent component : selectedComponents) {
            int baseRotation = selectionBaseRotations.getOrDefault(component,
                    component.getRotationQuarterTurns() % 2);
            int desiredRotation = (baseRotation + (selectionRotationTurns % 2)) % 2;
            int currentRotation = component.getRotationQuarterTurns() % 2;
            boolean rotateComponent = desiredRotation != currentRotation;
            rotateComponentAround(component, centerX, centerY, rotateComponent);
        }
        for (WireNode node : nodesToRotate) {
            rotateWireNodeAround(node, centerX, centerY);
        }
    }

    private java.awt.Rectangle getSelectionBounds() {
        boolean hasSelection = false;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (CircuitComponent component : selectedComponents) {
            java.awt.Rectangle bounds = component.getBounds();
            minX = Math.min(minX, bounds.x);
            minY = Math.min(minY, bounds.y);
            maxX = Math.max(maxX, bounds.x + bounds.width);
            maxY = Math.max(maxY, bounds.y + bounds.height);
            hasSelection = true;
        }
        for (Wire wire : selectedWires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            int x1 = wire.getStart().getX();
            int y1 = wire.getStart().getY();
            int x2 = wire.getEnd().getX();
            int y2 = wire.getEnd().getY();
            minX = Math.min(minX, Math.min(x1, x2));
            minY = Math.min(minY, Math.min(y1, y2));
            maxX = Math.max(maxX, Math.max(x1, x2));
            maxY = Math.max(maxY, Math.max(y1, y2));
            hasSelection = true;
        }
        if (!hasSelection) {
            return null;
        }
        return new java.awt.Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Set<WireNode> prepareWireNodesForRotation() {
        Set<WireNode> nodesToRotate = new HashSet<>();
        if (selectedWires.isEmpty()) {
            return nodesToRotate;
        }
        Set<Wire> selectedWireSet = new HashSet<>(selectedWires);
        Map<WireNode, WireNode> detachedNodes = new HashMap<>();
        for (Wire wire : selectedWires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start != null) {
                WireNode nodeToUse = resolveRotationNode(start, selectedWireSet, detachedNodes);
                if (nodeToUse != start) {
                    wire.setStart(nodeToUse);
                }
                nodesToRotate.add(nodeToUse);
            }
            if (end != null) {
                WireNode nodeToUse = resolveRotationNode(end, selectedWireSet, detachedNodes);
                if (nodeToUse != end) {
                    wire.setEnd(nodeToUse);
                }
                nodesToRotate.add(nodeToUse);
            }
        }
        return nodesToRotate;
    }

    private WireNode resolveRotationNode(WireNode node, Set<Wire> selectedWireSet,
            Map<WireNode, WireNode> detachedNodes) {
        boolean hasUnselected = false;
        for (Wire wire : node.getWires()) {
            if (!selectedWireSet.contains(wire)) {
                hasUnselected = true;
                break;
            }
        }
        if (!hasUnselected) {
            return node;
        }
        WireNode replacement = detachedNodes.get(node);
        if (replacement == null) {
            replacement = new WireNode(node.getX(), node.getY());
            detachedNodes.put(node, replacement);
        }
        return replacement;
    }

    private void rotateComponentAround(CircuitComponent component, double centerX, double centerY,
            boolean rotateComponent) {
        double componentCenterX = component.getX() + (component.getWidth() / 2.0);
        double componentCenterY = component.getY() + (component.getHeight() / 2.0);
        double rotatedX = centerX - (componentCenterY - centerY);
        double rotatedY = centerY + (componentCenterX - centerX);
        if (rotateComponent) {
            component.rotate90();
        }
        int newX = Grid.snap((int) Math.round(rotatedX - (component.getWidth() / 2.0)));
        int newY = Grid.snap((int) Math.round(rotatedY - (component.getHeight() / 2.0)));
        component.setPosition(newX, newY);
    }

    private void rotateWireNodeAround(WireNode node, double centerX, double centerY) {
        double rotatedX = centerX - (node.getY() - centerY);
        double rotatedY = centerY + (node.getX() - centerX);
        node.setPosition(Grid.snap((int) Math.round(rotatedX)), Grid.snap((int) Math.round(rotatedY)));
    }

    private void resetSelectionRotationState() {
        selectionRotationTurns = 0;
        selectionBaseRotations.clear();
        for (CircuitComponent component : selectedComponents) {
            selectionBaseRotations.put(component, component.getRotationQuarterTurns() % 2);
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

    private void updateSelectionFromArea() {
        java.awt.Rectangle area = getSelectionRectangle();
        if (area.width == 0 && area.height == 0) {
            clearSelection();
            return;
        }
        List<CircuitComponent> areaComponents = new ArrayList<>();
        List<Wire> areaWires = new ArrayList<>();
        for (CircuitComponent component : components) {
            if (area.intersects(component.getBounds())) {
                areaComponents.add(component);
            }
        }
        for (Wire wire : wires) {
            if (wireIntersectsArea(wire, area)) {
                areaWires.add(wire);
            }
        }
        setMultiSelection(areaComponents, areaWires);
    }

    private java.awt.Rectangle getSelectionRectangle() {
        int x = Math.min(selectionStartX, selectionEndX);
        int y = Math.min(selectionStartY, selectionEndY);
        int width = Math.abs(selectionEndX - selectionStartX);
        int height = Math.abs(selectionEndY - selectionStartY);
        return new java.awt.Rectangle(x, y, width, height);
    }

    private boolean wireIntersectsArea(Wire wire, java.awt.Rectangle area) {
        if (wire == null || wire.getStart() == null || wire.getEnd() == null) {
            return false;
        }
        int x1 = wire.getStart().getX();
        int y1 = wire.getStart().getY();
        int x2 = wire.getEnd().getX();
        int y2 = wire.getEnd().getY();
        if (area.contains(x1, y1) || area.contains(x2, y2)) {
            return true;
        }
        java.awt.geom.Line2D line = new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
        return area.intersectsLine(line);
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
