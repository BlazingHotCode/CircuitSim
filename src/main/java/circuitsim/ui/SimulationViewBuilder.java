package circuitsim.ui;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.ports.CustomComponent;
import circuitsim.components.ports.CustomInputPort;
import circuitsim.components.ports.CustomOutputPort;
import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireColor;
import circuitsim.components.wiring.WireNode;
import circuitsim.custom.CustomComponentDefinition;
import circuitsim.io.BoardState;
import circuitsim.ui.Grid;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Expands custom components into their internal circuits for simulation.
 */
final class SimulationViewBuilder {
    private SimulationViewBuilder() {
    }

    static SimulationView build(List<CircuitComponent> components, List<Wire> wires,
                                Function<String, CustomComponentDefinition> customDefinitionResolver,
                                BiConsumer<CircuitComponent, BoardState.ComponentState> applyComponentState) {
        List<CircuitComponent> simulationComponents = new ArrayList<>();
        List<Wire> simulationWires = new ArrayList<>(wires);
        for (CircuitComponent component : components) {
            if (component instanceof CustomComponent custom) {
                CustomComponentDefinition definition = custom.getDefinition();
                if (definition == null) {
                    continue;
                }
                ExpansionResult expansion = expandDefinition(definition, custom.getId(), new HashSet<>(),
                        customDefinitionResolver, applyComponentState);
                simulationComponents.addAll(expansion.components);
                simulationWires.addAll(expansion.wires);
                connectExternalPorts(custom, expansion, simulationWires);
            } else {
                simulationComponents.add(component);
            }
        }
        return new SimulationView(simulationComponents, simulationWires);
    }

    private static ExpansionResult expandDefinition(CustomComponentDefinition definition, long seed,
                                                   java.util.Set<String> path,
                                                   Function<String, CustomComponentDefinition> customDefinitionResolver,
                                                   BiConsumer<CircuitComponent, BoardState.ComponentState> applyComponentState) {
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
        Point offset = computeSimulationOffset(seed);
        List<BoardState.ComponentState> componentStates = state.getComponents();
        for (int i = 0; i < componentStates.size(); i++) {
            BoardState.ComponentState componentState = componentStates.get(i);
            if ("Custom".equals(componentState.getType())) {
                CustomComponentDefinition nested = customDefinitionResolver.apply(componentState.getCustomId());
                if (nested == null) {
                    continue;
                }
                CustomComponent nestedShell = new CustomComponent(0, 0, nested);
                applyComponentState.accept(nestedShell, componentState);
                nestedShell.setPosition(componentState.getX() + offset.x, componentState.getY() + offset.y);
                long nestedSeed = (seed * 31L) + i + 1;
                ExpansionResult nestedExpansion = expandDefinition(nested, nestedSeed,
                        new HashSet<>(path), customDefinitionResolver, applyComponentState);
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
            applyComponentState.accept(component, componentState);
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

    private static void connectExternalPorts(CustomComponent shell, ExpansionResult expansion, List<Wire> simWires) {
        connectPortLists(getPortPoints(shell, true), expansion.inputPoints, simWires);
        connectPortLists(getPortPoints(shell, false), expansion.outputPoints, simWires);
    }

    private static void connectPortLists(List<Point> externalPoints, List<Point> internalPoints, List<Wire> simWires) {
        int count = Math.min(externalPoints.size(), internalPoints.size());
        for (int i = 0; i < count; i++) {
            Point external = externalPoints.get(i);
            Point internal = internalPoints.get(i);
            WireNode start = new WireNode(external.x, external.y);
            WireNode end = new WireNode(internal.x, internal.y);
            simWires.add(Wire.connect(start, end, WireColor.WHITE));
        }
    }

    private static List<Point> getPortPoints(CustomComponent shell, boolean inputs) {
        List<Point> points = new ArrayList<>();
        for (ConnectionPoint point : shell.getConnectionPoints()) {
            if (inputs && shell.isInputPoint(point)) {
                points.add(new Point(shell.getConnectionPointWorldX(point),
                        shell.getConnectionPointWorldY(point)));
            } else if (!inputs && shell.isOutputPoint(point)) {
                points.add(new Point(shell.getConnectionPointWorldX(point),
                        shell.getConnectionPointWorldY(point)));
            }
        }
        return points;
    }

    private static Point getPrimaryConnectionPoint(CircuitComponent component) {
        if (component.getConnectionPoints().isEmpty()) {
            return new Point(component.getX(), component.getY());
        }
        ConnectionPoint point = component.getConnectionPoints().get(0);
        return new Point(component.getConnectionPointWorldX(point),
                component.getConnectionPointWorldY(point));
    }

    private static Point computeSimulationOffset(long seed) {
        long abs = Math.abs(seed);
        int baseX = 1_000_000 + (int) ((abs % 997) * 10_000L);
        int baseY = 1_000_000 + (int) (((abs / 997) % 997) * 10_000L);
        return new Point(Grid.snap(baseX), Grid.snap(baseY));
    }

    private static CircuitComponent createComponentFromStateForSimulation(BoardState.ComponentState state) {
        if (state == null || state.getType() == null) {
            return null;
        }
        return circuitsim.components.core.ComponentRegistry.createBuiltinFromType(state.getType(),
                state.getX(), state.getY());
    }

    static final class SimulationView {
        final List<CircuitComponent> components;
        final List<Wire> wires;

        SimulationView(List<CircuitComponent> components, List<Wire> wires) {
            this.components = components;
            this.wires = wires;
        }
    }

    private static final class ExpansionResult {
        final List<CircuitComponent> components = new ArrayList<>();
        final List<Wire> wires = new ArrayList<>();
        final List<Point> inputPoints = new ArrayList<>();
        final List<Point> outputPoints = new ArrayList<>();
    }
}
