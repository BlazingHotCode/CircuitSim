package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.electrical.Switch;
import circuitsim.components.ports.CustomComponent;
import circuitsim.components.ports.CustomInputPort;
import circuitsim.components.wiring.Wire;
import circuitsim.custom.CustomComponentDefinition;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/**
 * Handles mouse interactions for {@link CircuitPanel}.
 */
final class CircuitMouseHandler extends MouseAdapter {
    private final CircuitPanel panel;

    CircuitMouseHandler(CircuitPanel panel) {
        this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        panel.requestFocusInWindow();
        panel.updateLastMouseWorld(e.getX(), e.getY());
        boolean toggleSelection = e.isControlDown();
        if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            panel.panningView = true;
            panel.panStartX = e.getX();
            panel.panStartY = e.getY();
            panel.panOriginOffsetX = panel.viewTransform.getOffsetX();
            panel.panOriginOffsetY = panel.viewTransform.getOffsetY();
            panel.panMoved = false;
            return;
        }
        int worldX = panel.viewTransform.toWorldX(e.getX());
        int worldY = panel.viewTransform.toWorldY(e.getY());
        if (panel.placementEntry != null && SwingUtilities.isLeftMouseButton(e)) {
            panel.placeActiveComponentAt(worldX, worldY);
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
                circuitsim.components.electrical.VariableResistor slider = panel.findVariableResistorHandleAt(worldX, worldY);
                if (slider != null) {
                    panel.draggingSliderResistor = slider;
                    panel.selection.selectComponent(slider);
                    slider.setWiperFromWorld(worldX, worldY);
                    panel.updateAttachedWireNodes(slider);
                    panel.repaint();
                    return;
                }
        }
        if (panel.creatingWire && panel.lockedWire) {
            int endX = Grid.snap(worldX);
            int endY = Grid.snap(worldY);
            WireEndpointHit endHit = panel.findWireEndpointAt(endX, endY);
            panel.finalizeWire(endX, endY, endHit == null ? null : endHit.wire);
            panel.creatingWire = false;
            panel.lockedWire = false;
            panel.newWireStartNode = null;
            panel.pendingWireStartAnchor = null;
            panel.repaint();
            return;
        }
        if (!panel.creatingWire && SwingUtilities.isLeftMouseButton(e)) {
            // Let normal component selection/drag flow handle inputs/sources.
        }
        ConnectionPoint hitPoint = panel.findConnectionPointAt(worldX, worldY);
        if (hitPoint != null) {
            if (toggleSelection) {
                panel.selection.toggleComponentSelection(hitPoint.getOwner());
                panel.repaint();
                return;
            }
            panel.selection.selectComponent(hitPoint.getOwner());
            int startX = panel.selection.selectedComponent.getConnectionPointWorldX(hitPoint);
            int startY = panel.selection.selectedComponent.getConnectionPointWorldY(hitPoint);
            panel.newWireStartNode = panel.getOrCreateNodeAt(startX, startY);
            panel.attachWireNodeToConnectionPoint(panel.newWireStartNode, hitPoint);
            panel.creatingWire = true;
            panel.lockedWire = e.isShiftDown();
            panel.pendingWireStartAnchor = null;
            panel.wireDragX = startX;
            panel.wireDragY = startY;
            panel.repaint();
            return;
        }
        WireEndpointHit endpointHit = panel.findWireEndpointAt(worldX, worldY);
        if (endpointHit != null) {
            if (toggleSelection) {
                panel.selection.toggleWireSelection(endpointHit.wire);
                panel.repaint();
                return;
            }
            panel.newWireStartNode = endpointHit.node;
            panel.creatingWire = true;
            panel.lockedWire = e.isShiftDown();
            panel.selection.selectWire(endpointHit.wire);
            panel.pendingWireStartAnchor = endpointHit.wire;
            panel.wireDragX = panel.newWireStartNode.getX();
            panel.wireDragY = panel.newWireStartNode.getY();
            panel.repaint();
            return;
        }
        WireHit wireHit = panel.findWireAt(worldX, worldY);
        if (wireHit != null) {
            if (toggleSelection) {
                panel.selection.toggleWireSelection(wireHit.wire);
                panel.repaint();
                return;
            }
            if (panel.selection.isMultiSelection() && panel.selection.selectedWires.contains(wireHit.wire)) {
                panel.beginSelectionDrag(worldX, worldY);
                return;
            }
            panel.draggingWire = wireHit.wire;
            panel.selection.selectWire(wireHit.wire);
            panel.wireDragStartX = Grid.snap(worldX);
            panel.wireDragStartY = Grid.snap(worldY);
            panel.wireStartAX = panel.draggingWire.getStart().getX();
            panel.wireStartAY = panel.draggingWire.getStart().getY();
            panel.wireStartBX = panel.draggingWire.getEnd().getX();
            panel.wireStartBY = panel.draggingWire.getEnd().getY();
            panel.detachSharedEndpoints(panel.draggingWire);
            panel.repaint();
            return;
        }
        for (int i = panel.components.size() - 1; i >= 0; i--) {
            CircuitComponent component = panel.components.get(i);
            if (component.contains(worldX, worldY)) {
                if (toggleSelection) {
                    panel.selection.toggleComponentSelection(component);
                    panel.repaint();
                    return;
                }
                if (panel.selection.isMultiSelection() && panel.selection.selectedComponents.contains(component)) {
                    panel.beginSelectionDrag(worldX, worldY);
                    return;
                }
                panel.selection.selectComponent(component);
                if (panel.isInRotateHandle(component, worldX, worldY)) {
                    panel.rotateSelectionGroup();
                    panel.repaint();
                    return;
                }
                panel.draggedComponent = component;
                panel.draggingWithoutWires = e.isShiftDown();
                panel.detachedWiresForShiftDrag = false;
                panel.attachWireNodesAtComponent(component);
                panel.updateAttachedWireNodes(component);
                panel.resizing = panel.isInResizeHandle(component, worldX, worldY);
                if (!panel.resizing) {
                    panel.dragOffsetX = worldX - component.getX();
                    panel.dragOffsetY = worldY - component.getY();
                }
                panel.components.remove(i);
                panel.components.add(component);
                panel.repaint();
                return;
            }
        }
        if (!panel.selection.isMultiSelection() && panel.selection.selectedComponent != null
                && panel.isInRotateHandle(panel.selection.selectedComponent, worldX, worldY)) {
            panel.rotateSelectionGroup();
            panel.repaint();
            return;
        }
        if (toggleSelection) {
            return;
        }
        panel.selection.clearSelection();
        panel.draggedComponent = null;
        panel.resizing = false;
        panel.selectingArea = true;
        panel.selectionStartX = worldX;
        panel.selectionStartY = worldY;
        panel.selectionEndX = worldX;
        panel.selectionEndY = worldY;
        panel.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int worldX = panel.viewTransform.toWorldX(e.getX());
        int worldY = panel.viewTransform.toWorldY(e.getY());
        if (e.getClickCount() == 2) {
            CircuitComponent component = panel.findComponentAtPoint(worldX, worldY);
            if (component instanceof circuitsim.components.electrical.LightBulb lightBulb
                    && lightBulb.isBurnedOut()) {
                lightBulb.setBurnedOut(false);
                panel.selection.selectComponent(lightBulb);
                panel.recordHistoryState();
                panel.repaint();
                return;
            }
            if (panel.removeWireAt(worldX, worldY)) {
                panel.recordHistoryState();
                panel.repaint();
            }
            if (component instanceof CustomComponent custom) {
                CustomComponentDefinition definition = custom.getDefinition();
                if (definition != null) {
                    panel.requestEditCustomComponent(definition.getId());
                }
            }
            return;
        }
        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
            if (panel.creatingWire || panel.draggingWire != null || panel.draggedComponent != null || panel.resizing) {
                return;
            }
            CircuitComponent component = panel.findComponentAtPoint(worldX, worldY);
            if (component instanceof Switch toggle) {
                toggle.toggle();
                panel.recordHistoryState();
                panel.repaint();
            } else if (component instanceof CustomInputPort inputPort
                    && panel.findConnectionPointAt(worldX, worldY) == null) {
                inputPort.setActive(!inputPort.isActive());
                panel.selection.selectComponent(inputPort);
                panel.recordHistoryState();
                panel.repaint();
            } else if (component instanceof circuitsim.components.electrical.Source source
                    && panel.findConnectionPointAt(worldX, worldY) == null) {
                source.setActive(!source.isActive());
                panel.selection.selectComponent(source);
                panel.recordHistoryState();
                panel.repaint();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.updateLastMouseWorld(e.getX(), e.getY());
        if (panel.draggingSliderResistor != null) {
            int worldX = panel.viewTransform.toWorldX(e.getX());
            int worldY = panel.viewTransform.toWorldY(e.getY());
            panel.draggingSliderResistor.setWiperFromWorld(worldX, worldY);
            panel.updateAttachedWireNodes(panel.draggingSliderResistor);
            panel.repaint();
            return;
        }
        if (!panel.panningView && SwingUtilities.isRightMouseButton(e)) {
            panel.panningView = true;
            panel.panStartX = e.getX();
            panel.panStartY = e.getY();
            panel.panOriginOffsetX = panel.viewTransform.getOffsetX();
            panel.panOriginOffsetY = panel.viewTransform.getOffsetY();
            panel.panMoved = false;
            panel.selectingArea = false;
            panel.draggingSelection = false;
            panel.draggedComponent = null;
            panel.draggingWire = null;
            panel.resizing = false;
            if (panel.creatingWire && !panel.lockedWire) {
                panel.creatingWire = false;
                panel.newWireStartNode = null;
                panel.pendingWireStartAnchor = null;
            }
        }
        if (panel.panningView) {
            int dx = e.getX() - panel.panStartX;
            int dy = e.getY() - panel.panStartY;
            panel.viewTransform.setOffset(panel.panOriginOffsetX + dx, panel.panOriginOffsetY + dy);
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                panel.panMoved = true;
            }
            panel.repaint();
            return;
        }
        int worldX = panel.viewTransform.toWorldX(e.getX());
        int worldY = panel.viewTransform.toWorldY(e.getY());
        if (panel.selectingArea) {
            panel.selectionEndX = worldX;
            panel.selectionEndY = worldY;
            panel.repaint();
            return;
        }
        if (panel.draggingSelection) {
            panel.dragSelectionTo(worldX, worldY);
            return;
        }
        if (panel.creatingWire) {
            panel.wireDragX = Grid.snap(worldX);
            panel.wireDragY = Grid.snap(worldY);
            panel.repaint();
            return;
        }
        if (panel.draggingWire != null) {
            int dx = Grid.snap(worldX) - panel.wireDragStartX;
            int dy = Grid.snap(worldY) - panel.wireDragStartY;
            panel.draggingWire.getStart().setPosition(panel.wireStartAX + dx, panel.wireStartAY + dy);
            panel.draggingWire.getEnd().setPosition(panel.wireStartBX + dx, panel.wireStartBY + dy);
            panel.repaint();
            return;
        }
        if (panel.draggedComponent != null) {
            if (panel.resizing) {
                int targetWidth = Math.max(CircuitPanel.MIN_COMPONENT_SIZE,
                        Grid.snap(worldX) - panel.draggedComponent.getX());
                int targetHeight = Math.max(CircuitPanel.MIN_COMPONENT_SIZE,
                        Grid.snap(worldY) - panel.draggedComponent.getY());
                panel.draggedComponent.resizeKeepingRatio(targetWidth, targetHeight);
                int snappedWidth = Grid.snapSize(panel.draggedComponent.getWidth());
                int snappedHeight = Grid.snapSize(panel.draggedComponent.getHeight());
                panel.draggedComponent.setSize(snappedWidth, snappedHeight);
            } else {
                int snappedX = Grid.snap(worldX - panel.dragOffsetX);
                int snappedY = Grid.snap(worldY - panel.dragOffsetY);
                if (panel.draggingWithoutWires && !panel.detachedWiresForShiftDrag) {
                    panel.detachWireAttachments(panel.draggedComponent);
                    panel.detachedWiresForShiftDrag = true;
                }
                panel.draggedComponent.setPosition(snappedX, snappedY);
            }
            if (!panel.draggingWithoutWires) {
                panel.updateAttachedWireNodes(panel.draggedComponent);
            }
            panel.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int worldX = panel.viewTransform.toWorldX(e.getX());
        int worldY = panel.viewTransform.toWorldY(e.getY());
        boolean didChange = false;
        if (panel.draggingSliderResistor != null) {
            panel.draggingSliderResistor.setWiperFromWorld(worldX, worldY);
            panel.updateAttachedWireNodes(panel.draggingSliderResistor);
            panel.draggingSliderResistor = null;
            panel.recordHistoryState();
            panel.repaint();
            return;
        }
        if (panel.panningView) {
            panel.panningView = false;
            if (!panel.panMoved && (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger())) {
                panel.showContextMenu(e, worldX, worldY);
            }
            return;
        }
        if (panel.lockedWire) {
            return;
        }
        if (panel.selectingArea) {
            panel.selectingArea = false;
            panel.selectionEndX = worldX;
            panel.selectionEndY = worldY;
            panel.updateSelectionFromArea();
            panel.repaint();
            return;
        }
        if (panel.draggingSelection) {
            panel.draggingSelection = false;
            panel.repaint();
            return;
        }
        if (panel.creatingWire) {
            int endX = Grid.snap(worldX);
            int endY = Grid.snap(worldY);
            WireEndpointHit endHit = panel.findWireEndpointAt(endX, endY);
            panel.finalizeWire(endX, endY, endHit == null ? null : endHit.wire);
            panel.creatingWire = false;
            panel.newWireStartNode = null;
            panel.pendingWireStartAnchor = null;
            panel.repaint();
            return;
        }
        if (panel.draggingWire != null) {
            panel.draggingWire = null;
            panel.repaint();
            return;
        }
        if (panel.draggedComponent != null) {
            didChange = true;
        }
        panel.draggedComponent = null;
        panel.resizing = false;
        panel.draggingWithoutWires = false;
        panel.detachedWiresForShiftDrag = false;
        if (didChange) {
            panel.recordHistoryState();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        panel.updateLastMouseWorld(e.getX(), e.getY());
        if (panel.placementEntry != null) {
            panel.updatePlacementAtScreen(e.getX(), e.getY());
        }
        if (panel.creatingWire) {
            int worldX = panel.viewTransform.toWorldX(e.getX());
            int worldY = panel.viewTransform.toWorldY(e.getY());
            panel.wireDragX = Grid.snap(worldX);
            panel.wireDragY = Grid.snap(worldY);
            panel.repaint();
        }
    }
}
