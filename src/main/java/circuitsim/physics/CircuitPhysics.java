package circuitsim.physics;

import circuitsim.components.instruments.Ammeter;
import circuitsim.components.electrical.Battery;
import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.logic.LogicGate;
import circuitsim.components.ports.CustomInputPort;
import circuitsim.components.ports.CustomOutputPort;
import circuitsim.components.electrical.Ground;
import circuitsim.components.electrical.Resistor;
import circuitsim.components.electrical.Source;
import circuitsim.components.core.SwitchLike;
import circuitsim.components.instruments.Voltmeter;
import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireNode;
import circuitsim.ui.Grid;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Circuit solver that computes currents and voltages for the scene.
 */
public final class CircuitPhysics {
    private static final double WIRE_RESISTANCE = 1e-9;
    private static final double MIN_RESISTANCE = 1e-9;
    private static final double SHORT_THRESHOLD = 1e-6;
    private static final double LOGIC_INPUT_RESISTANCE = 2e4;

    /**
     * Prevent instantiation.
     */
    private CircuitPhysics() {
    }

    /**
     * Updates computed values for the circuit.
     *
     * @return true if a short circuit is detected
     */
    public static boolean update(List<CircuitComponent> components, Collection<Wire> wires) {
        return updateInternal(components, wires, false);
    }

    /**
     * Updates computed values for the circuit.
     *
     * @param treatCustomOutputsAsGround true to treat custom outputs as ground (editor only)
     * @return true if a short circuit is detected
     */
    public static boolean update(List<CircuitComponent> components, Collection<Wire> wires,
                                 boolean treatCustomOutputsAsGround) {
        return updateInternal(components, wires, treatCustomOutputsAsGround);
    }

