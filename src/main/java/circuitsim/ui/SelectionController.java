package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.instruments.Ammeter;
import circuitsim.components.instruments.Voltmeter;
import circuitsim.components.wiring.Wire;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages component/wire selection state and keeps the properties panel in sync.
 */
final class SelectionController {
    final List<CircuitComponent> selectedComponents = new ArrayList<>();
    final List<Wire> selectedWires = new ArrayList<>();

    CircuitComponent selectedComponent;
    Wire selectedWire;

    private final ComponentPropertiesPanel propertiesPanel;
    private final Runnable rotationResetCallback;

    SelectionController(ComponentPropertiesPanel propertiesPanel, Runnable rotationResetCallback) {
        this.propertiesPanel = propertiesPanel;
        this.rotationResetCallback = rotationResetCallback;
    }

    void selectComponent(CircuitComponent component) {
        selectedComponents.clear();
        selectedWires.clear();
        if (component != null) {
            selectedComponents.add(component);
        }
        selectedComponent = component;
        selectedWire = null;
        updatePropertiesPanel();
        rotationResetCallback.run();
    }

    void selectWire(Wire wire) {
        selectedComponents.clear();
        selectedWires.clear();
        if (wire != null) {
            selectedWires.add(wire);
        }
        selectedWire = wire;
        selectedComponent = null;
        updatePropertiesPanel();
        rotationResetCallback.run();
    }

    void setMultiSelection(List<CircuitComponent> components, List<Wire> wires) {
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
        rotationResetCallback.run();
    }

    void toggleComponentSelection(CircuitComponent component) {
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
        rotationResetCallback.run();
    }

    void toggleWireSelection(Wire wire) {
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
        rotationResetCallback.run();
    }

    void clearSelection() {
        selectedComponents.clear();
        selectedWires.clear();
        selectedComponent = null;
        selectedWire = null;
        updatePropertiesPanel();
        rotationResetCallback.run();
    }

    void refresh() {
        selectedComponent = (selectedComponents.size() == 1 && selectedWires.isEmpty())
                ? selectedComponents.get(0)
                : null;
        selectedWire = (selectedWires.size() == 1 && selectedComponents.isEmpty())
                ? selectedWires.get(0)
                : null;
        updatePropertiesPanel();
        rotationResetCallback.run();
    }

    boolean isMultiSelection() {
        return selectedComponents.size() + selectedWires.size() > 1;
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
}
