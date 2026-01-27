package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ComponentRegistry;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.electrical.Switch;
import circuitsim.components.instruments.Ammeter;
import circuitsim.components.instruments.Voltmeter;
import circuitsim.components.ports.CustomComponent;
import circuitsim.components.ports.CustomInputPort;
import circuitsim.components.ports.CustomOutputPort;
import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireColor;
import circuitsim.components.wiring.WireNode;
import circuitsim.custom.CustomComponentDefinition;
import circuitsim.io.BoardState;
import circuitsim.io.BoardStateIO;
import circuitsim.physics.CircuitPhysics;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Main canvas for circuit editing, rendering, and interaction.
 */
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
    private int lastMouseWorldX;
    private int lastMouseWorldY;
    private boolean hasLastMouseWorld;
    private JPopupMenu clearPopup;
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 0.1;
    private static final int MAX_HISTORY = 200;
    private Path lastBoardPath;
    private Path autosavePath;
    private final Deque<BoardState> undoStack = new ArrayDeque<>();
    private final Deque<BoardState> redoStack = new ArrayDeque<>();
    private boolean applyingState;
    private Runnable toggleComponentBarAction;
    private java.util.function.Supplier<List<circuitsim.custom.CustomComponentDefinition>> customDefinitionsSupplier =
            java.util.Collections::emptyList;
    private java.util.function.Function<String, circuitsim.custom.CustomComponentDefinition> customDefinitionResolver =
            id -> null;
    private final Map<String, circuitsim.custom.CustomComponentDefinition> embeddedCustomDefinitions = new HashMap<>();
    private java.util.function.Function<BoardState, BoardState> boardLoadTransform = state -> state;
    private boolean treatCustomOutputsAsGround;
    private Runnable changeListener = () -> {};
    private Runnable createCustomComponentAction = () -> {};
    private java.util.function.Consumer<String> editCustomComponentAction = id -> {};
    private java.util.function.Consumer<String> deleteCustomComponentAction = id -> {};
    private ComponentRegistry.Entry placementEntry;
    private CircuitComponent placementPreview;
    private Integer placementX;
    private Integer placementY;
    private boolean draggingWithoutWires;
    private boolean detachedWiresForShiftDrag;

    /**
     * @param propertiesPanel panel used to edit component properties
     */
    public CircuitPanel(ComponentPropertiesPanel propertiesPanel) {
        this(propertiesPanel, true);
    }

    /**
     * @param propertiesPanel panel used to edit component properties
     * @param enableAutosave whether to enable autosave behavior
     */
    public CircuitPanel(ComponentPropertiesPanel propertiesPanel, boolean enableAutosave) {
        this.propertiesPanel = propertiesPanel;
        setBackground(Colors.CANVAS_BG);
        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        add(shortCircuitPopup);
        MouseAdapter mouseHandler = new MouseAdapter() {
            /**
             * Handles selection, dragging, and wire creation on press.
             */
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                updateLastMouseWorld(e.getX(), e.getY());
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
                if (placementEntry != null && SwingUtilities.isLeftMouseButton(e)) {
                    placeActiveComponentAt(worldX, worldY);
                    return;
                }
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
                if (!creatingWire && SwingUtilities.isLeftMouseButton(e)) {
                    // Let normal component selection/drag flow handle inputs/sources.
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
                    attachWireNodeToConnectionPoint(newWireStartNode, hitPoint);
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
                        draggingWithoutWires = e.isShiftDown();
                        detachedWiresForShiftDrag = false;
                        attachWireNodesAtComponent(component);
                        updateAttachedWireNodes(component);
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

            /**
             * Handles double-click removal and switch toggles.
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                if (e.getClickCount() == 2) {
                    if (removeWireAt(worldX, worldY)) {
                        recordHistoryState();
                        repaint();
                    }
                    CircuitComponent component = findComponentAtPoint(worldX, worldY);
                    if (component instanceof CustomComponent custom) {
                        CustomComponentDefinition definition = custom.getDefinition();
                        if (definition != null) {
                            requestEditCustomComponent(definition.getId());
                        }
                    }
                    return;
                }
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    if (creatingWire || draggingWire != null || draggedComponent != null || resizing) {
                        return;
                    }
                    CircuitComponent component = findComponentAtPoint(worldX, worldY);
                    if (component instanceof Switch toggle) {
                        toggle.toggle();
                        recordHistoryState();
                        repaint();
                    } else if (component instanceof CustomInputPort inputPort
                            && findConnectionPointAt(worldX, worldY) == null) {
                        inputPort.setActive(!inputPort.isActive());
                        selectComponent(inputPort);
                        recordHistoryState();
                        repaint();
                    } else if (component instanceof circuitsim.components.electrical.Source source
                            && findConnectionPointAt(worldX, worldY) == null) {
                        source.setActive(!source.isActive());
                        selectComponent(source);
                        recordHistoryState();
                        repaint();
                    }
                }
            }

            /**
             * Handles dragging for selection, wires, components, and view panning.
             */
            @Override
            public void mouseDragged(MouseEvent e) {
                updateLastMouseWorld(e.getX(), e.getY());
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
                        if (draggingWithoutWires && !detachedWiresForShiftDrag) {
                            detachWireAttachments(draggedComponent);
                            detachedWiresForShiftDrag = true;
                        }
                        draggedComponent.setPosition(snappedX, snappedY);
                    }
                    if (!draggingWithoutWires) {
                        updateAttachedWireNodes(draggedComponent);
                    }
                    repaint();
                }
            }

            /**
             * Handles drag release, selection finalize, and context menus.
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());
                boolean didChange = false;
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
                if (draggedComponent != null) {
                    didChange = true;
                }
                draggedComponent = null;
                resizing = false;
                draggingWithoutWires = false;
                detachedWiresForShiftDrag = false;
                if (didChange) {
                    recordHistoryState();
                }
            }

            /**
             * Updates wire preview during mouse movement.
             */
            @Override
            public void mouseMoved(MouseEvent e) {
                updateLastMouseWorld(e.getX(), e.getY());
                if (placementEntry != null) {
                    updatePlacementAtScreen(e.getX(), e.getY());
                }
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
        configureSaveKeyBindings();
        configureLoadKeyBindings();
        configureUndoRedoKeyBindings();
        configureComponentBarKeyBindings();
        if (enableAutosave) {
            initializeAutosavePath();
            attemptLoadAutosave();
        } else {
            autosavePath = null;
        }
        if (undoStack.isEmpty()) {
            recordHistoryState();
        }
    }

    /**
     * Paints the grid, wires, components, and selection overlays.
     */
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
        drawPlacementPreview(g2);
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
        if (isShowing()) {
            repaint(33);
        }
    }

    /**
     * Draws the wire preview while creating a wire.
     */
    private void drawWirePreview(Graphics2D g2) {
        if (!creatingWire || newWireStartNode == null) {
            return;
        }
        Color originalColor = g2.getColor();
        g2.setColor(activeWireColor.getColor());
        g2.drawLine(newWireStartNode.getX(), newWireStartNode.getY(), wireDragX, wireDragY);
        g2.setColor(originalColor);
    }

    /**
     * Draws the placement ghost for the active component.
     */
    private void drawPlacementPreview(Graphics2D g2) {
        if (placementPreview == null || placementX == null || placementY == null) {
            return;
        }
        java.awt.Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        placementPreview.setPosition(placementX, placementY);
        placementPreview.draw(g2);
        g2.setComposite(originalComposite);
    }

    /**
     * Updates the circuit physics and renders all wires.
     */
    private void drawWires(Graphics2D g2) {
        SimulationView simulationView = buildSimulationView();
        // Call before simulation hooks
        for (circuitsim.components.core.CircuitComponent component : simulationView.components) {
            component.beforeSimulation();
        }
        
        boolean shortCircuit = CircuitPhysics.update(simulationView.components, simulationView.wires,
                treatCustomOutputsAsGround);

        // Call after simulation hooks
        for (circuitsim.components.core.CircuitComponent component : simulationView.components) {
            component.afterSimulation();
        }
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
            drawWireEndpointStub(g2, renderWire, true);
            drawWireEndpointStub(g2, renderWire, false);
        }
        drawWireCrossings(g2, lastRenderWires);
    }

    private void drawWireEndpointStub(Graphics2D g2, RenderWire renderWire, boolean start) {
        int baseX = start ? renderWire.baseX1 : renderWire.baseX2;
        int baseY = start ? renderWire.baseY1 : renderWire.baseY2;
        int x = start ? renderWire.x1 : renderWire.x2;
        int y = start ? renderWire.y1 : renderWire.y2;
        if (baseX == x && baseY == y) {
            return;
        }
        java.awt.Color originalColor = g2.getColor();
        java.awt.Stroke originalStroke = g2.getStroke();
        g2.setColor(renderWire.wire.getColor());
        g2.setStroke(new java.awt.BasicStroke(Wire.getStrokeWidth()));
        g2.drawLine(baseX, baseY, x, y);
        g2.setStroke(originalStroke);
        g2.setColor(originalColor);
    }

    private SimulationView buildSimulationView() {
        List<CircuitComponent> simulationComponents = new ArrayList<>();
        List<Wire> simulationWires = new ArrayList<>(wires);
        for (CircuitComponent component : components) {
            if (component instanceof CustomComponent custom) {
                CustomComponentDefinition definition = custom.getDefinition();
                if (definition == null) {
                    continue;
                }
                ExpansionResult expansion = expandDefinition(definition, custom.getId(), new java.util.HashSet<>());
                simulationComponents.addAll(expansion.components);
                simulationWires.addAll(expansion.wires);
                connectExternalPorts(custom, expansion, simulationWires);
            } else {
                simulationComponents.add(component);
            }
        }
        return new SimulationView(simulationComponents, simulationWires);
    }

    private ExpansionResult expandDefinition(CustomComponentDefinition definition, long seed,
                                             java.util.Set<String> path) {
        ExpansionResult result = new ExpansionResult();
        if (definition == null) {
            return result;
        }
        if (path.contains(definition.getId())) {
            return result;
        }
        path.add(definition.getId());
        BoardState state = definition.getBoardState();
        if (state == null) {
            return result;
        }
        java.awt.Point offset = computeSimulationOffset(seed);
        List<BoardState.ComponentState> componentStates = state.getComponents();
        for (int i = 0; i < componentStates.size(); i++) {
            BoardState.ComponentState componentState = componentStates.get(i);
            if ("Custom".equals(componentState.getType())) {
                CustomComponentDefinition nested = customDefinitionResolver.apply(componentState.getCustomId());
                if (nested == null) {
                    continue;
                }
                CustomComponent nestedShell = new CustomComponent(0, 0, nested);
                applyComponentState(nestedShell, componentState);
                nestedShell.setPosition(componentState.getX() + offset.x, componentState.getY() + offset.y);
                long nestedSeed = (seed * 31L) + i + 1;
                ExpansionResult nestedExpansion = expandDefinition(nested, nestedSeed,
                        new java.util.HashSet<>(path));
                result.components.addAll(nestedExpansion.components);
                result.wires.addAll(nestedExpansion.wires);
                connectPortLists(getPortPoints(nestedShell, true), nestedExpansion.inputPoints, result.wires);
                connectPortLists(getPortPoints(nestedShell, false), nestedExpansion.outputPoints, result.wires);
                continue;
            }
            CircuitComponent component = createComponentFromStateForSimulation(componentState);
            if (component == null) {
                continue;
            }
            applyComponentState(component, componentState);
            component.setPosition(componentState.getX() + offset.x, componentState.getY() + offset.y);
            result.components.add(component);
            if (component instanceof CustomInputPort) {
                result.inputPoints.add(getPrimaryConnectionPoint(component));
            } else if (component instanceof CustomOutputPort) {
                result.outputPoints.add(getPrimaryConnectionPoint(component));
            }
        }
        for (BoardState.WireState wireState : state.getWires()) {
            WireNode start = new WireNode(wireState.getStartX() + offset.x, wireState.getStartY() + offset.y);
            WireNode end = new WireNode(wireState.getEndX() + offset.x, wireState.getEndY() + offset.y);
            Wire wire = Wire.connect(start, end, wireState.getColor());
            wire.setShowData(wireState.isShowData());
            result.wires.add(wire);
        }
        return result;
    }

    private void connectExternalPorts(CustomComponent shell, ExpansionResult expansion, List<Wire> simWires) {
        connectPortLists(getPortPoints(shell, true), expansion.inputPoints, simWires);
        connectPortLists(getPortPoints(shell, false), expansion.outputPoints, simWires);
    }

    private void connectPortLists(List<java.awt.Point> externalPoints, List<java.awt.Point> internalPoints,
                                  List<Wire> simWires) {
        int count = Math.min(externalPoints.size(), internalPoints.size());
        for (int i = 0; i < count; i++) {
            java.awt.Point external = externalPoints.get(i);
            java.awt.Point internal = internalPoints.get(i);
            WireNode start = new WireNode(external.x, external.y);
            WireNode end = new WireNode(internal.x, internal.y);
            simWires.add(Wire.connect(start, end, WireColor.WHITE));
        }
    }

    private List<java.awt.Point> getPortPoints(CustomComponent shell, boolean inputs) {
        List<java.awt.Point> points = new ArrayList<>();
        for (ConnectionPoint point : shell.getConnectionPoints()) {
            if (inputs && shell.isInputPoint(point)) {
                points.add(new java.awt.Point(shell.getConnectionPointWorldX(point),
                        shell.getConnectionPointWorldY(point)));
            } else if (!inputs && shell.isOutputPoint(point)) {
                points.add(new java.awt.Point(shell.getConnectionPointWorldX(point),
                        shell.getConnectionPointWorldY(point)));
            }
        }
        return points;
    }

    private java.awt.Point getPrimaryConnectionPoint(CircuitComponent component) {
        if (component.getConnectionPoints().isEmpty()) {
            return new java.awt.Point(component.getX(), component.getY());
        }
        ConnectionPoint point = component.getConnectionPoints().get(0);
        return new java.awt.Point(component.getConnectionPointWorldX(point),
                component.getConnectionPointWorldY(point));
    }

    private java.awt.Point computeSimulationOffset(long seed) {
        long abs = Math.abs(seed);
        int baseX = 1_000_000 + (int) ((abs % 997) * 10_000L);
        int baseY = 1_000_000 + (int) (((abs / 997) % 997) * 10_000L);
        // Keep offsets grid-aligned so that existing snapped wire endpoints inside the
        // definition continue to land on the same snapped connection points after offsetting.
        return new java.awt.Point(circuitsim.ui.Grid.snap(baseX), circuitsim.ui.Grid.snap(baseY));
    }

    private CircuitComponent createComponentFromStateForSimulation(BoardState.ComponentState state) {
        if (state == null || state.getType() == null) {
            return null;
        }
        return circuitsim.components.core.ComponentRegistry.createBuiltinFromType(state.getType(),
                state.getX(), state.getY());
    }

    private static final class SimulationView {
        private final List<CircuitComponent> components;
        private final List<Wire> wires;

        private SimulationView(List<CircuitComponent> components, List<Wire> wires) {
            this.components = components;
            this.wires = wires;
        }
    }

    private static final class ExpansionResult {
        private final List<CircuitComponent> components = new ArrayList<>();
        private final List<Wire> wires = new ArrayList<>();
        private final List<java.awt.Point> inputPoints = new ArrayList<>();
        private final List<java.awt.Point> outputPoints = new ArrayList<>();
    }

    /**
     * @return true if the mouse is over the resize handle for the component
     */
    private boolean isInResizeHandle(CircuitComponent component, int mouseX, int mouseY) {
        java.awt.Rectangle bounds = component.getBounds();
        int handleX = bounds.x + bounds.width - RESIZE_HANDLE_SIZE;
        int handleY = bounds.y + bounds.height - RESIZE_HANDLE_SIZE;
        return mouseX >= handleX && mouseX <= handleX + RESIZE_HANDLE_SIZE
                && mouseY >= handleY && mouseY <= handleY + RESIZE_HANDLE_SIZE;
    }

    /**
     * @return true if the mouse is over the rotate handle for the component
     */
    private boolean isInRotateHandle(CircuitComponent component, int mouseX, int mouseY) {
        java.awt.Rectangle bounds = component.getBounds();
        int handleX = bounds.x + bounds.width - ROTATE_HANDLE_SIZE;
        int handleY = bounds.y - ROTATE_HANDLE_SIZE;
        return mouseX >= handleX && mouseX <= handleX + ROTATE_HANDLE_SIZE
                && mouseY >= handleY && mouseY <= handleY + ROTATE_HANDLE_SIZE;
    }

    /**
     * Draws a selection rectangle and optional handles for a component.
     */
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

    /**
     * Draws the rotation handle for a selected component.
     */
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

    /**
     * Highlights selected wires with a thicker overlay.
     */
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

    /**
     * Draws the selection marquee rectangle.
     */
    private void drawSelectionArea(Graphics2D g2) {
        java.awt.Color original = g2.getColor();
        g2.setColor(Colors.SELECTION);
        java.awt.Rectangle area = getSelectionRectangle();
        g2.drawRect(area.x, area.y, area.width, area.height);
        g2.setColor(original);
    }

    /**
     * Draws the background grid based on the current view transform.
     */
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

    /**
     * Shows a context menu based on what is under the cursor.
     */
    private void showContextMenu(MouseEvent e, int worldX, int worldY) {
        WireHit wireHit = findWireAt(worldX, worldY);
        if (wireHit != null) {
            selectWire(wireHit.wire);
            JPopupMenu menu = new JPopupMenu();
            JCheckBoxMenuItem showDataItem = new JCheckBoxMenuItem("Show Data", wireHit.wire.isShowData());
            showDataItem.addActionListener(event -> {
                wireHit.wire.setShowData(showDataItem.isSelected());
                recordHistoryState();
                repaint();
            });
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(event -> {
                removeWire(wireHit.wire);
                recordHistoryState();
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
                    addComponentAtWorld(entry, worldX, worldY);
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
            recordHistoryState();
            repaint();
        });
        menu.add(deleteItem);
        menu.show(this, e.getX(), e.getY());
    }

    /**
     * Adds a component entry at a world position and updates history.
     */
    private void addComponentAtWorld(ComponentRegistry.Entry entry, int worldX, int worldY) {
        if (entry == null) {
            return;
        }
        components.add(entry.create(Grid.snap(worldX), Grid.snap(worldY)));
        recordHistoryState();
        repaint();
    }

    /**
     * Begins placement mode with a preview component.
     */
    public void beginPlacementMode(ComponentRegistry.Entry entry) {
        if (entry == null) {
            return;
        }
        placementEntry = entry;
        placementPreview = entry.create(0, 0);
        int startX = hasLastMouseWorld ? lastMouseWorldX : toWorldX(getWidth() / 2);
        int startY = hasLastMouseWorld ? lastMouseWorldY : toWorldY(getHeight() / 2);
        placementX = Grid.snap(startX);
        placementY = Grid.snap(startY);
        repaint();
    }

    /**
     * Ends placement mode and clears the preview.
     */
    public void endPlacementMode() {
        placementEntry = null;
        placementPreview = null;
        placementX = null;
        placementY = null;
        repaint();
    }

    /**
     * Updates the placement preview based on a screen coordinate.
     */
    public void updatePlacementFromScreenPoint(Point screenPoint) {
        if (screenPoint == null || !isShowing()) {
            return;
        }
        Point local = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(local, this);
        updatePlacementAtScreen(local.x, local.y);
    }

    /**
     * Places a component using a screen coordinate.
     */
    public void placeComponentAtScreenPoint(ComponentRegistry.Entry entry, Point screenPoint) {
        if (entry == null || screenPoint == null || !isShowing()) {
            return;
        }
        Point local = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(local, this);
        addComponentAtWorld(entry, toWorldX(local.x), toWorldY(local.y));
    }

    /**
     * Adds a component near the most recent mouse position or the view center.
     */
    public void addComponentFromPalette(ComponentRegistry.Entry entry) {
        if (entry == null) {
            return;
        }
        int targetX = hasLastMouseWorld ? lastMouseWorldX : toWorldX(getWidth() / 2);
        int targetY = hasLastMouseWorld ? lastMouseWorldY : toWorldY(getHeight() / 2);
        addComponentAtWorld(entry, targetX, targetY);
    }

    /**
     * Stores the latest mouse position in world coordinates for palette placement.
     */
    private void updateLastMouseWorld(int screenX, int screenY) {
        lastMouseWorldX = toWorldX(screenX);
        lastMouseWorldY = toWorldY(screenY);
        hasLastMouseWorld = true;
    }

    private void updatePlacementAtScreen(int screenX, int screenY) {
        if (placementEntry == null) {
            return;
        }
        placementX = Grid.snap(toWorldX(screenX));
        placementY = Grid.snap(toWorldY(screenY));
        repaint();
    }

    private void placeActiveComponentAt(int worldX, int worldY) {
        if (placementEntry == null) {
            return;
        }
        addComponentAtWorld(placementEntry, worldX, worldY);
        endPlacementMode();
    }

    /**
     * Sets the toggle action for the top component bar.
     */
    public void setComponentBarToggle(Runnable toggleComponentBarAction) {
        this.toggleComponentBarAction = toggleComponentBarAction;
    }

    /**
     * Toggles whether custom outputs are treated as ground (editor testing only).
     */
    public void setTreatCustomOutputsAsGround(boolean treatCustomOutputsAsGround) {
        this.treatCustomOutputsAsGround = treatCustomOutputsAsGround;
    }

    /**
     * Sets a callback for when the board state changes.
     */
    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener == null ? () -> {} : changeListener;
    }

    /**
     * Sets the autosave path for this panel.
     */
    public void setAutosavePath(Path autosavePath, boolean load) {
        this.autosavePath = autosavePath;
        if (this.autosavePath != null) {
            try {
                Files.createDirectories(this.autosavePath.getParent());
            } catch (IOException ex) {
                this.autosavePath = null;
            }
        }
        if (load) {
            attemptLoadAutosave();
        }
    }

    /**
     * Forces the current board state (including custom component definitions) to be written to autosave.
     */
    public void flushAutosave() {
        writeAutosave(buildBoardState());
    }

    /**
     * Supplies custom component definitions for saves.
     */
    public void setCustomDefinitionsSupplier(
            java.util.function.Supplier<List<circuitsim.custom.CustomComponentDefinition>> supplier) {
        this.customDefinitionsSupplier = supplier == null
                ? java.util.Collections::emptyList
                : supplier;
    }

    /**
     * Resolves custom component definitions by id when loading.
     */
    public void setCustomDefinitionResolver(
            java.util.function.Function<String, circuitsim.custom.CustomComponentDefinition> resolver) {
        this.customDefinitionResolver = resolver == null ? id -> null : resolver;
    }

    /**
     * Allows callers to transform board state after loading.
     */
    public void setBoardLoadTransform(java.util.function.Function<BoardState, BoardState> transform) {
        this.boardLoadTransform = transform == null ? state -> state : transform;
    }

    /**
     * @return snapshot of the current board state
     */
    public BoardState exportBoardState() {
        return buildBoardState();
    }

    /**
     * Loads a board state into the panel.
     */
    public void importBoardState(BoardState state, boolean resetHistory) {
        applyBoardState(state);
        if (resetHistory) {
            resetHistoryState(state);
        } else {
            recordHistoryState();
        }
    }

    /**
     * @return immutable list of current components
     */
    public List<CircuitComponent> getComponentsSnapshot() {
        return java.util.Collections.unmodifiableList(new ArrayList<>(components));
    }

    /**
     * Sets callbacks for custom component actions in the palette.
     */
    public void setCustomComponentHandlers(Runnable onCreate,
                                           java.util.function.Consumer<String> onEdit,
                                           java.util.function.Consumer<String> onDelete) {
        this.createCustomComponentAction = onCreate == null ? () -> {} : onCreate;
        this.editCustomComponentAction = onEdit == null ? id -> {} : onEdit;
        this.deleteCustomComponentAction = onDelete == null ? id -> {} : onDelete;
    }

    /**
     * Requests creation of a new custom component.
     */
    public void requestCreateCustomComponent() {
        createCustomComponentAction.run();
    }

    /**
     * Requests editing of a custom component by id.
     */
    public void requestEditCustomComponent(String customId) {
        editCustomComponentAction.accept(customId);
    }

    /**
     * Requests deletion of a custom component by id.
     */
    public void requestDeleteCustomComponent(String customId) {
        deleteCustomComponentAction.accept(customId);
    }

    /**
     * Updates existing instances of a custom component by id.
     */
    public void updateCustomComponentInstances(String customId,
                                               circuitsim.custom.CustomComponentDefinition definition) {
        if (customId == null || definition == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            CircuitComponent component = components.get(i);
            if (!(component instanceof CustomComponent)) {
                continue;
            }
            CustomComponent custom = (CustomComponent) component;
            CustomComponentDefinition existing = custom.getDefinition();
            if (existing == null || !customId.equals(existing.getId())) {
                continue;
            }
            CustomComponent replacement = new CustomComponent(custom.getX(), custom.getY(), definition);
            replacement.setSize(replacement.getWidth(), replacement.getHeight());
            replacement.setRotationQuarterTurns(custom.getRotationQuarterTurns());
            components.set(i, replacement);
            if (selectedComponents.contains(custom)) {
                int index = selectedComponents.indexOf(custom);
                selectedComponents.set(index, replacement);
            }
            if (selectedComponent == custom) {
                selectedComponent = replacement;
            }
        }
        repaint();
    }

    /**
     * Removes all instances of a custom component by id.
     */
    public void removeCustomComponentInstances(String customId) {
        if (customId == null || customId.trim().isEmpty()) {
            return;
        }
        boolean removed = false;
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            if (!(component instanceof CustomComponent)) {
                continue;
            }
            CustomComponent custom = (CustomComponent) component;
            CustomComponentDefinition definition = custom.getDefinition();
            if (definition != null && customId.equals(definition.getId())) {
                components.remove(i);
                selectedComponents.remove(component);
                if (selectedComponent == component) {
                    selectedComponent = null;
                }
                removed = true;
            }
        }
        if (removed) {
            recordHistoryState();
            repaint();
        }
    }

    /**
     * Finds the topmost component at the provided point.
     */
    private CircuitComponent findComponentAtPoint(int mouseX, int mouseY) {
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            if (component.contains(mouseX, mouseY)) {
                return component;
            }
        }
        return null;
    }

    /**
     * Removes a wire at the given point, if any.
     */
    private boolean removeWireAt(int mouseX, int mouseY) {
        WireHit hit = findWireAt(mouseX, mouseY);
        if (hit == null) {
            return false;
        }
        return removeWire(hit.wire);
    }

    /**
     * Removes a specific wire from the circuit.
     */
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

    /**
     * Deletes the current selection of components and wires.
     */
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
        recordHistoryState();
        repaint();
    }

    /**
     * Binds delete/backspace to remove the selection.
     */
    private void configureDeleteKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelection");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteSelection");
        actionMap.put("deleteSelection", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelected();
            }
        });
    }

    /**
     * Binds the rotate hotkey.
     */
    private void configureRotateKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotateSelection");
        actionMap.put("rotateSelection", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!selectedComponents.isEmpty()) {
                    rotateSelectionGroup();
                    repaint();
                }
            }
        });
    }

    /**
     * Binds arrow keys to move the selection.
     */
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

    /**
     * Binds zoom shortcuts.
     */
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
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAt(getWidth() / 2, getHeight() / 2, ZOOM_STEP);
            }
        });
        actionMap.put("zoomOut", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAt(getWidth() / 2, getHeight() / 2, -ZOOM_STEP);
            }
        });
    }

    /**
     * Binds the reset view shortcut.
     */
    private void configureResetViewKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "resetView");
        actionMap.put("resetView", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                resetView();
            }
        });
    }

    /**
     * Binds the clear board shortcut.
     */
    private void configureClearKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "clearBoard");
        actionMap.put("clearBoard", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                requestClearBoard();
            }
        });
    }

    /**
     * Binds the save shortcut.
     */
    private void configureSaveKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "saveBoard");
        actionMap.put("saveBoard", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveBoardState();
            }
        });
    }

    /**
     * Binds the load shortcut.
     */
    private void configureLoadKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_O,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "loadBoard");
        actionMap.put("loadBoard", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                loadBoardState();
            }
        });
    }

    /**
     * Binds undo/redo shortcuts.
     */
    private void configureUndoRedoKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                java.awt.event.InputEvent.CTRL_DOWN_MASK), "undoAction");
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "redoAction");
        actionMap.put("undoAction", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                undoLastAction();
            }
        });
        actionMap.put("redoAction", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                redoLastAction();
            }
        });
    }

    /**
     * Binds Tab to toggle the component bar.
     */
    private void configureComponentBarKeyBindings() {
        javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = getActionMap();
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "toggleComponentBar");
        actionMap.put("toggleComponentBar", new javax.swing.AbstractAction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (toggleComponentBarAction != null) {
                    toggleComponentBarAction.run();
                }
            }
        });
    }

    /**
     * Resets zoom and pan to defaults.
     */
    private void resetView() {
        zoomFactor = 1.0;
        viewOffsetX = 0;
        viewOffsetY = 0;
        repaint();
    }

    /**
     * Action that moves the current selection by a grid delta.
     */
    private class MoveSelectionAction extends javax.swing.AbstractAction {
        private final int dx;
        private final int dy;

        /**
         * @param dx delta in X
         * @param dy delta in Y
         */
        private MoveSelectionAction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (!selectedComponents.isEmpty() || !selectedWires.isEmpty()) {
                moveSelectionBy(dx, dy);
                recordHistoryState();
                repaint();
            }
        }
    }

    /**
     * Selects a single component and clears other selections.
     */
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

    /**
     * Selects a single wire and clears other selections.
     */
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

    /**
     * Updates selection to include multiple components and wires.
     */
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

    /**
     * Toggles component selection in multi-select mode.
     */
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

    /**
     * Toggles wire selection in multi-select mode.
     */
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

    /**
     * Clears all selection state.
     */
    private void clearSelection() {
        selectedComponents.clear();
        selectedWires.clear();
        selectedComponent = null;
        selectedWire = null;
        updatePropertiesPanel();
        resetSelectionRotationState();
    }

    /**
     * Updates the properties panel with the current selection.
     */
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

    /**
     * @return true if more than one item is selected
     */
    private boolean isMultiSelection() {
        return selectedComponents.size() + selectedWires.size() > 1;
    }

    /**
     * Begins dragging a multi-selection.
     */
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

    /**
     * Drags the selection to a new snapped position.
     */
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

    /**
     * Moves selected components and wires by the provided delta.
     */
    private void moveSelectionBy(int dx, int dy) {
        for (CircuitComponent component : selectedComponents) {
            component.setPosition(component.getX() + dx, component.getY() + dy);
        }
        for (CircuitComponent component : selectedComponents) {
            updateAttachedWireNodes(component);
        }
        for (Wire wire : selectedWires) {
            wire.moveBy(dx, dy);
        }
    }

    /**
     * Rotates the entire selection around its bounding box center.
     */
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
        for (CircuitComponent component : selectedComponents) {
            updateAttachedWireNodes(component);
        }
        for (WireNode node : nodesToRotate) {
            rotateWireNodeAround(node, centerX, centerY);
        }
        recordHistoryState();
    }

    /**
     * Computes the bounding box of the current selection.
     */
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

    /**
     * Prepares wire nodes for rotation by detaching shared nodes.
     */
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

    /**
     * Resolves a node to rotate, cloning it if it is shared with unselected wires.
     */
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

    /**
     * Rotates a component around a center point and optionally rotates its orientation.
     */
    private void rotateComponentAround(CircuitComponent component, double centerX, double centerY,
            boolean rotateComponent) {
        double componentCenterX = component.getX() + (component.getWidth() / 2.0);
        double componentCenterY = component.getY() + (component.getHeight() / 2.0);
        double rotatedX = centerX - (componentCenterY - centerY);
        double rotatedY = centerY + (componentCenterX - centerX);
        if (rotateComponent) {
            component.rotate90();
            component.setSize(component.getWidth(), component.getHeight());
        }
        int newX = Grid.snap((int) Math.round(rotatedX - (component.getWidth() / 2.0)));
        int newY = Grid.snap((int) Math.round(rotatedY - (component.getHeight() / 2.0)));
        component.setPosition(newX, newY);
    }

    /**
     * Rotates a wire node around a center point.
     */
    private void rotateWireNodeAround(WireNode node, double centerX, double centerY) {
        double rotatedX = centerX - (node.getY() - centerY);
        double rotatedY = centerY + (node.getX() - centerX);
        node.setPosition(Grid.snap((int) Math.round(rotatedX)), Grid.snap((int) Math.round(rotatedY)));
    }

    /**
     * Creates and adds a new wire once the user completes placement.
     */
    private void finalizeWire(int endX, int endY, Wire endAnchorWire) {
        WireSplitResult splitResult = splitWireAt(endX, endY);
        WireNode endNode = splitResult == null ? getOrCreateNodeAt(endX, endY) : splitResult.node;
        Wire resolvedEndAnchor = splitResult == null ? endAnchorWire : splitResult.anchorWire;
        if (newWireStartNode != null) {
            if (isCustomInputConnectionAt(newWireStartNode.getX(), newWireStartNode.getY())
                    && newWireStartNode.getWireCount() > 0) {
                return;
            }
            if (endNode != null && isCustomInputConnectionAt(endNode.getX(), endNode.getY())
                    && endNode.getWireCount() > 0) {
                return;
            }
            
            // Check logic gate input restrictions
            if (isLogicInputConnectionAt(newWireStartNode.getX(), newWireStartNode.getY())
                    && newWireStartNode.getWireCount() > 0) {
                return;
            }
            if (endNode != null && isLogicInputConnectionAt(endNode.getX(), endNode.getY())
                    && endNode.getWireCount() > 0) {
                return;
            }
        }
            if (newWireStartNode != null && endNode != null
                    && (newWireStartNode.getX() != endNode.getX()
                    || newWireStartNode.getY() != endNode.getY())) {
                attachWireNodeIfOnConnectionPoint(endNode);
                Wire wire = Wire.connect(newWireStartNode, endNode, activeWireColor);
                if (pendingWireStartAnchor != null) {
                    wire.setStartAnchorWire(pendingWireStartAnchor);
                }
                if (resolvedEndAnchor != null) {
                    wire.setEndAnchorWire(resolvedEndAnchor);
                }
                wires.add(wire);
                rebuildWireAttachments();
                recordHistoryState();
            }
    }

    /**
     * Resets rotation tracking for the current selection.
     */
    private void resetSelectionRotationState() {
        selectionRotationTurns = 0;
        selectionBaseRotations.clear();
        for (CircuitComponent component : selectedComponents) {
            selectionBaseRotations.put(component, component.getRotationQuarterTurns() % 2);
        }
    }

    /**
     * Updates the active wire color for new wires.
     */
    public void setActiveWireColor(WireColor color) {
        WireColor next = color == null ? WireColor.WHITE : color;
        if (this.activeWireColor != next) {
            this.activeWireColor = next;
            recordHistoryState();
        }
    }

    /**
     * @return active wire color for new wires
     */
    public WireColor getActiveWireColor() {
        return activeWireColor;
    }

    /**
     * Shows a confirmation popup for clearing the board.
     */
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

    /**
     * Records a history state after a property change.
     */
    public void handlePropertyChange() {
        recordHistoryState();
    }

    /**
     * Saves the current board state to a JSON file.
     */
    private void saveBoardState() {
        JFileChooser chooser = new JFileChooser();
        if (lastBoardPath != null) {
            chooser.setSelectedFile(lastBoardPath.toFile());
        }
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        Path path = ensureJsonExtension(selected);
        BoardState state = buildBoardState();
        try {
            Files.writeString(path, BoardStateIO.toJson(state));
            lastBoardPath = path;
        } catch (IOException ex) {
            showError("Failed to save board state.", ex);
        }
    }

    /**
     * Loads a board state from a JSON file.
     */
    private void loadBoardState() {
        JFileChooser chooser = new JFileChooser();
        if (lastBoardPath != null) {
            chooser.setSelectedFile(lastBoardPath.toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            String json = Files.readString(path);
            BoardState state = BoardStateIO.fromJson(json);
            state = boardLoadTransform.apply(state);
            applyBoardState(state);
            lastBoardPath = path;
            resetHistoryState(state);
        } catch (IOException ex) {
            showError("Failed to load board state.", ex);
        } catch (RuntimeException ex) {
            showError("Invalid board state file.", ex);
        }
    }

    /**
     * Ensures the selected file path ends with .json.
     */
    private Path ensureJsonExtension(Path selected) {
        String name = selected.getFileName().toString();
        if (name.toLowerCase().endsWith(".json")) {
            return selected;
        }
        return selected.resolveSibling(name + ".json");
    }

    /**
     * Builds a serializable snapshot of the board state.
     */
    private BoardState buildBoardState() {
        List<BoardState.ComponentState> componentStates = new ArrayList<>();
        for (CircuitComponent component : components) {
            BoardState.ComponentState state = buildComponentState(component);
            if (state != null) {
                componentStates.add(state);
            }
        }
        List<BoardState.WireState> wireStates = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            wireStates.add(new BoardState.WireState(
                    wire.getStart().getX(),
                    wire.getStart().getY(),
                    wire.getEnd().getX(),
                    wire.getEnd().getY(),
                    wire.getWireColor(),
                    wire.isShowData()));
        }
        java.util.LinkedHashMap<String, circuitsim.custom.CustomComponentDefinition> customById =
                new java.util.LinkedHashMap<>();
        for (circuitsim.custom.CustomComponentDefinition definition : customDefinitionsSupplier.get()) {
            if (definition == null || definition.getId() == null) {
                continue;
            }
            customById.put(definition.getId(), definition);
        }
        for (CircuitComponent component : components) {
            if (!(component instanceof CustomComponent)) {
                continue;
            }
            circuitsim.custom.CustomComponentDefinition definition = ((CustomComponent) component).getDefinition();
            if (definition == null || definition.getId() == null) {
                continue;
            }
            customById.putIfAbsent(definition.getId(), definition);
        }
        List<circuitsim.custom.CustomComponentDefinition> customComponents =
                new java.util.ArrayList<>(customById.values());
        return new BoardState(BoardState.CURRENT_VERSION, activeWireColor, componentStates, wireStates,
                customComponents);
    }

    /**
     * Builds a component state for serialization.
     */
    private BoardState.ComponentState buildComponentState(CircuitComponent component) {
        if (component == null) {
            return null;
        }
        String type = component.getClass().getSimpleName();
        Float voltage = null;
        Float internalResistance = null;
        Float resistance = null;
        Float powerWatt = null;
        Boolean burnedOut = null;
        Boolean closed = null;
        switch (component) {
            case circuitsim.components.electrical.Battery battery -> {
                voltage = battery.getVoltage();
                internalResistance = battery.getInternalResistance();
            }
            case circuitsim.components.electrical.Resistor resistor -> resistance = resistor.getResistance();
            case circuitsim.components.electrical.PowerUser powerUser -> powerWatt = powerUser.getTargetPowerWatt();
            case circuitsim.components.electrical.LightBulb lightBulb -> {
                powerWatt = lightBulb.getRatedPowerWatt();
                burnedOut = lightBulb.isBurnedOut();
            }
            case circuitsim.components.electrical.Switch toggle -> closed = toggle.isClosed();
            case circuitsim.components.electrical.Source source -> closed = source.isActive();
            default -> {
            }
        }
        String customId = null;
        if (component instanceof circuitsim.components.ports.CustomComponent custom) {
            if (custom.getDefinition() != null) {
                customId = custom.getDefinition().getId();
            }
            type = "Custom";
        }
        return new BoardState.ComponentState(type, component.getX(), component.getY(),
                component.getWidth(), component.getHeight(), component.getRotationQuarterTurns(),
                component.getDisplayName(), customId, component.isShowTitle(), component.isShowingPropertyValues(),
                voltage, internalResistance, resistance, powerWatt, burnedOut, closed);
    }

    /**
     * Applies a board state to the current scene.
     */
    private void applyBoardState(BoardState state) {
        applyingState = true;
        clearBoard();
        if (state == null) {
            applyingState = false;
            return;
        }
        embeddedCustomDefinitions.clear();
        for (circuitsim.custom.CustomComponentDefinition definition : state.getCustomComponents()) {
            if (definition == null || definition.getId() == null) {
                continue;
            }
            embeddedCustomDefinitions.put(definition.getId(), definition);
        }
        setActiveWireColor(state.getActiveWireColor());
        for (BoardState.ComponentState componentState : state.getComponents()) {
            CircuitComponent component = createComponentFromState(componentState);
            if (component != null) {
                components.add(component);
            }
        }
        Map<Point, WireNode> nodeCache = new HashMap<>();
        for (BoardState.WireState wireState : state.getWires()) {
            WireNode start = getOrCreateWireNode(nodeCache, wireState.getStartX(), wireState.getStartY());
            WireNode end = getOrCreateWireNode(nodeCache, wireState.getEndX(), wireState.getEndY());
            Wire wire = Wire.connect(start, end, wireState.getColor());
            wire.setShowData(wireState.isShowData());
            wires.add(wire);
        }
        rebuildWireAttachments();
        clearSelection();
        applyingState = false;
        repaint();
    }

    /**
     * Creates a component instance based on a serialized state.
     */
    private CircuitComponent createComponentFromState(BoardState.ComponentState state) {
        if (state == null || state.getType() == null) {
            return null;
        }
        CircuitComponent component;
        if ("Custom".equals(state.getType())) {
            circuitsim.custom.CustomComponentDefinition definition =
                    customDefinitionResolver.apply(state.getCustomId());
            if (definition == null) {
                definition = embeddedCustomDefinitions.get(state.getCustomId());
            }
            if (definition == null) {
                return null;
            }
            component = new circuitsim.components.ports.CustomComponent(state.getX(), state.getY(), definition);
        } else {
            component = circuitsim.components.core.ComponentRegistry.createBuiltinFromType(state.getType(),
                    state.getX(), state.getY());
            if (component == null) {
                return null;
            }
        }
        applyComponentState(component, state);
        return component;
    }

    /**
     * Applies serialized values to a component instance.
     */
    private void applyComponentState(CircuitComponent component, BoardState.ComponentState state) {
        component.setPosition(state.getX(), state.getY());
        if (state.getWidth() > 0 && state.getHeight() > 0) {
            component.setSize(state.getWidth(), state.getHeight());
        }
        component.setRotationQuarterTurns(state.getRotationQuarterTurns());
        component.setDisplayName(state.getDisplayName());
        component.setShowTitle(state.isShowTitle());
        component.setShowPropertyValues(state.isShowValues());
        switch (component) {
            case circuitsim.components.electrical.Battery battery -> {
                if (state.getVoltage() != null) {
                    battery.setVoltage(state.getVoltage());
                }
                if (state.getInternalResistance() != null) {
                    battery.setInternalResistance(state.getInternalResistance());
                }
            }
            case circuitsim.components.electrical.Resistor resistor -> {
                if (state.getResistance() != null) {
                    resistor.setResistance(state.getResistance());
                }
            }
            case circuitsim.components.electrical.PowerUser powerUser -> {
                if (state.getPowerWatt() != null) {
                    powerUser.setTargetPowerWatt(state.getPowerWatt());
                }
            }
            case circuitsim.components.electrical.LightBulb lightBulb -> {
                if (state.getPowerWatt() != null) {
                    lightBulb.setRatedPowerWatt(state.getPowerWatt());
                }
                if (state.getBurnedOut() != null) {
                    lightBulb.setBurnedOut(state.getBurnedOut());
                }
            }
            case circuitsim.components.electrical.Switch toggle -> {
                if (state.getClosed() != null) {
                    toggle.setClosed(state.getClosed());
                }
            }
            case circuitsim.components.electrical.Source source -> {
                if (state.getClosed() != null) {
                    source.setActive(state.getClosed());
                }
            }
            default -> {
            }
        }
    }

    /**
     * Reuses wire nodes to preserve shared endpoints while loading.
     */
    private WireNode getOrCreateWireNode(Map<Point, WireNode> cache, int x, int y) {
        Point key = new Point(x, y);
        WireNode node = cache.get(key);
        if (node == null) {
            node = new WireNode(x, y);
            cache.put(key, node);
        }
        return node;
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String message, Exception ex) {
        String detail = ex.getMessage();
        String fullMessage = detail == null ? message : message + " " + detail;
        JOptionPane.showMessageDialog(this, fullMessage, "CircuitSim", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Initializes the autosave file location based on the OS.
     */
    private void initializeAutosavePath() {
        autosavePath = circuitsim.io.DataPaths.getAutosavePath();
        try {
            Files.createDirectories(autosavePath.getParent());
        } catch (IOException ex) {
            autosavePath = null;
        }
    }

    /**
     * Loads an autosaved board state if present.
     */
    private void attemptLoadAutosave() {
        if (autosavePath == null || !Files.exists(autosavePath)) {
            return;
        }
        try {
            String json = Files.readString(autosavePath);
            BoardState state = BoardStateIO.fromJson(json);
            applyBoardState(state);
            resetHistoryState(state);
        } catch (IOException | RuntimeException ignored) {
            // Autosave is best-effort; ignore failures.
        }
    }

    /**
     * Writes the autosave file to disk.
     */
    private void writeAutosave(BoardState state) {
        if (autosavePath == null) {
            return;
        }
        try {
            Files.writeString(autosavePath, BoardStateIO.toJson(state));
        } catch (IOException ignored) {
            // Autosave is best-effort; ignore failures.
        }
    }

    /**
     * Captures the current state for undo history and autosave.
     */
    private void recordHistoryState() {
        if (applyingState) {
            return;
        }
        BoardState state = buildBoardState();
        undoStack.push(state);
        redoStack.clear();
        trimHistory(undoStack);
        writeAutosave(state);
        changeListener.run();
    }

    /**
     * Resets history stacks to the provided state.
     */
    private void resetHistoryState(BoardState state) {
        undoStack.clear();
        redoStack.clear();
        if (state != null) {
            undoStack.push(state);
            trimHistory(undoStack);
        }
        writeAutosave(state);
    }

    /**
     * Undoes the last action and stores the current state for redo.
     */
    private void undoLastAction() {
        if (undoStack.size() < 2) {
            return;
        }
        BoardState current = undoStack.pop();
        redoStack.push(current);
        BoardState previous = undoStack.peek();
        applyBoardState(previous);
        writeAutosave(previous);
    }

    /**
     * Redoes the last undone action.
     */
    private void redoLastAction() {
        if (redoStack.isEmpty()) {
            return;
        }
        BoardState next = redoStack.pop();
        undoStack.push(next);
        trimHistory(undoStack);
        applyBoardState(next);
        writeAutosave(next);
    }

    /**
     * Ensures the history stack does not exceed the configured limit.
     */
    private void trimHistory(Deque<BoardState> stack) {
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    /**
     * Clears all components and wires from the board.
     */
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
        recordHistoryState();
        repaint();
    }

    /**
     * Zooms in or out around the provided screen coordinate.
     */
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

    /**
     * Converts a screen X coordinate to world space.
     */
    private int toWorldX(int screenX) {
        return (int) Math.round((screenX - viewOffsetX) / zoomFactor);
    }

    /**
     * Converts a screen Y coordinate to world space.
     */
    private int toWorldY(int screenY) {
        return (int) Math.round((screenY - viewOffsetY) / zoomFactor);
    }

    /**
     * Finds a wire near the provided point.
     */
    private WireHit findWireAt(int mouseX, int mouseY) {
        double maxDistance = 6.0;
        if (wires.isEmpty()) {
            return null;
        }
        List<RenderWire> renderWires = lastRenderWires.isEmpty() ? buildRenderWires() : lastRenderWires;
        for (RenderWire renderWire : renderWires) {
            if (distanceToSegment(mouseX, mouseY, renderWire.x1, renderWire.y1, renderWire.x2, renderWire.y2)
                    <= maxDistance) {
                return new WireHit(renderWire.wire);
            }
        }
        return null;
    }

    /**
     * Updates the selection based on the current drag area.
     */
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

    /**
     * @return selection marquee rectangle in world coordinates
     */
    private java.awt.Rectangle getSelectionRectangle() {
        int x = Math.min(selectionStartX, selectionEndX);
        int y = Math.min(selectionStartY, selectionEndY);
        int width = Math.abs(selectionEndX - selectionStartX);
        int height = Math.abs(selectionEndY - selectionStartY);
        return new java.awt.Rectangle(x, y, width, height);
    }

    /**
     * @return true when a wire intersects the selection rectangle
     */
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

    /**
     * Finds a wire endpoint at the provided point.
     */
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

    /**
     * Returns an existing node at the position or creates a new one.
     */
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

    /**
     * Splits a wire that passes through the provided point.
     */
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
        Wire first = Wire.connect(start, splitNode, color);
        Wire second = Wire.connect(splitNode, end, color);
        first.setShowData(showData);
        second.setShowData(showData);
        wires.add(first);
        wires.add(second);
        return new WireSplitResult(splitNode, first);
    }

    /**
     * Finds a wire eligible for splitting at the provided point.
     */
    private Wire findWireForSplit(int x, int y) {
        double maxDistance = 6.0;
        int endpointRadiusSq = WIRE_ENDPOINT_RADIUS * WIRE_ENDPOINT_RADIUS;
        for (Wire wire : wires) {
            if (wire.getWireColor() != activeWireColor) {
                continue;
            }
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

    /**
     * Ensures wire endpoints are unique when dragging a wire.
     */
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

    /**
     * Hit result for a wire segment.
     */
    private static class WireHit {
        private final Wire wire;

        /**
         * @param wire hit wire
         */
        private WireHit(Wire wire) {
            this.wire = wire;
        }
    }

    /**
     * Hit result for a wire endpoint.
     */
    private static class WireEndpointHit {
        private final Wire wire;
        private final WireNode node;

        /**
         * @param wire hit wire
         * @param node hit endpoint node
         */
        private WireEndpointHit(Wire wire, WireNode node) {
            this.wire = wire;
            this.node = node;
        }
    }

    /**
     * Result of splitting a wire, including the new node and anchor.
     */
    private static class WireSplitResult {
        private final WireNode node;
        private final Wire anchorWire;

        /**
         * @param node split node
         * @param anchorWire original anchor wire
         */
        private WireSplitResult(WireNode node, Wire anchorWire) {
            this.node = node;
            this.anchorWire = anchorWire;
        }
    }

    /**
     * Wire render metadata with precomputed angle.
     */
    private static class RenderWire {
        private final Wire wire;
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;
        private final int baseX1;
        private final int baseY1;
        private final int baseX2;
        private final int baseY2;
        private final double angle;

        /**
         * @param wire wire to draw
         * @param x1 start X
         * @param y1 start Y
         * @param x2 end X
         * @param y2 end Y
         */
        private RenderWire(Wire wire, int x1, int y1, int x2, int y2,
                           int baseX1, int baseY1, int baseX2, int baseY2) {
            this.wire = wire;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.baseX1 = baseX1;
            this.baseY1 = baseY1;
            this.baseX2 = baseX2;
            this.baseY2 = baseY2;
            this.angle = Math.atan2(y2 - y1, x2 - x1);
        }
    }

    /**
     * Intersection marker for wire crossings.
     */
    private static class WireCrossing {
        private final int x;
        private final int y;
        private final double angle;

        /**
         * @param x crossing X
         * @param y crossing Y
         * @param angle wire angle
         */
        private WireCrossing(int x, int y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
        }
    }

    /**
     * Computes distance from a point to a line segment.
     */
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

    /**
     * Builds render wires with offset calculations applied.
     */
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
            int baseStartX = wire.getStart().getX();
            int baseStartY = wire.getStart().getY();
            int baseEndX = wire.getEnd().getX();
            int baseEndY = wire.getEnd().getY();
            int startX = baseStartX + startOffset.dx;
            int startY = baseStartY + startOffset.dy;
            int endX = baseEndX + endOffset.dx;
            int endY = baseEndY + endOffset.dy;
            renderWires.add(new RenderWire(wire, startX, startY, endX, endY,
                    baseStartX, baseStartY, baseEndX, baseEndY));
        }
        return renderWires;
    }

    /**
     * Draws overpass arcs where wires cross.
     */
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

    /**
     * Computes crossings between wire segments.
     */
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

    /**
     * Chooses which wire should draw over the other at a crossing.
     */
    private RenderWire chooseOverWire(RenderWire first, RenderWire second) {
        boolean firstVertical = isMostlyVertical(first);
        boolean secondVertical = isMostlyVertical(second);
        if (firstVertical != secondVertical) {
            return firstVertical ? first : second;
        }
        return second;
    }

    /**
     * @return true when the wire is mostly vertical
     */
    private boolean isMostlyVertical(RenderWire wire) {
        int dx = Math.abs(wire.x2 - wire.x1);
        int dy = Math.abs(wire.y2 - wire.y1);
        return dy > dx;
    }

    /**
     * @return true if two wires share a node
     */
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

    /**
     * @return true if render wires are colinear
     */
    private boolean areColinear(RenderWire first, RenderWire second) {
        long dx1 = first.x2 - first.x1;
        long dy1 = first.y2 - first.y1;
        long dx2 = second.x2 - second.x1;
        long dy2 = second.y2 - second.y1;
        return (dx1 * dy2) - (dy1 * dx2) == 0;
    }

    /**
     * Computes the intersection point of two line segments, if any.
     */
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

    /**
     * @return true if the point lies within the segment bounds
     */
    private boolean isWithinSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2) - 0.1;
        double maxX = Math.max(x1, x2) + 0.1;
        double minY = Math.min(y1, y2) - 0.1;
        double maxY = Math.max(y1, y2) + 0.1;
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }

    /**
     * Computes parallel offsets for colinear wires.
     */
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

    /**
     * Computes average offsets for nodes based on attached wires.
     */
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

    /**
     * Resolves the offset for a specific wire endpoint.
     */
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

    /**
     * Adds an offset into the accumulator for a node.
     */
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

    /**
     * @return true if two wires are colinear and overlapping
     */
    private boolean areColinearOverlap(Wire first, Wire second) {
        if (!areColinear(first, second)) {
            return false;
        }
        if (sharesNode(first, second)) {
            return false;
        }
        return segmentsOverlap(first, second);
    }

    /**
     * @return true if two wires are colinear
     */
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

    /**
     * @return true when two colinear segments overlap
     */
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
        return maxB > minA && minB < maxA;
    }

    /**
     * Offset for drawing parallel wires.
     */
    private static class Offset {
        private final int dx;
        private final int dy;

        /**
         * @param dx offset in X
         * @param dy offset in Y
         */
        private Offset(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        /**
         * @return true if both offsets are zero
         */
        private boolean isZero() {
            return dx == 0 && dy == 0;
        }
    }

    /**
     * Accumulates offsets for a node.
     */
    private static class OffsetAccumulator {
        private int sumDx;
        private int sumDy;
        private int count;
    }

    /**
     * Key used to avoid duplicate crossings.
     */
    private static class PointKey {
        private final int x;
        private final int y;

        /**
         * @param x point X
         * @param y point Y
         */
        private PointKey(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return (31 * x) + y;
        }
    }

    /**
     * Finds a connection point near the provided coordinates.
     */
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

    private boolean isCustomInputConnectionAt(int x, int y) {
        ConnectionPoint point = findConnectionPointAt(x, y);
        if (point == null) {
            return false;
        }
        CircuitComponent owner = point.getOwner();
        if (owner instanceof circuitsim.components.ports.CustomComponent custom) {
            return custom.isInputPoint(point);
        }
        return false;
    }

    private void rebuildWireAttachments() {
        java.util.Map<java.awt.Point, java.util.List<WireNode>> nodesByPoint = new java.util.HashMap<>();
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            if (start != null) {
                start.detachFromComponent();
                nodesByPoint.computeIfAbsent(new java.awt.Point(start.getX(), start.getY()),
                        key -> new java.util.ArrayList<>()).add(start);
            }
            WireNode end = wire.getEnd();
            if (end != null) {
                end.detachFromComponent();
                nodesByPoint.computeIfAbsent(new java.awt.Point(end.getX(), end.getY()),
                        key -> new java.util.ArrayList<>()).add(end);
            }
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            java.util.List<ConnectionPoint> points = component.getConnectionPoints();
            for (int index = 0; index < points.size(); index++) {
                ConnectionPoint point = points.get(index);
                int x = component.getConnectionPointWorldX(point);
                int y = component.getConnectionPointWorldY(point);
                java.util.List<WireNode> nodes = nodesByPoint.get(new java.awt.Point(x, y));
                if (nodes == null) {
                    continue;
                }
                for (WireNode node : nodes) {
                    if (node != null && !node.isAttachedToComponent()) {
                        node.attachToComponent(component.getId(), index);
                    }
                }
            }
        }
    }

    private void attachWireNodesAtComponent(CircuitComponent component) {
        if (component == null) {
            return;
        }
        java.util.List<ConnectionPoint> points = component.getConnectionPoints();
        for (int index = 0; index < points.size(); index++) {
            ConnectionPoint point = points.get(index);
            int x = component.getConnectionPointWorldX(point);
            int y = component.getConnectionPointWorldY(point);
            for (Wire wire : wires) {
                WireNode start = wire.getStart();
                WireNode end = wire.getEnd();
                if (start != null && start.getX() == x && start.getY() == y) {
                    start.attachToComponent(component.getId(), index);
                }
                if (end != null && end.getX() == x && end.getY() == y) {
                    end.attachToComponent(component.getId(), index);
                }
            }
        }
    }

    private void attachWireNodeToConnectionPoint(WireNode node, ConnectionPoint point) {
        if (node == null || point == null) {
            return;
        }
        CircuitComponent owner = point.getOwner();
        if (owner == null) {
            return;
        }
        int index = owner.getConnectionPointIndex(point);
        if (index < 0) {
            return;
        }
        node.attachToComponent(owner.getId(), index);
        node.setPosition(owner.getConnectionPointWorldX(point), owner.getConnectionPointWorldY(point));
    }

    private void attachWireNodeIfOnConnectionPoint(WireNode node) {
        if (node == null) {
            return;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            java.util.List<ConnectionPoint> points = component.getConnectionPoints();
            for (int index = 0; index < points.size(); index++) {
                ConnectionPoint point = points.get(index);
                int x = component.getConnectionPointWorldX(point);
                int y = component.getConnectionPointWorldY(point);
                if (node.getX() == x && node.getY() == y) {
                    node.attachToComponent(component.getId(), index);
                    return;
                }
            }
        }
    }

    private void updateAttachedWireNodes(CircuitComponent component) {
        if (component == null) {
            return;
        }
        long id = component.getId();
        java.util.List<ConnectionPoint> points = component.getConnectionPoints();
        for (Wire wire : wires) {
            updateAttachedWireNodeEndpoint(component, id, points, wire.getStart());
            updateAttachedWireNodeEndpoint(component, id, points, wire.getEnd());
        }
    }

    private void updateAttachedWireNodeEndpoint(CircuitComponent component, long id,
                                                java.util.List<ConnectionPoint> points, WireNode node) {
        if (node == null || !node.isAttachedToComponent()) {
            return;
        }
        Long attachedId = node.getAttachedComponentId();
        Integer attachedIndex = node.getAttachedConnectionIndex();
        if (attachedId == null || attachedIndex == null || attachedId.longValue() != id) {
            return;
        }
        int index = attachedIndex.intValue();
        if (index < 0 || index >= points.size()) {
            node.detachFromComponent();
            return;
        }
        ConnectionPoint point = points.get(index);
        int x = component.getConnectionPointWorldX(point);
        int y = component.getConnectionPointWorldY(point);
        node.setPosition(x, y);
    }

    private void detachWireAttachments(CircuitComponent component) {
        if (component == null) {
            return;
        }
        long id = component.getId();
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            if (start != null && start.isAttachedToComponent()
                    && start.getAttachedComponentId() != null
                    && start.getAttachedComponentId().longValue() == id) {
                start.detachFromComponent();
            }
            WireNode end = wire.getEnd();
            if (end != null && end.isAttachedToComponent()
                    && end.getAttachedComponentId() != null
                    && end.getAttachedComponentId().longValue() == id) {
                end.detachFromComponent();
            }
        }
    }

    /**
     * Checks if coordinates match any logic gate input connection point.
     * This prevents multiple wires from connecting to logic gate inputs.
     */
    private boolean isLogicInputConnectionAt(int x, int y) {
        for (circuitsim.components.core.CircuitComponent component : components) {
            if (component.isLogicComponent()) {
                List<circuitsim.components.core.ConnectionPoint> points = component.getConnectionPoints();
                for (int i = 0; i < points.size(); i++) {
                    if (component.isInputConnection(i)) {
                        int pointX = component.getConnectionPointWorldX(points.get(i));
                        int pointY = component.getConnectionPointWorldY(points.get(i));
                        if (pointX == x && pointY == y) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Lays out the short-circuit popup in the corner.
     */
    @Override
    public void doLayout() {
        super.doLayout();
        int padding = 12;
        int popupWidth = shortCircuitPopup.getPreferredSize().width;
        int popupHeight = shortCircuitPopup.getPreferredSize().height;
        shortCircuitPopup.setBounds(padding, getHeight() - popupHeight - padding, popupWidth, popupHeight);
    }
}