    /**
     * Performs the internal solver update.
     */
    private static boolean updateInternal(List<CircuitComponent> components, Collection<Wire> wires,
                                          boolean treatCustomOutputsAsGround) {
        if (components == null || wires == null) {
            return false;
        }
        Map<Point, Integer> nodeIndex = new HashMap<>();
        for (CircuitComponent component : components) {
            for (ConnectionPoint point : component.getConnectionPoints()) {
                int x = component.getConnectionPointWorldX(point);
                int y = component.getConnectionPointWorldY(point);
                getNodeIndex(nodeIndex, x, y);
            }
        }
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start == null || end == null) {
                continue;
            }
            getNodeIndex(nodeIndex, start.getX(), start.getY());
            getNodeIndex(nodeIndex, end.getX(), end.getY());
        }
        if (nodeIndex.isEmpty()) {
            return false;
        }

        List<Edge> edges = new ArrayList<>();
        List<Battery> batteries = new ArrayList<>();
        List<Ground> grounds = new ArrayList<>();
        List<Ground> groundComponents = new ArrayList<>();
        List<Voltmeter> voltmeters = new ArrayList<>();
        List<SwitchLike> switches = new ArrayList<>();
        List<CustomOutputPort> outputPorts = new ArrayList<>();
        List<CustomInputPort> inputPorts = new ArrayList<>();
        List<Source> sources = new ArrayList<>();
        List<LogicGate> logicGates = new ArrayList<>();
        for (CircuitComponent component : components) {
            if (component instanceof Resistor) {
                Resistor resistor = (Resistor) component;
                List<ConnectionPoint> points = resistor.getConnectionPoints();
                if (points.size() < 2) {
                    continue;
                }
                int aIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(0)),
                        component.getConnectionPointWorldY(points.get(0)));
                int bIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(1)),
                        component.getConnectionPointWorldY(points.get(1)));
                edges.add(new Edge(aIndex, bIndex,
                        Math.max(MIN_RESISTANCE, resistor.getResistance()), resistor));
            } else if (component instanceof Battery) {
                Battery battery = (Battery) component;
                batteries.add(battery);
            } else if (component instanceof Ammeter) {
                Ammeter ammeter = (Ammeter) component;
                List<ConnectionPoint> points = ammeter.getConnectionPoints();
                if (points.size() < 2) {
                    continue;
                }
                int aIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(0)),
                        component.getConnectionPointWorldY(points.get(0)));
                int bIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(1)),
                        component.getConnectionPointWorldY(points.get(1)));
                edges.add(new Edge(aIndex, bIndex, WIRE_RESISTANCE, ammeter));
            } else if (component instanceof Voltmeter) {
                voltmeters.add((Voltmeter) component);
            } else if (component instanceof CustomInputPort) {
                inputPorts.add((CustomInputPort) component);
            } else if (component instanceof Source) {
                sources.add((Source) component);
            } else if (component instanceof LogicGate) {
                logicGates.add((LogicGate) component);
            } else if (component instanceof SwitchLike) {
                SwitchLike circuitSwitch = (SwitchLike) component;
                switches.add(circuitSwitch);
                if (!circuitSwitch.isClosed()) {
                    continue;
                }
                List<ConnectionPoint> points = component.getConnectionPoints();
                if (points.size() < 2) {
                    continue;
                }
                int aIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(0)),
                        component.getConnectionPointWorldY(points.get(0)));
                int bIndex = getNodeIndex(nodeIndex,
                        component.getConnectionPointWorldX(points.get(1)),
                        component.getConnectionPointWorldY(points.get(1)));
                edges.add(new Edge(aIndex, bIndex, WIRE_RESISTANCE, circuitSwitch));
            } else if (component instanceof CustomOutputPort) {
                outputPorts.add((CustomOutputPort) component);
            }
            if (component instanceof Ground) {
                Ground ground = (Ground) component;
                grounds.add(ground);
                groundComponents.add(ground);
            } else if (component instanceof CustomOutputPort && treatCustomOutputsAsGround) {
                grounds.add(new GroundAdapter((CustomOutputPort) component));
            }
        }
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            int aIndex = getNodeIndex(nodeIndex, Grid.snap(wire.getStart().getX()),
                    Grid.snap(wire.getStart().getY()));
            int bIndex = getNodeIndex(nodeIndex, Grid.snap(wire.getEnd().getX()),
                    Grid.snap(wire.getEnd().getY()));
            edges.add(new Edge(aIndex, bIndex,
                    WIRE_RESISTANCE, wire));
        }
        int nodeCount = nodeIndex.size();
        java.awt.Point groundPoint = resolveGroundPoint(nodeIndex, grounds,
                batteries.isEmpty() ? null : batteries.get(0), wires);
        if (groundPoint == null && treatCustomOutputsAsGround && !outputPorts.isEmpty()) {
            groundPoint = getOutputPortPoint(outputPorts.get(0));
        }
        if (groundPoint == null && !logicGates.isEmpty()) {
            groundPoint = getLogicGateReferencePoint(logicGates.get(0));
        }
        LogicPhysics.updateLogicComponents(components, wires);
        addInputBatteries(batteries, inputPorts, groundPoint);
        addSourceBatteries(batteries, sources, groundPoint);
        addLogicGateBatteries(batteries, logicGates, groundPoint);
        addLogicGateInputLoads(edges, logicGates, nodeIndex, groundPoint);
        if (batteries.isEmpty()) {
            resetComputedValues(edges);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            resetGroundIndicators(groundComponents);
            return false;
        }
        for (Battery battery : batteries) {
            ConnectionPoint neg = battery.getNegativePoint();
            ConnectionPoint pos = battery.getPositivePoint();
            if (neg == null || pos == null) {
                continue;
            }
            int negIndex = getNodeIndex(nodeIndex,
                    battery.getConnectionPointWorldX(neg),
                    battery.getConnectionPointWorldY(neg));
            int posIndex = getNodeIndex(nodeIndex,
                    battery.getConnectionPointWorldX(pos),
                    battery.getConnectionPointWorldY(pos));
            int internalNode = nodeCount++;
            double resistance = Math.max(MIN_RESISTANCE, battery.getInternalResistance());
            edges.add(new Edge(negIndex, internalNode, resistance));
            battery.setInternalNodeIndex(internalNode);
            battery.setPositiveNodeIndex(posIndex);
        }

        Battery primaryBattery = batteries.get(0);
        ConnectionPoint negative = primaryBattery.getNegativePoint();
        ConnectionPoint positive = primaryBattery.getPositivePoint();
        if (negative == null || positive == null) {
            resetComputedValues(edges);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            resetGroundIndicators(groundComponents);
            return false;
        }
        groundPoint = resolveGroundPoint(nodeIndex, grounds, primaryBattery, wires);
        if (groundPoint == null && treatCustomOutputsAsGround && !outputPorts.isEmpty()) {
            groundPoint = getOutputPortPoint(outputPorts.get(0));
        }
        Integer groundIndex = groundPoint == null ? null : nodeIndex.get(groundPoint);
        if ((groundIndex == null || groundIndex < 0) && primaryBattery != null) {
            java.awt.Point fallbackGround = getBatteryNegativePoint(primaryBattery);
            groundPoint = fallbackGround;
            groundIndex = fallbackGround == null ? null : nodeIndex.get(fallbackGround);
        }
        if (groundIndex == null || groundIndex < 0) {
            resetComputedValues(edges);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            resetGroundIndicators(groundComponents);
            return false;
        }
        int positiveIndex = getNodeIndex(nodeIndex,
                primaryBattery.getConnectionPointWorldX(positive),
                primaryBattery.getConnectionPointWorldY(positive));
        GraphView pruned = pruneToConnected(nodeCount, edges, batteries, groundIndex, positiveIndex);
        if (pruned.edges.isEmpty() && primaryBattery != null && grounds != null && !grounds.isEmpty()) {
            java.awt.Point fallbackGround = getBatteryNegativePoint(primaryBattery);
            Integer fallbackGroundIndex = fallbackGround == null ? null : nodeIndex.get(fallbackGround);
            if (fallbackGroundIndex != null && fallbackGroundIndex >= 0 && fallbackGroundIndex != groundIndex) {
                pruned = pruneToConnected(nodeCount, edges, batteries, fallbackGroundIndex, positiveIndex);
            }
        }
        if (pruned.edges.isEmpty()) {
            resetComputedValues(edges);
            resetUnusedValues(edges, wires, pruned);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            return false;
        }

        boolean allowShortCircuit = areAllInputBatteries(batteries);
        boolean shortCircuit = detectShortCircuit(pruned.positiveIndex, pruned.groundIndex, pruned.nodeCount,
                pruned.edges);
        if (shortCircuit && !allowShortCircuit) {
            resetComputedValues(pruned.edges);
            resetUnusedValues(edges, wires, pruned);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            resetGroundIndicators(groundComponents);
            return true;
        }
        if (allowShortCircuit) {
            shortCircuit = false;
        }

        double[] nodeVoltages = solveNodeVoltages(pruned.nodeCount, pruned.edges, pruned.batteries,
                pruned.groundIndex);
        if (nodeVoltages == null) {
            resetComputedValues(pruned.edges);
            resetUnusedValues(edges, wires, pruned);
            resetVoltmeterValues(voltmeters);
            resetSwitchValues(switches);
            resetOutputIndicators(outputPorts);
            resetGroundIndicators(groundComponents);
            return false;
        }

        java.util.Set<Integer> activeNodes = new java.util.HashSet<>();
        for (Edge edge : pruned.edges) {
            int a = edge.aIndex;
            int b = edge.bIndex;
            double va = nodeVoltages[a];
            double vb = nodeVoltages[b];
            double voltage = va - vb;
            double current = voltage / edge.resistance;
            if (Math.abs(current) > 0.0001) {
                activeNodes.add(a);
                activeNodes.add(b);
            }
            if (edge.wire != null) {
                edge.wire.setComputedVoltage((float) Math.abs(voltage));
                edge.wire.setComputedAmpere((float) Math.abs(current));
            }
            if (edge.resistor != null) {
                edge.resistor.setComputedVoltage((float) Math.abs(voltage));
                edge.resistor.setComputedAmpere((float) Math.abs(current));
            }
            if (edge.ammeter != null) {
                edge.ammeter.setComputedAmpere((float) Math.abs(current));
            }
            if (edge.circuitSwitch != null) {
                edge.circuitSwitch.setComputedAmpere((float) Math.abs(current));
            }
        }
        updateVoltmeterValues(voltmeters, nodeIndex, pruned, nodeVoltages);
        resetUnusedValues(edges, wires, pruned);
        
        // Update logic components after analog simulation
        LogicPhysics.updateLogicComponents(components, wires);
        resetSwitchValues(switches);
        updateOutputIndicators(outputPorts, nodeIndex, pruned, activeNodes);
        updateGroundIndicators(groundComponents, nodeIndex, pruned, activeNodes, wires);
        return false;
    }

    /**
     * Resets computed values for edges in the provided list.
     */
    private static void resetComputedValues(List<Edge> edges) {
        for (Edge edge : edges) {
            if (edge.wire != null) {
                edge.wire.setComputedVoltage(0f);
                edge.wire.setComputedAmpere(0f);
            }
            if (edge.resistor != null) {
                edge.resistor.setComputedVoltage(0f);
                edge.resistor.setComputedAmpere(0f);
            }
            if (edge.ammeter != null) {
                edge.ammeter.setComputedAmpere(0f);
            }
            if (edge.circuitSwitch != null) {
                edge.circuitSwitch.setComputedAmpere(0f);
            }
        }
    }

    /**
     * Resets computed values for items not included in the pruned graph.
     */
    private static void resetUnusedValues(List<Edge> allEdges, Collection<Wire> wires, GraphView pruned) {
        for (Wire wire : wires) {
            if (!pruned.wires.contains(wire)) {
                wire.setComputedVoltage(0f);
                wire.setComputedAmpere(0f);
            }
        }
        for (Edge edge : allEdges) {
            if (edge.resistor != null && !pruned.resistors.contains(edge.resistor)) {
                edge.resistor.setComputedVoltage(0f);
                edge.resistor.setComputedAmpere(0f);
            }
            if (edge.ammeter != null && !pruned.ammeters.contains(edge.ammeter)) {
                edge.ammeter.setComputedAmpere(0f);
            }
            if (edge.circuitSwitch != null && !pruned.switches.contains(edge.circuitSwitch)) {
                edge.circuitSwitch.setComputedAmpere(0f);
            }
        }
    }

    /**
     * Clears computed voltages for all voltmeters.
     */
    private static void resetVoltmeterValues(List<Voltmeter> voltmeters) {
        for (Voltmeter voltmeter : voltmeters) {
            voltmeter.setComputedVoltage(0f);
        }
    }

    /**
     * Resets computed current for open switches.
     */
    private static void resetSwitchValues(List<SwitchLike> switches) {
        for (SwitchLike circuitSwitch : switches) {
            if (!circuitSwitch.isClosed()) {
                circuitSwitch.setComputedAmpere(0f);
            }
        }
    }

    private static void resetOutputIndicators(List<CustomOutputPort> outputPorts) {
        for (CustomOutputPort outputPort : outputPorts) {
            outputPort.setActiveIndicator(false);
        }
    }

    private static void resetGroundIndicators(List<Ground> groundComponents) {
        for (Ground ground : groundComponents) {
            ground.setActiveIndicator(false);
        }
    }

    private static void updateOutputIndicators(List<CustomOutputPort> outputPorts, Map<Point, Integer> nodeIndex,
                                               GraphView pruned, java.util.Set<Integer> activeNodes) {
        if (outputPorts.isEmpty()) {
            return;
        }
        for (CustomOutputPort outputPort : outputPorts) {
            List<ConnectionPoint> points = outputPort.getConnectionPoints();
            if (points.isEmpty()) {
                outputPort.setActiveIndicator(false);
                continue;
            }
            ConnectionPoint point = points.get(0);
            Integer originalIndex = nodeIndex.get(new Point(
                    outputPort.getConnectionPointWorldX(point),
                    outputPort.getConnectionPointWorldY(point)));
            if (originalIndex == null) {
                outputPort.setActiveIndicator(false);
                continue;
            }
            int remapped = pruned.nodeRemap[originalIndex];
            outputPort.setActiveIndicator(remapped >= 0 && activeNodes.contains(remapped));
        }
    }

    private static void updateGroundIndicators(List<Ground> groundComponents, Map<Point, Integer> nodeIndex,
                                               GraphView pruned, java.util.Set<Integer> activeNodes,
                                               Collection<Wire> wires) {
        if (groundComponents.isEmpty()) {
            return;
        }
        for (Ground ground : groundComponents) {
            List<ConnectionPoint> points = ground.getConnectionPoints();
            if (points.isEmpty()) {
                ground.setActiveIndicator(false);
                continue;
            }
            ConnectionPoint point = points.get(0);
            int groundX = ground.getConnectionPointWorldX(point);
            int groundY = ground.getConnectionPointWorldY(point);
            Integer originalIndex = nodeIndex.get(new Point(groundX, groundY));
            if (originalIndex == null) {
                ground.setActiveIndicator(false);
                continue;
            }
            int remapped = pruned.nodeRemap[originalIndex];
            boolean nodeActive = remapped >= 0 && activeNodes.contains(remapped);
            ground.setActiveIndicator(nodeActive && isWirePoweredAt(wires, groundX, groundY));
        }
    }

    private static boolean isWirePoweredAt(Collection<Wire> wires, int x, int y) {
        if (wires == null || wires.isEmpty()) {
            return false;
        }
        int sx = Grid.snap(x);
        int sy = Grid.snap(y);
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start != null
                    && Grid.snap(start.getX()) == sx
                    && Grid.snap(start.getY()) == sy
                    && wire.getComputedAmpere() > 0.0001f) {
                return true;
            }
            if (end != null
                    && Grid.snap(end.getX()) == sx
                    && Grid.snap(end.getY()) == sy
                    && wire.getComputedAmpere() > 0.0001f) {
                return true;
            }
        }
        return false;
    }

    private static final class GroundAdapter extends Ground {
        private final CustomOutputPort outputPort;

        private GroundAdapter(CustomOutputPort outputPort) {
            super(outputPort.getX(), outputPort.getY());
            this.outputPort = outputPort;
        }

        @Override
        public List<ConnectionPoint> getConnectionPoints() {
            return outputPort.getConnectionPoints();
        }

        @Override
        public int getConnectionPointWorldX(ConnectionPoint point) {
            return outputPort.getConnectionPointWorldX(point);
        }

        @Override
        public int getConnectionPointWorldY(ConnectionPoint point) {
            return outputPort.getConnectionPointWorldY(point);
        }
    }

    /**
     * Updates voltmeters based on solved node voltages.
     */
    private static void updateVoltmeterValues(List<Voltmeter> voltmeters, Map<Point, Integer> nodeIndex,
            GraphView pruned, double[] nodeVoltages) {
        for (Voltmeter voltmeter : voltmeters) {
            List<ConnectionPoint> points = voltmeter.getConnectionPoints();
            if (points.size() < 2) {
                voltmeter.setComputedVoltage(0f);
                continue;
            }
            Integer aIndex = nodeIndex.get(new Point(
                    voltmeter.getConnectionPointWorldX(points.get(0)),
                    voltmeter.getConnectionPointWorldY(points.get(0))));
            Integer bIndex = nodeIndex.get(new Point(
                    voltmeter.getConnectionPointWorldX(points.get(1)),
                    voltmeter.getConnectionPointWorldY(points.get(1))));
            if (aIndex == null || bIndex == null) {
                voltmeter.setComputedVoltage(0f);
                continue;
            }
            int aRemap = pruned.nodeRemap[aIndex];
            int bRemap = pruned.nodeRemap[bIndex];
            if (aRemap < 0 || bRemap < 0 || aRemap >= nodeVoltages.length || bRemap >= nodeVoltages.length) {
                voltmeter.setComputedVoltage(0f);
                continue;
            }
            double voltage = nodeVoltages[aRemap] - nodeVoltages[bRemap];
            voltmeter.setComputedVoltage((float) Math.abs(voltage));
        }
    }

    /**
     * Resolves the ground node index, falling back to the battery negative terminal.
     */
    private static java.awt.Point resolveGroundPoint(Map<Point, Integer> nodeIndex, List<Ground> grounds,
            Battery primaryBattery, Collection<Wire> wires) {
        if (grounds != null && !grounds.isEmpty()) {
            for (Ground ground : grounds) {
                List<ConnectionPoint> points = ground.getConnectionPoints();
                if (points.isEmpty()) {
                    continue;
                }
                ConnectionPoint point = points.get(0);
                int x = ground.getConnectionPointWorldX(point);
                int y = ground.getConnectionPointWorldY(point);
                if (!isWireConnectedAt(wires, x, y)) {
                    continue;
                }
                return new java.awt.Point(x, y);
            }
        }
        if (primaryBattery == null) {
            return null;
        }
        return getBatteryNegativePoint(primaryBattery);
    }

    private static boolean isWireConnectedAt(Collection<Wire> wires, int x, int y) {
        if (wires == null || wires.isEmpty()) {
            return false;
        }
        int sx = Grid.snap(x);
        int sy = Grid.snap(y);
        for (Wire wire : wires) {
            WireNode start = wire.getStart();
            WireNode end = wire.getEnd();
            if (start != null
                    && Grid.snap(start.getX()) == sx
                    && Grid.snap(start.getY()) == sy) {
                return true;
            }
            if (end != null
                    && Grid.snap(end.getX()) == sx
                    && Grid.snap(end.getY()) == sy) {
                return true;
            }
        }
        return false;
    }

    private static java.awt.Point getBatteryNegativePoint(Battery battery) {
        if (battery == null) {
            return null;
        }
        ConnectionPoint negative = battery.getNegativePoint();
        if (negative == null) {
            return null;
        }
        return new java.awt.Point(battery.getConnectionPointWorldX(negative),
                battery.getConnectionPointWorldY(negative));
    }

    private static void addInputBatteries(List<Battery> batteries, List<CustomInputPort> inputPorts,
                                          java.awt.Point groundPoint) {
        if (inputPorts.isEmpty() || groundPoint == null) {
            return;
        }
        for (CustomInputPort inputPort : inputPorts) {
            if (!inputPort.isActive()) {
                continue;
            }
            List<ConnectionPoint> points = inputPort.getConnectionPoints();
            if (points.isEmpty()) {
                continue;
            }
            ConnectionPoint point = points.get(0);
            int posX = inputPort.getConnectionPointWorldX(point);
            int posY = inputPort.getConnectionPointWorldY(point);
            batteries.add(new InputBatteryAdapter(posX, posY, groundPoint.x, groundPoint.y));
        }
    }

    private static void addSourceBatteries(List<Battery> batteries, List<Source> sources,
                                           java.awt.Point groundPoint) {
        if (sources.isEmpty() || groundPoint == null) {
            return;
        }
        for (Source source : sources) {
            if (!source.isActive()) {
                continue;
            }
            List<ConnectionPoint> points = source.getConnectionPoints();
            if (points.isEmpty()) {
                continue;
            }
            ConnectionPoint point = points.get(0);
            int posX = source.getConnectionPointWorldX(point);
            int posY = source.getConnectionPointWorldY(point);
            batteries.add(new InputBatteryAdapter(posX, posY, groundPoint.x, groundPoint.y));
        }
    }

    private static void addLogicGateBatteries(List<Battery> batteries, List<LogicGate> logicGates,
                                              java.awt.Point groundPoint) {
        if (logicGates.isEmpty() || groundPoint == null) {
            return;
        }
        for (LogicGate gate : logicGates) {
            if (!gate.isOutputPowered()) {
                continue;
            }
            ConnectionPoint output = gate.getOutputPoint();
            if (output == null) {
                continue;
            }
            int posX = gate.getConnectionPointWorldX(output);
            int posY = gate.getConnectionPointWorldY(output);
            batteries.add(new InputBatteryAdapter(posX, posY, groundPoint.x, groundPoint.y));
        }
    }

    private static void addLogicGateInputLoads(List<Edge> edges, List<LogicGate> logicGates,
                                               Map<Point, Integer> nodeIndex, java.awt.Point groundPoint) {
        if (logicGates.isEmpty() || groundPoint == null) {
            return;
        }
        int groundIndex = getNodeIndex(nodeIndex, groundPoint.x, groundPoint.y);
        for (LogicGate gate : logicGates) {
            for (ConnectionPoint point : gate.getConnectionPoints()) {
                if (!gate.isInputPoint(point)) {
                    continue;
                }
                int node = getNodeIndex(nodeIndex,
                        gate.getConnectionPointWorldX(point),
                        gate.getConnectionPointWorldY(point));
                if (node == groundIndex) {
                    continue;
                }
                edges.add(new Edge(node, groundIndex, LOGIC_INPUT_RESISTANCE));
            }
        }
    }

    private static java.awt.Point getOutputPortPoint(CustomOutputPort outputPort) {
        if (outputPort == null) {
            return null;
        }
        List<ConnectionPoint> points = outputPort.getConnectionPoints();
        if (points.isEmpty()) {
            return null;
        }
        ConnectionPoint point = points.get(0);
        return new java.awt.Point(outputPort.getConnectionPointWorldX(point),
                outputPort.getConnectionPointWorldY(point));
    }

    private static boolean areAllInputBatteries(List<Battery> batteries) {
        if (batteries.isEmpty()) {
            return false;
        }
        for (Battery battery : batteries) {
            if (!(battery instanceof InputBatteryAdapter)) {
                return false;
            }
        }
        return true;
    }

    private static java.awt.Point getLogicGateReferencePoint(LogicGate gate) {
        if (gate == null) {
            return null;
        }
        ConnectionPoint output = gate.getOutputPoint();
        if (output != null) {
            return new java.awt.Point(gate.getConnectionPointWorldX(output),
                    gate.getConnectionPointWorldY(output));
        }
        List<ConnectionPoint> points = gate.getConnectionPoints();
        if (points.isEmpty()) {
            return null;
        }
        ConnectionPoint point = points.get(0);
        return new java.awt.Point(gate.getConnectionPointWorldX(point),
                gate.getConnectionPointWorldY(point));
    }

    private static final class InputBatteryAdapter extends Battery {
        private static final float INPUT_VOLTAGE = 5f;
        private static final float INPUT_RESISTANCE = 0.2f;
        private final ConnectionPoint negativePoint = new ConnectionPoint(this, 0f, 0f);
        private final ConnectionPoint positivePoint = new ConnectionPoint(this, 0f, 0f);
        private final int negX;
        private final int negY;
        private final int posX;
        private final int posY;

        private InputBatteryAdapter(int posX, int posY, int negX, int negY) {
            super(0, 0, INPUT_VOLTAGE, INPUT_RESISTANCE);
            this.posX = posX;
            this.posY = posY;
            this.negX = negX;
            this.negY = negY;
        }

        @Override
        public ConnectionPoint getNegativePoint() {
            return negativePoint;
        }

        @Override
        public ConnectionPoint getPositivePoint() {
            return positivePoint;
        }

        @Override
        public int getConnectionPointWorldX(ConnectionPoint point) {
            if (point == positivePoint) {
                return posX;
            }
            if (point == negativePoint) {
                return negX;
            }
            return super.getConnectionPointWorldX(point);
        }

        @Override
        public int getConnectionPointWorldY(ConnectionPoint point) {
            if (point == positivePoint) {
                return posY;
            }
            if (point == negativePoint) {
                return negY;
            }
            return super.getConnectionPointWorldY(point);
        }
    }


    /**
     * Detects a short circuit via a minimal-resistance path between nodes.
     */
    private static boolean detectShortCircuit(int start, int goal, int nodeCount, List<Edge> edges) {
        double[] dist = new double[nodeCount];
        boolean[] visited = new boolean[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
        }
        dist[start] = 0;
        for (int i = 0; i < nodeCount; i++) {
            int current = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int j = 0; j < nodeCount; j++) {
                if (!visited[j] && dist[j] < best) {
                    best = dist[j];
                    current = j;
                }
            }
            if (current == -1) {
                break;
            }
            if (current == goal) {
                break;
            }
            visited[current] = true;
            for (Edge edge : edges) {
                int a = edge.aIndex;
                int b = edge.bIndex;
                if (a != current && b != current) {
                    continue;
                }
                int next = a == current ? b : a;
                double alt = dist[current] + edge.resistance;
                if (alt < dist[next]) {
                    dist[next] = alt;
                }
            }
        }
        return dist[goal] <= SHORT_THRESHOLD;
    }

    /**
     * Solves node voltages using modified nodal analysis.
     */
    private static double[] solveNodeVoltages(int nodeCount, List<Edge> edges,
            List<Battery> batteries, int groundIndex) {
        int voltageSourceCount = batteries.size();
        int unknownNodeCount = nodeCount - 1;
        if (unknownNodeCount <= 0) {
            return null;
        }
        int size = unknownNodeCount + voltageSourceCount;
        double[][] matrix = new double[size][size];
        double[] rhs = new double[size];

        for (Edge edge : edges) {
            int a = edge.aIndex;
            int b = edge.bIndex;
            double conductance = 1.0 / edge.resistance;
            if (a != groundIndex) {
                int ia = nodeToMatrixIndex(a, groundIndex);
                matrix[ia][ia] += conductance;
            }
            if (b != groundIndex) {
                int ib = nodeToMatrixIndex(b, groundIndex);
                matrix[ib][ib] += conductance;
            }
            if (a != groundIndex && b != groundIndex) {
                int ia = nodeToMatrixIndex(a, groundIndex);
                int ib = nodeToMatrixIndex(b, groundIndex);
                matrix[ia][ib] -= conductance;
                matrix[ib][ia] -= conductance;
            }
        }

        for (int i = 0; i < batteries.size(); i++) {
            Battery battery = batteries.get(i);
            int p = battery.getPositiveNodeIndex();
            int n = battery.getInternalNodeIndex();
            if (p < 0 || n < 0) {
                continue;
            }
            int row = unknownNodeCount + i;
            if (p != groundIndex) {
                int ip = nodeToMatrixIndex(p, groundIndex);
                matrix[ip][row] += 1;
                matrix[row][ip] += 1;
            }
            if (n != groundIndex) {
                int in = nodeToMatrixIndex(n, groundIndex);
                matrix[in][row] -= 1;
                matrix[row][in] -= 1;
            }
            rhs[row] = battery.getVoltage();
        }

        double[] solution = solveLinearSystem(matrix, rhs);
        if (solution == null) {
            return null;
        }
        double[] voltages = new double[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            if (i == groundIndex) {
                voltages[i] = 0;
            } else {
                int idx = nodeToMatrixIndex(i, groundIndex);
                voltages[i] = solution[idx];
            }
        }
        return voltages;
    }

    /**
     * Maps a node index to a matrix index that excludes ground.
     */
    private static int nodeToMatrixIndex(int nodeIndex, int groundIndex) {
        return nodeIndex < groundIndex ? nodeIndex : nodeIndex - 1;
    }

    /**
     * Solves a linear system using Gaussian elimination.
     */
    private static double[] solveLinearSystem(double[][] matrix, double[] rhs) {
        int n = rhs.length;
        double[][] a = new double[n][n];
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, a[i], 0, n);
            b[i] = rhs[i];
        }
        for (int pivot = 0; pivot < n; pivot++) {
            int maxRow = pivot;
            for (int row = pivot + 1; row < n; row++) {
                if (Math.abs(a[row][pivot]) > Math.abs(a[maxRow][pivot])) {
                    maxRow = row;
                }
            }
            if (Math.abs(a[maxRow][pivot]) < 1e-12) {
                return null;
            }
            if (maxRow != pivot) {
                double[] tempRow = a[pivot];
                a[pivot] = a[maxRow];
                a[maxRow] = tempRow;
                double tempVal = b[pivot];
                b[pivot] = b[maxRow];
                b[maxRow] = tempVal;
            }
            double pivotVal = a[pivot][pivot];
            for (int col = pivot; col < n; col++) {
                a[pivot][col] /= pivotVal;
            }
            b[pivot] /= pivotVal;
            for (int row = 0; row < n; row++) {
                if (row == pivot) {
                    continue;
                }
                double factor = a[row][pivot];
                for (int col = pivot; col < n; col++) {
                    a[row][col] -= factor * a[pivot][col];
                }
                b[row] -= factor * b[pivot];
            }
        }
        return b;
    }

    /**
     * Returns the node index for a point, creating one if missing.
     */
    private static int getNodeIndex(Map<Point, Integer> nodeIndex, int x, int y) {
        Point key = new Point(x, y);
        Integer existing = nodeIndex.get(key);
        if (existing != null) {
            return existing;
        }
        int index = nodeIndex.size();
        nodeIndex.put(key, index);
        return index;
    }

    /**
     * Edge between two nodes, optionally tied to a component.
     */
    private static class Edge {
        private final int aIndex;
        private final int bIndex;
        private final double resistance;
        private final Wire wire;
        private final Resistor resistor;
        private final Ammeter ammeter;
        private final SwitchLike circuitSwitch;

        /**
         * @param aIndex node A index
         * @param bIndex node B index
         * @param resistance resistance value
         */
        private Edge(int aIndex, int bIndex, double resistance) {
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.resistance = resistance;
            this.wire = null;
            this.resistor = null;
            this.ammeter = null;
            this.circuitSwitch = null;
        }

        /**
         * @param aIndex node A index
         * @param bIndex node B index
         * @param resistance resistance value
         * @param wire associated wire
         */
        private Edge(int aIndex, int bIndex, double resistance, Wire wire) {
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.resistance = resistance;
            this.wire = wire;
            this.resistor = null;
            this.ammeter = null;
            this.circuitSwitch = null;
        }

        /**
         * @param aIndex node A index
         * @param bIndex node B index
         * @param resistance resistance value
         * @param resistor associated resistor
         */
        private Edge(int aIndex, int bIndex, double resistance, Resistor resistor) {
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.resistance = resistance;
            this.wire = null;
            this.resistor = resistor;
            this.ammeter = null;
            this.circuitSwitch = null;
        }

        /**
         * @param aIndex node A index
         * @param bIndex node B index
         * @param resistance resistance value
         * @param ammeter associated ammeter
         */
        private Edge(int aIndex, int bIndex, double resistance, Ammeter ammeter) {
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.resistance = resistance;
            this.wire = null;
            this.resistor = null;
            this.ammeter = ammeter;
            this.circuitSwitch = null;
        }

        /**
         * @param aIndex node A index
         * @param bIndex node B index
         * @param resistance resistance value
         * @param circuitSwitch associated switch
         */
        private Edge(int aIndex, int bIndex, double resistance, SwitchLike circuitSwitch) {
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.resistance = resistance;
            this.wire = null;
            this.resistor = null;
            this.ammeter = null;
            this.circuitSwitch = circuitSwitch;
        }
    }

    /**
     * Snapshot of a pruned graph used for solving.
     */
    private static class GraphView {
        private final int nodeCount;
        private final int groundIndex;
        private final int positiveIndex;
        private final List<Edge> edges;
        private final List<Battery> batteries;
        private final java.util.Set<Wire> wires;
        private final java.util.Set<Resistor> resistors;
        private final java.util.Set<Ammeter> ammeters;
        private final java.util.Set<SwitchLike> switches;
        private final int[] nodeRemap;

        /**
         * @param nodeCount total nodes in the pruned graph
         * @param groundIndex ground node index
         * @param positiveIndex positive node index
         * @param edges pruned edges
         * @param batteries pruned batteries
         * @param wires pruned wires
         * @param resistors pruned resistors
         * @param ammeters pruned ammeters
         * @param switches pruned switches
         * @param nodeRemap remap table from original to pruned indices
         */
        private GraphView(int nodeCount, int groundIndex, int positiveIndex, List<Edge> edges,
                List<Battery> batteries, java.util.Set<Wire> wires, java.util.Set<Resistor> resistors,
                java.util.Set<Ammeter> ammeters, java.util.Set<SwitchLike> switches, int[] nodeRemap) {
            this.nodeCount = nodeCount;
            this.groundIndex = groundIndex;
            this.positiveIndex = positiveIndex;
            this.edges = edges;
            this.batteries = batteries;
            this.wires = wires;
            this.resistors = resistors;
            this.ammeters = ammeters;
            this.switches = switches;
            this.nodeRemap = nodeRemap;
        }
    }

    /**
     * Prunes the graph to nodes connected to ground or positive terminals.
     */
    private static GraphView pruneToConnected(int nodeCount, List<Edge> edges, List<Battery> batteries,
            int groundIndex, int positiveIndex) {
        java.util.List<java.util.List<Integer>> adjacency = new java.util.ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new java.util.ArrayList<>());
        }
        for (Edge edge : edges) {
            adjacency.get(edge.aIndex).add(edge.bIndex);
            adjacency.get(edge.bIndex).add(edge.aIndex);
        }
        for (Battery battery : batteries) {
            int p = battery.getPositiveNodeIndex();
            int n = battery.getInternalNodeIndex();
            if (p >= 0 && n >= 0) {
                adjacency.get(p).add(n);
                adjacency.get(n).add(p);
            }
        }
        boolean[] reachable = new boolean[nodeCount];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        if (groundIndex >= 0) {
            reachable[groundIndex] = true;
            queue.add(groundIndex);
        }
        if (positiveIndex >= 0 && !reachable[positiveIndex]) {
            reachable[positiveIndex] = true;
            queue.add(positiveIndex);
        }
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (int next : adjacency.get(current)) {
                if (!reachable[next]) {
                    reachable[next] = true;
                    queue.add(next);
                }
            }
        }
        int[] remap = new int[nodeCount];
        java.util.Arrays.fill(remap, -1);
        int newCount = 0;
        for (int i = 0; i < nodeCount; i++) {
            if (reachable[i]) {
                remap[i] = newCount++;
            }
        }
        java.util.List<Edge> prunedEdges = new java.util.ArrayList<>();
        java.util.Set<Wire> prunedWires = new java.util.HashSet<>();
        java.util.Set<Resistor> prunedResistors = new java.util.HashSet<>();
        java.util.Set<Ammeter> prunedAmmeters = new java.util.HashSet<>();
        java.util.Set<SwitchLike> prunedSwitches = new java.util.HashSet<>();
        for (Edge edge : edges) {
            int a = remap[edge.aIndex];
            int b = remap[edge.bIndex];
            if (a >= 0 && b >= 0) {
                Edge remapped;
                if (edge.wire != null) {
                    remapped = new Edge(a, b, edge.resistance, edge.wire);
                    prunedWires.add(edge.wire);
                } else if (edge.resistor != null) {
                    remapped = new Edge(a, b, edge.resistance, edge.resistor);
                    prunedResistors.add(edge.resistor);
                } else if (edge.ammeter != null) {
                    remapped = new Edge(a, b, edge.resistance, edge.ammeter);
                    prunedAmmeters.add(edge.ammeter);
                } else if (edge.circuitSwitch != null) {
                    remapped = new Edge(a, b, edge.resistance, edge.circuitSwitch);
                    prunedSwitches.add(edge.circuitSwitch);
                } else {
                    remapped = new Edge(a, b, edge.resistance);
                }
                prunedEdges.add(remapped);
            }
        }
        java.util.List<Battery> prunedBatteries = new java.util.ArrayList<>();
        for (Battery battery : batteries) {
            int p = remap[battery.getPositiveNodeIndex()];
            int n = remap[battery.getInternalNodeIndex()];
            if (p >= 0 && n >= 0) {
                battery.setPositiveNodeIndex(p);
                battery.setInternalNodeIndex(n);
                prunedBatteries.add(battery);
            }
        }
        int remappedGround = remap[groundIndex];
        int remappedPositive = remap[positiveIndex];
        if (remappedGround < 0 || remappedPositive < 0) {
            return new GraphView(0, remappedGround, remappedPositive, java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(), prunedWires, prunedResistors, prunedAmmeters, prunedSwitches,
                    remap);
        }
        return new GraphView(newCount, remappedGround, remappedPositive, prunedEdges,
                prunedBatteries, prunedWires, prunedResistors, prunedAmmeters, prunedSwitches, remap);
    }
}
