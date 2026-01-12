package circuitsim.ui;

import circuitsim.components.Ammeter;
import circuitsim.components.CircuitComponent;
import circuitsim.components.ComponentRegistry;
import circuitsim.components.ConnectionPoint;
import circuitsim.components.Voltmeter;
import circuitsim.components.WireNode;
import circuitsim.components.Wire;
import circuitsim.components.WireColor;
import circuitsim.components.Switch;
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
    private WireColor activeWireColor = WireColor.WHITE;
    private List<RenderWire> lastRenderWires = new ArrayList<>();
    private static final int WIRE_OFFSET_STEP = 6;
    private static final int CROSSING_RADIUS = 7;
    private Wire pendingWireStartAnchor;
    private int viewOffsetX;
    private int viewOffsetY;
    private boolean panningView;
    private int panStartX;
    private int panStartY;
    private int panOriginOffsetX;
    private int panOriginOffsetY;
    private boolean panMoved;
    private JPopupMenu clearPopup;
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 0.1;

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
                    panningView = true;
                    panStartX = e.getX();
                    panStartY = e.getY();
                    panOriginOffsetX = viewOffsetX;
                    panOriginOffsetY = viewOffsetY;
                    panMoved = false;
                    return;
                }
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                if (creatingWire && lockedWire) {
                    int endX = Grid.snap(worldX);
                    int endY = Grid.snap(worldY);
                    WireEndpointHit endHit = findWireEndpointAt(endX, endY);
                    finalizeWire(endX, endY, endHit == null ? null : endHit.wire);
                    creatingWire = false;
                    lockedWire = false;
                    newWireStartNode = null;
                    pendingWireStartAnchor = null;
                    repaint();
                    return;
                }
                ConnectionPoint hitPoint = findConnectionPointAt(worldX, worldY);
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
                    pendingWireStartAnchor = null;
                    wireDragX = startX;
                    wireDragY = startY;
                    repaint();
                    return;
                }
                WireEndpointHit endpointHit = findWireEndpointAt(worldX, worldY);
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
                    pendingWireStartAnchor = endpointHit.wire;
                    wireDragX = newWireStartNode.getX();
                    wireDragY = newWireStartNode.getY();
                    repaint();
                    return;
                }
                WireHit wireHit = findWireAt(worldX, worldY);
                if (wireHit != null) {
                    if (toggleSelection) {
                        toggleWireSelection(wireHit.wire);
                        repaint();
                        return;
                    }
                    if (isMultiSelection() && selectedWires.contains(wireHit.wire)) {
                        beginSelectionDrag(worldX, worldY);
                        return;
                    }
                    draggingWire = wireHit.wire;
                    selectWire(wireHit.wire);
                    wireDragStartX = Grid.snap(worldX);
                    wireDragStartY = Grid.snap(worldY);
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
                    if (component.contains(worldX, worldY)) {
                        if (toggleSelection) {
                            toggleComponentSelection(component);
                            repaint();
                            return;
                        }
                        if (isMultiSelection() && selectedComponents.contains(component)) {
                            beginSelectionDrag(worldX, worldY);
                            return;
                        }
                        selectComponent(component);
                        if (isInRotateHandle(component, worldX, worldY)) {
                            rotateSelectionGroup();
                            repaint();
                            return;
                        }
                        draggedComponent = component;
                        resizing = isInResizeHandle(component, worldX, worldY);
                        if (!resizing) {
                            dragOffsetX = worldX - component.getX();
                            dragOffsetY = worldY - component.getY();
                        }
                        components.remove(i);
                        components.add(component);
                        repaint();
                        return;
                    }
                }
                if (!isMultiSelection() && selectedComponent != null
                        && isInRotateHandle(selectedComponent, worldX, worldY)) {
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
                selectionStartX = worldX;
                selectionStartY = worldY;
                selectionEndX = worldX;
                selectionEndY = worldY;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                if (e.getClickCount() == 2) {
                    if (removeWireAt(worldX, worldY)) {
                        repaint();
                    }
                    return;
                }
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    if (creatingWire || draggingWire != null || draggedComponent != null || resizing) {
                        return;
                    }
                    CircuitComponent component = findComponentAtPoint(worldX, worldY);
                    if (component instanceof Switch) {
                        ((Switch) component).toggle();
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!panningView && SwingUtilities.isRightMouseButton(e)) {
                    panningView = true;
                    panStartX = e.getX();
                    panStartY = e.getY();
                    panOriginOffsetX = viewOffsetX;
                    panOriginOffsetY = viewOffsetY;
                    panMoved = false;
                    selectingArea = false;
                    draggingSelection = false;
                    draggedComponent = null;
                    draggingWire = null;
                    resizing = false;
                    if (creatingWire && !lockedWire) {
                        creatingWire = false;
                        newWireStartNode = null;
                        pendingWireStartAnchor = null;
                    }
                }
                if (panningView) {
                    int dx = e.getX() - panStartX;
                    int dy = e.getY() - panStartY;
                    viewOffsetX = panOriginOffsetX + dx;
                    viewOffsetY = panOriginOffsetY + dy;
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                        panMoved = true;
                    }
                    repaint();
                    return;
                }
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                if (selectingArea) {
                    selectionEndX = worldX;
                    selectionEndY = worldY;
                    repaint();
                    return;
                }
                if (draggingSelection) {
                    dragSelectionTo(worldX, worldY);
                    return;
                }
                if (creatingWire) {
                    wireDragX = Grid.snap(worldX);
                    wireDragY = Grid.snap(worldY);
                    repaint();
                    return;
                }
                if (draggingWire != null) {
                    int dx = Grid.snap(worldX) - wireDragStartX;
                    int dy = Grid.snap(worldY) - wireDragStartY;
                    draggingWire.getStart().setPosition(wireStartAX + dx, wireStartAY + dy);
                    draggingWire.getEnd().setPosition(wireStartBX + dx, wireStartBY + dy);
                    repaint();
                    return;
                }
                if (draggedComponent != null) {
                    if (resizing) {
                        int targetWidth = Math.max(MIN_COMPONENT_SIZE,
                                Grid.snap(worldX) - draggedComponent.getX());
                        int targetHeight = Math.max(MIN_COMPONENT_SIZE,
                                Grid.snap(worldY) - draggedComponent.getY());
                        draggedComponent.resizeKeepingRatio(targetWidth, targetHeight);
                        int snappedWidth = Grid.snapSize(draggedComponent.getWidth());
                        int snappedHeight = Grid.snapSize(draggedComponent.getHeight());
                        draggedComponent.setSize(snappedWidth, snappedHeight);
                    } else {
                        int snappedX = Grid.snap(worldX - dragOffsetX);
                        int snappedY = Grid.snap(worldY - dragOffsetY);
                        draggedComponent.setPosition(snappedX, snappedY);
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                if (panningView) {
                    panningView = false;
                    if (!panMoved && (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger())) {
                        showContextMenu(e, worldX, worldY);
                    }
                    return;
                }
                if (lockedWire) {
                    return;
                }
                if (selectingArea) {
                    selectingArea = false;
                    selectionEndX = worldX;
                    selectionEndY = worldY;
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
                    int endX = Grid.snap(worldX);
                    int endY = Grid.snap(worldY);
                    WireEndpointHit endHit = findWireEndpointAt(endX, endY);
                    finalizeWire(endX, endY, endHit == null ? null : endHit.wire);
                    creatingWire = false;
                    newWireStartNode = null;
                    pendingWireStartAnchor = null;
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
                    int worldX = toWorldX(e.getX());
                    int worldY = toWorldY(e.getY());
                    wireDragX = Grid.snap(worldX);
                    wireDragY = Grid.snap(worldY);
                    repaint();
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(e -> {
            if (!e.isControlDown()) {
                return;
            }
            int rotation = e.getWheelRotation();
            if (rotation == 0) {
                return;
            }
            double delta = rotation < 0 ? ZOOM_STEP : -ZOOM_STEP;
            zoomAt(e.getX(), e.getY(), delta);
        });
        configureDeleteKeyBindings();
        configureRotateKeyBindings();
        configureMoveKeyBindings();
        configureZoomKeyBindings();
        configureResetViewKeyBindings();
        configureClearKeyBindings();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        java.awt.geom.AffineTransform originalTransform = g2.getTransform();
        g2.translate(viewOffsetX, viewOffsetY);
        g2.scale(zoomFactor, zoomFactor);
        drawGrid(g2);
        drawWires(g2);
        drawWirePreview(g2);
        drawSelectedWires(g2);
        for (CircuitComponent component : components) {
            component.draw(g2);
        }
        if (!selectedComponents.isEmpty()) {
            boolean drawHandles = selectedComponents.size() == 1 && selectedWires.isEmpty();
            for (CircuitComponent component : selectedComponents) {
                drawSelection(g2, component, drawHandles);
            }
        }
        if (selectingArea) {
            drawSelectionArea(g2);
        }
        g2.setTransform(originalTransform);
    }

    private void drawWirePreview(Graphics2D g2) {
        if (!creatingWire || newWireStartNode == null) {
            return;
        }
        Color originalColor = g2.getColor();
        g2.setColor(activeWireColor.getColor());
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
        lastRenderWires = buildRenderWires();
        for (RenderWire renderWire : lastRenderWires) {
            renderWire.wire.drawAt(g2, renderWire.x1, renderWire.y1, renderWire.x2, renderWire.y2);
        }
        drawWireCrossings(g2, lastRenderWires);
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
        Map<Wire, RenderWire> renderMap = new HashMap<>();
        for (RenderWire renderWire : lastRenderWires) {
            renderMap.put(renderWire.wire, renderWire);
        }
        for (Wire wire : selectedWires) {
            RenderWire renderWire = renderMap.get(wire);
            if (renderWire == null) {
                continue;
            }
            g2.drawLine(renderWire.x1, renderWire.y1, renderWire.x2, renderWire.y2);
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
        int leftWorld = (int) Math.floor(-viewOffsetX / zoomFactor);
        int topWorld = (int) Math.floor(-viewOffsetY / zoomFactor);
        int rightWorld = (int) Math.ceil((width - viewOffsetX) / zoomFactor);
        int bottomWorld = (int) Math.ceil((height - viewOffsetY) / zoomFactor);
        int startX = (int) Math.floor(leftWorld / (double) Grid.SIZE) * Grid.SIZE;
        int startY = (int) Math.floor(topWorld / (double) Grid.SIZE) * Grid.SIZE;
        for (int x = startX; x <= rightWorld; x += Grid.SIZE) {
            g2.drawLine(x, topWorld, x, bottomWorld);
        }
        for (int y = startY; y <= bottomWorld; y += Grid.SIZE) {
            g2.drawLine(leftWorld, y, rightWorld, y);
        }
        g2.setColor(original);
    }

    private void showContextMenu(MouseEvent e, int worldX, int worldY) {
        WireHit wireHit = findWireAt(worldX, worldY);
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
        CircuitComponent component = findComponentAtPoint(worldX, worldY);
        if (component == null) {
            JPopupMenu menu = new JPopupMenu();
            JMenu addMenu = new JMenu("Add");
            for (ComponentRegistry.Entry entry : ComponentRegistry.getEntries()) {
                JMenuItem item = new JMenuItem(entry.getName());
                item.addActionListener(event -> {
                    components.add(entry.create(Grid.snap(worldX), Grid.snap(worldY)));
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

    private void configureZoomKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ADD,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        actionMap.put("zoomIn", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAt(getWidth() / 2, getHeight() / 2, ZOOM_STEP);
            }
        });
        actionMap.put("zoomOut", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAt(getWidth() / 2, getHeight() / 2, -ZOOM_STEP);
            }
        });
    }

    private void configureResetViewKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "resetView");
        actionMap.put("resetView", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                resetView();
            }
        });
    }

    private void configureClearKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "clearBoard");
        actionMap.put("clearBoard", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                requestClearBoard();
            }
        });
    }

    private void resetView() {
        zoomFactor = 1.0;
        viewOffsetX = 0;
        viewOffsetY = 0;
        repaint();
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

    private void beginSelectionDrag(int worldX, int worldY) {
        draggingSelection = true;
        selectionDragStartX = Grid.snap(worldX);
        selectionDragStartY = Grid.snap(worldY);
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

    private void finalizeWire(int endX, int endY, Wire endAnchorWire) {
        WireSplitResult splitResult = splitWireAt(endX, endY);
        WireNode endNode = splitResult == null ? getOrCreateNodeAt(endX, endY) : splitResult.node;
        Wire resolvedEndAnchor = splitResult == null ? endAnchorWire : splitResult.anchorWire;
        if (newWireStartNode != null && endNode != null
                && (newWireStartNode.getX() != endNode.getX()
                || newWireStartNode.getY() != endNode.getY())) {
            Wire wire = new Wire(newWireStartNode, endNode, activeWireColor);
            if (pendingWireStartAnchor != null) {
                wire.setStartAnchorWire(pendingWireStartAnchor);
            }
            if (resolvedEndAnchor != null) {
                wire.setEndAnchorWire(resolvedEndAnchor);
            }
            wires.add(wire);
        }
    }

    private void resetSelectionRotationState() {
        selectionRotationTurns = 0;
        selectionBaseRotations.clear();
        for (CircuitComponent component : selectedComponents) {
            selectionBaseRotations.put(component, component.getRotationQuarterTurns() % 2);
        }
    }

    public void setActiveWireColor(WireColor color) {
        this.activeWireColor = color == null ? WireColor.WHITE : color;
    }

    public WireColor getActiveWireColor() {
        return activeWireColor;
    }

    public void requestClearBoard() {
        if (clearPopup != null && clearPopup.isVisible()) {
            clearPopup.setVisible(false);
            return;
        }
        clearPopup = new JPopupMenu();
        JMenuItem titleItem = new JMenuItem("Clear all components and wires?");
        titleItem.setEnabled(false);
        JMenuItem confirmItem = new JMenuItem("Clear Board");
        confirmItem.addActionListener(event -> {
            clearPopup.setVisible(false);
            clearBoard();
        });
        JMenuItem cancelItem = new JMenuItem("Cancel");
        cancelItem.addActionListener(event -> clearPopup.setVisible(false));
        clearPopup.add(titleItem);
        clearPopup.add(confirmItem);
        clearPopup.add(cancelItem);
        int popupX = Math.max(0, (getWidth() - clearPopup.getPreferredSize().width) / 2);
        int popupY = Math.max(0, (getHeight() - clearPopup.getPreferredSize().height) / 2);
        clearPopup.show(this, popupX, popupY);
    }

    private void clearBoard() {
        for (CircuitComponent component : new ArrayList<>(components)) {
            component.disconnectAllConnections();
        }
        components.clear();
        for (Wire wire : new ArrayList<>(wires)) {
            wire.detach();
        }
        wires.clear();
        clearSelection();
        repaint();
    }

    private void zoomAt(int screenX, int screenY, double delta) {
        double nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomFactor + delta));
        if (Math.abs(nextZoom - zoomFactor) < 0.0001) {
            return;
        }
        double worldX = (screenX - viewOffsetX) / zoomFactor;
        double worldY = (screenY - viewOffsetY) / zoomFactor;
        zoomFactor = nextZoom;
        viewOffsetX = (int) Math.round(screenX - (worldX * zoomFactor));
        viewOffsetY = (int) Math.round(screenY - (worldY * zoomFactor));
        repaint();
    }

    private int toWorldX(int screenX) {
        return (int) Math.round((screenX - viewOffsetX) / zoomFactor);
    }

    private int toWorldY(int screenY) {
        return (int) Math.round((screenY - viewOffsetY) / zoomFactor);
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

    private WireSplitResult splitWireAt(int x, int y) {
        int snappedX = Grid.snap(x);
        int snappedY = Grid.snap(y);
        Wire target = findWireForSplit(snappedX, snappedY);
        if (target == null) {
            return null;
        }
        WireNode start = target.getStart();
        WireNode end = target.getEnd();
        if (start == null || end == null) {
            return null;
        }
        WireNode splitNode = new WireNode(snappedX, snappedY);
        WireColor color = target.getWireColor();
        boolean showData = target.isShowData();
        target.detach();
        wires.remove(target);
        Wire first = new Wire(start, splitNode, color);
        Wire second = new Wire(splitNode, end, color);
        first.setShowData(showData);
        second.setShowData(showData);
        wires.add(first);
        wires.add(second);
        return new WireSplitResult(splitNode, first);
    }

    private Wire findWireForSplit(int x, int y) {
        double maxDistance = 6.0;
        int endpointRadiusSq = WIRE_ENDPOINT_RADIUS * WIRE_ENDPOINT_RADIUS;
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start == null || end == null) {
                continue;
            }
            int dxStart = x - start.getX();
            int dyStart = y - start.getY();
            int dxEnd = x - end.getX();
            int dyEnd = y - end.getY();
            if ((dxStart * dxStart) + (dyStart * dyStart) <= endpointRadiusSq
                    || (dxEnd * dxEnd) + (dyEnd * dyEnd) <= endpointRadiusSq) {
                continue;
            }
            if (distanceToSegment(x, y, start.getX(), start.getY(), end.getX(), end.getY()) <= maxDistance) {
                return wire;
            }
        }
        return null;
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

    private static class WireSplitResult {
        private final WireNode node;
        private final Wire anchorWire;

        private WireSplitResult(WireNode node, Wire anchorWire) {
            this.node = node;
            this.anchorWire = anchorWire;
        }
    }

    private static class RenderWire {
        private final Wire wire;
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;
        private final double angle;

        private RenderWire(Wire wire, int x1, int y1, int x2, int y2) {
            this.wire = wire;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.angle = Math.atan2(y2 - y1, x2 - x1);
        }
    }

    private static class WireCrossing {
        private final int x;
        private final int y;
        private final double angle;

        private WireCrossing(int x, int y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
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

    private List<RenderWire> buildRenderWires() {
        List<RenderWire> renderWires = new ArrayList<>();
        Map<Wire, Offset> offsets = computeWireOffsets();
        Map<WireNode, Offset> nodeOffsets = computeNodeOffsets(offsets);
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Offset wireOffset = offsets.getOrDefault(wire, new Offset(0, 0));
            Offset startOffset = resolveEndpointOffset(wire, wire.getStart(), true, wireOffset, offsets, nodeOffsets);
            Offset endOffset = resolveEndpointOffset(wire, wire.getEnd(), false, wireOffset, offsets, nodeOffsets);
            int startX = wire.getStart().getX() + startOffset.dx;
            int startY = wire.getStart().getY() + startOffset.dy;
            int endX = wire.getEnd().getX() + endOffset.dx;
            int endY = wire.getEnd().getY() + endOffset.dy;
            renderWires.add(new RenderWire(wire, startX, startY, endX, endY));
        }
        return renderWires;
    }

    private void drawWireCrossings(Graphics2D g2, List<RenderWire> renderWires) {
        Map<Wire, List<WireCrossing>> crossings = computeWireCrossings(renderWires);
        if (crossings.isEmpty()) {
            return;
        }
        java.awt.Stroke originalStroke = g2.getStroke();
        java.awt.Color originalColor = g2.getColor();
        g2.setStroke(new java.awt.BasicStroke(Wire.getStrokeWidth()));
        for (RenderWire renderWire : renderWires) {
            List<WireCrossing> wireCrossings = crossings.get(renderWire.wire);
            if (wireCrossings == null) {
                continue;
            }
            for (WireCrossing crossing : wireCrossings) {
                g2.setColor(Colors.CANVAS_BG);
                g2.fillOval(crossing.x - CROSSING_RADIUS, crossing.y - CROSSING_RADIUS,
                        CROSSING_RADIUS * 2, CROSSING_RADIUS * 2);
                java.awt.geom.AffineTransform originalTransform = g2.getTransform();
                g2.translate(crossing.x, crossing.y);
                g2.rotate(crossing.angle);
                g2.setColor(renderWire.wire.getColor());
                g2.drawArc(-CROSSING_RADIUS, -CROSSING_RADIUS,
                        CROSSING_RADIUS * 2, CROSSING_RADIUS * 2, 0, 180);
                g2.setTransform(originalTransform);
            }
        }
        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
    }

    private Map<Wire, List<WireCrossing>> computeWireCrossings(List<RenderWire> renderWires) {
        Map<Wire, List<WireCrossing>> crossings = new HashMap<>();
        Map<Wire, Set<PointKey>> seen = new HashMap<>();
        for (int i = 0; i < renderWires.size(); i++) {
            RenderWire first = renderWires.get(i);
            for (int j = i + 1; j < renderWires.size(); j++) {
                RenderWire second = renderWires.get(j);
                if (sharesNode(first.wire, second.wire)) {
                    continue;
                }
                if (areColinear(first, second)) {
                    continue;
                }
                java.awt.Point intersection = getIntersectionPoint(first, second);
                if (intersection == null) {
                    continue;
                }
                RenderWire overWire = chooseOverWire(first, second);
                PointKey key = new PointKey(intersection.x, intersection.y);
                Set<PointKey> seenPoints = seen.computeIfAbsent(overWire.wire, wire -> new HashSet<>());
                if (seenPoints.add(key)) {
                    crossings.computeIfAbsent(overWire.wire, wire -> new ArrayList<>())
                            .add(new WireCrossing(intersection.x, intersection.y, overWire.angle));
                }
            }
        }
        return crossings;
    }

    private RenderWire chooseOverWire(RenderWire first, RenderWire second) {
        boolean firstVertical = isMostlyVertical(first);
        boolean secondVertical = isMostlyVertical(second);
        if (firstVertical != secondVertical) {
            return firstVertical ? first : second;
        }
        return second;
    }

    private boolean isMostlyVertical(RenderWire wire) {
        int dx = Math.abs(wire.x2 - wire.x1);
        int dy = Math.abs(wire.y2 - wire.y1);
        return dy > dx;
    }

    private boolean sharesNode(Wire first, Wire second) {
        if (first == null || second == null) {
            return false;
        }
        WireNode aStart = first.getStart();
        WireNode aEnd = first.getEnd();
        WireNode bStart = second.getStart();
        WireNode bEnd = second.getEnd();
        return (aStart != null && (aStart == bStart || aStart == bEnd))
                || (aEnd != null && (aEnd == bStart || aEnd == bEnd));
    }

    private boolean areColinear(RenderWire first, RenderWire second) {
        long dx1 = first.x2 - first.x1;
        long dy1 = first.y2 - first.y1;
        long dx2 = second.x2 - second.x1;
        long dy2 = second.y2 - second.y1;
        return (dx1 * dy2) - (dy1 * dx2) == 0;
    }

    private java.awt.Point getIntersectionPoint(RenderWire first, RenderWire second) {
        double x1 = first.x1;
        double y1 = first.y1;
        double x2 = first.x2;
        double y2 = first.y2;
        double x3 = second.x1;
        double y3 = second.y1;
        double x4 = second.x2;
        double y4 = second.y2;
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 0.0001) {
            return null;
        }
        double det1 = (x1 * y2) - (y1 * x2);
        double det2 = (x3 * y4) - (y3 * x4);
        double px = (det1 * (x3 - x4) - (x1 - x2) * det2) / denom;
        double py = (det1 * (y3 - y4) - (y1 - y2) * det2) / denom;
        if (!isWithinSegment(px, py, x1, y1, x2, y2)
                || !isWithinSegment(px, py, x3, y3, x4, y4)) {
            return null;
        }
        return new java.awt.Point((int) Math.round(px), (int) Math.round(py));
    }

    private boolean isWithinSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2) - 0.1;
        double maxX = Math.max(x1, x2) + 0.1;
        double minY = Math.min(y1, y2) - 0.1;
        double maxY = Math.max(y1, y2) + 0.1;
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }

    private Map<Wire, Offset> computeWireOffsets() {
        Map<Wire, Offset> offsets = new HashMap<>();
        List<List<Wire>> groups = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            List<Wire> targetGroup = null;
            for (List<Wire> group : groups) {
                if (!group.isEmpty() && areColinearOverlap(wire, group.get(0))) {
                    targetGroup = group;
                    break;
                }
            }
            if (targetGroup == null) {
                targetGroup = new ArrayList<>();
                groups.add(targetGroup);
            }
            targetGroup.add(wire);
        }
        for (List<Wire> group : groups) {
            if (group.size() <= 1) {
                Wire wire = group.get(0);
                offsets.put(wire, new Offset(0, 0));
                continue;
            }
            Wire base = group.get(0);
            int dx = base.getEnd().getX() - base.getStart().getX();
            int dy = base.getEnd().getY() - base.getStart().getY();
            double length = Math.hypot(dx, dy);
            if (length == 0) {
                for (Wire wire : group) {
                    offsets.put(wire, new Offset(0, 0));
                }
                continue;
            }
            double normalX = -dy / length;
            double normalY = dx / length;
            int count = group.size();
            for (int i = 0; i < count; i++) {
                double index = i - (count - 1) / 2.0;
                int offsetX = (int) Math.round(normalX * index * WIRE_OFFSET_STEP);
                int offsetY = (int) Math.round(normalY * index * WIRE_OFFSET_STEP);
                offsets.put(group.get(i), new Offset(offsetX, offsetY));
            }
        }
        return offsets;
    }

    private Map<WireNode, Offset> computeNodeOffsets(Map<Wire, Offset> wireOffsets) {
        Map<WireNode, OffsetAccumulator> accumulators = new HashMap<>();
        for (Wire wire : wires) {
            Offset offset = wireOffsets.getOrDefault(wire, new Offset(0, 0));
            if (offset.isZero()) {
                continue;
            }
            addOffsetToNode(accumulators, wire.getStart(), offset);
            addOffsetToNode(accumulators, wire.getEnd(), offset);
        }
        Map<WireNode, Offset> nodeOffsets = new HashMap<>();
        for (Map.Entry<WireNode, OffsetAccumulator> entry : accumulators.entrySet()) {
            OffsetAccumulator accumulator = entry.getValue();
            if (accumulator.count == 0) {
                continue;
            }
            int dx = Math.round(accumulator.sumDx / (float) accumulator.count);
            int dy = Math.round(accumulator.sumDy / (float) accumulator.count);
            nodeOffsets.put(entry.getKey(), new Offset(dx, dy));
        }
        return nodeOffsets;
    }

    private Offset resolveEndpointOffset(Wire wire, WireNode node, boolean isStart, Offset wireOffset,
            Map<Wire, Offset> wireOffsets, Map<WireNode, Offset> nodeOffsets) {
        if (!wireOffset.isZero()) {
            return wireOffset;
        }
        Wire anchor = isStart ? wire.getStartAnchorWire() : wire.getEndAnchorWire();
        if (anchor != null) {
            Offset anchorOffset = wireOffsets.getOrDefault(anchor, new Offset(0, 0));
            if (!anchorOffset.isZero()) {
                return anchorOffset;
            }
        }
        return nodeOffsets.getOrDefault(node, wireOffset);
    }

    private void addOffsetToNode(Map<WireNode, OffsetAccumulator> accumulators, WireNode node, Offset offset) {
        if (node == null) {
            return;
        }
        OffsetAccumulator accumulator = accumulators.get(node);
        if (accumulator == null) {
            accumulator = new OffsetAccumulator();
            accumulators.put(node, accumulator);
        }
        accumulator.sumDx += offset.dx;
        accumulator.sumDy += offset.dy;
        accumulator.count++;
    }

    private boolean areColinearOverlap(Wire first, Wire second) {
        if (!areColinear(first, second)) {
            return false;
        }
        return segmentsOverlap(first, second);
    }

    private boolean areColinear(Wire first, Wire second) {
        int x1 = first.getStart().getX();
        int y1 = first.getStart().getY();
        int x2 = first.getEnd().getX();
        int y2 = first.getEnd().getY();
        int x3 = second.getStart().getX();
        int y3 = second.getStart().getY();
        int x4 = second.getEnd().getX();
        int y4 = second.getEnd().getY();
        long dx1 = x2 - x1;
        long dy1 = y2 - y1;
        long dx3 = x3 - x1;
        long dy3 = y3 - y1;
        long dx4 = x4 - x1;
        long dy4 = y4 - y1;
        return (dx1 * dy3) - (dy1 * dx3) == 0 && (dx1 * dy4) - (dy1 * dx4) == 0;
    }

    private boolean segmentsOverlap(Wire first, Wire second) {
        int x1 = first.getStart().getX();
        int y1 = first.getStart().getY();
        int x2 = first.getEnd().getX();
        int y2 = first.getEnd().getY();
        int x3 = second.getStart().getX();
        int y3 = second.getStart().getY();
        int x4 = second.getEnd().getX();
        int y4 = second.getEnd().getY();
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = (dx * dx) + (dy * dy);
        if (lengthSq == 0) {
            return false;
        }
        double t1 = ((x3 - x1) * dx + (y3 - y1) * dy) / lengthSq;
        double t2 = ((x4 - x1) * dx + (y4 - y1) * dy) / lengthSq;
        double minA = Math.min(0, 1);
        double maxA = Math.max(0, 1);
        double minB = Math.min(t1, t2);
        double maxB = Math.max(t1, t2);
        return maxB >= minA && minB <= maxA;
    }

    private static class Offset {
        private final int dx;
        private final int dy;

        private Offset(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private boolean isZero() {
            return dx == 0 && dy == 0;
        }
    }

    private static class OffsetAccumulator {
        private int sumDx;
        private int sumDy;
        private int count;
    }

    private static class PointKey {
        private final int x;
        private final int y;

        private PointKey(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PointKey)) {
                return false;
            }
            PointKey other = (PointKey) obj;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return (31 * x) + y;
        }
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
