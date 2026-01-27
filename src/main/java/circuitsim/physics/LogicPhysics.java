package circuitsim.physics;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.electrical.Source;
import circuitsim.components.logic.ANDGate;
import circuitsim.components.logic.LogicGate;
import circuitsim.components.logic.NANDGate;
import circuitsim.components.logic.NOTGate;
import circuitsim.components.logic.ORGate;
import circuitsim.components.logic.XORGate;
import circuitsim.components.ports.CustomInputPort;
import circuitsim.components.ports.CustomOutputPort;
import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireNode;
import circuitsim.ui.Grid;
import java.awt.Point;
import java.util.Collection;
import java.util.List;

/**
 * Logic gate simulator for digital components like NAND gates.
 * SIMPLE VERSION: Focused on making NAND gate work perfectly.
 */
public final class LogicPhysics {
    
    private static final float HIGH_VOLTAGE = 5f;
    private static final float LOW_VOLTAGE = 0f;
    private static final float THRESHOLD_VOLTAGE = 2.5f;
    private static final int MAX_STABILIZATION_PASSES = 32;
    private static final java.util.Map<java.awt.Point, Boolean> OUTPUT_MEMORY =
            new java.util.HashMap<>();

    public static final class LogicGateInputs {
        private final float[] voltages;

        public LogicGateInputs(float[] voltages) {
            this.voltages = voltages == null ? new float[0] : voltages.clone();
        }

        public float[] getVoltages() {
            return voltages.clone();
        }
    }
    
    private LogicPhysics() {
    }
    
    /**
     * Updates all logic components in circuit.
     */
    public static void updateLogicComponents(Collection<CircuitComponent> components, Collection<Wire> wires) {
        if (components == null || wires == null) {
            return;
        }

        // Digital feedback (e.g., SR latches) requires repeated evaluation until outputs settle.
        // Use an asynchronous relaxation update (Gauss-Seidel style) so feedback loops converge
        // instead of oscillating between global states.
        Connectivity connectivity = new Connectivity(wires);
        java.util.List<LogicGate> logicGates = new java.util.ArrayList<>();
        java.util.List<CustomOutputPort> outputPorts = new java.util.ArrayList<>();
        java.util.List<Seed> seeds = new java.util.ArrayList<>();

        for (CircuitComponent component : components) {
            switch (component) {
                case LogicGate gate -> logicGates.add(gate);
                case CustomOutputPort outputPort -> outputPorts.add(outputPort);
                case CustomInputPort inputPort -> {
                    if (inputPort.isActive()) {
                        java.awt.Point seedPoint = getPrimaryPoint(inputPort);
                        if (seedPoint != null) {
                            seeds.add(new Seed(seedPoint, true));
                        }
                    }
                }
                case Source source -> {
                    if (source.isActive()) {
                        java.awt.Point seedPoint = getPrimaryPoint(source);
                        if (seedPoint != null) {
                            seeds.add(new Seed(seedPoint, true));
                        }
                    }
                }
                default -> {
                }
            }
        }

        java.util.Map<java.awt.Point, Boolean> fixedHigh = new java.util.HashMap<>();
        java.util.Map<java.awt.Point, Boolean> networkValue = new java.util.HashMap<>();

        // Seed from explicit sources/inputs (fixed HIGH).
        for (Seed seed : seeds) {
            int root = connectivity.root(connectivity.getOrCreate(seed.point));
            java.awt.Point rep = connectivity.representative(root);
            if (rep != null && seed.high) {
                fixedHigh.put(rep, true);
                networkValue.put(rep, true);
            }
        }

        // Seed from analog voltage (fixed HIGH).
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            if (wire.getComputedVoltage() < THRESHOLD_VOLTAGE) {
                continue;
            }
            java.awt.Point start = snapPoint(wire.getStart().getX(), wire.getStart().getY());
            int root = connectivity.root(connectivity.getOrCreate(start));
            java.awt.Point rep = connectivity.representative(root);
            if (rep != null) {
                fixedHigh.put(rep, true);
                networkValue.put(rep, true);
            }
        }

        // Initialize from previous tick memory for deterministic latch startup.
        for (LogicGate gate : logicGates) {
            java.awt.Point outputKey = getOutputKey(gate);
            if (outputKey == null) {
                continue;
            }
            int root = connectivity.root(connectivity.getOrCreate(outputKey));
            java.awt.Point rep = connectivity.representative(root);
            if (rep == null || fixedHigh.containsKey(rep)) {
                continue;
            }
            boolean remembered = OUTPUT_MEMORY.getOrDefault(outputKey, gate.isOutputPowered());
            networkValue.put(rep, remembered);
        }

        for (int pass = 0; pass < MAX_STABILIZATION_PASSES; pass++) {
            boolean changed = false;
            for (LogicGate gate : logicGates) {
                GateState state = evaluateGate(gate, connectivity, networkValue);
                gate.setInputPowered(0, state.inputAHigh);
                gate.setInputPowered(1, state.inputBHigh);

                boolean previousOutput = gate.isOutputPowered();
                gate.setOutputPowered(state.outputHigh);
                if (previousOutput != state.outputHigh) {
                    changed = true;
                }

                java.awt.Point outputKey = getOutputKey(gate);
                if (outputKey == null) {
                    continue;
                }
                int root = connectivity.root(connectivity.getOrCreate(outputKey));
                java.awt.Point rep = connectivity.representative(root);
                if (rep == null || fixedHigh.containsKey(rep)) {
                    continue;
                }
                Boolean previous = networkValue.put(rep, state.outputHigh);
                if (previous == null || previous.booleanValue() != state.outputHigh) {
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }

        for (LogicGate gate : logicGates) {
            java.awt.Point outputKey = getOutputKey(gate);
            if (outputKey != null) {
                OUTPUT_MEMORY.put(outputKey, gate.isOutputPowered());
            }
        }

        applyNetworkPowerToWires(wires, connectivity, networkValue);
        updateOutputPorts(outputPorts, connectivity, networkValue);
    }
    
    /**
     * Updates a NAND gate based on input voltages and drives output wires.
     */
    private static void updateNANDGate(NANDGate nandGate, Collection<Wire> wires) {
        List<ConnectionPoint> points = nandGate.getConnectionPoints();
        if (points.size() < 3) {
            return;
        }
        
        // Get input voltages from connected wires
        float inputA = getVoltageAtConnection(points.get(0), wires);
        float inputB = getVoltageAtConnection(points.get(1), wires);
        
        // Apply NAND logic: output is LOW only when both inputs are HIGH
        boolean aHigh = inputA >= THRESHOLD_VOLTAGE;
        boolean bHigh = inputB >= THRESHOLD_VOLTAGE;
        float outputVoltage = (aHigh && bHigh) ? LOW_VOLTAGE : HIGH_VOLTAGE;

        nandGate.setInputPowered(0, aHigh);
        nandGate.setInputPowered(1, bHigh);
        nandGate.setOutputPowered(outputVoltage >= THRESHOLD_VOLTAGE);
        
        // Drive output wires with computed voltage
        driveOutputWires(points.get(2), outputVoltage, wires);
    }

    private static void updateANDGate(ANDGate andGate, Collection<Wire> wires) {
        List<ConnectionPoint> points = andGate.getConnectionPoints();
        if (points.size() < 3) {
            return;
        }
        float inputA = getVoltageAtConnection(points.get(0), wires);
        float inputB = getVoltageAtConnection(points.get(1), wires);
        boolean aHigh = inputA >= THRESHOLD_VOLTAGE;
        boolean bHigh = inputB >= THRESHOLD_VOLTAGE;
        float outputVoltage = (aHigh && bHigh) ? HIGH_VOLTAGE : LOW_VOLTAGE;
        andGate.setInputPowered(0, aHigh);
        andGate.setInputPowered(1, bHigh);
        andGate.setOutputPowered(outputVoltage >= THRESHOLD_VOLTAGE);
        driveOutputWires(points.get(2), outputVoltage, wires);
    }

    private static void updateORGate(ORGate orGate, Collection<Wire> wires) {
        List<ConnectionPoint> points = orGate.getConnectionPoints();
        if (points.size() < 3) {
            return;
        }
        float inputA = getVoltageAtConnection(points.get(0), wires);
        float inputB = getVoltageAtConnection(points.get(1), wires);
        boolean aHigh = inputA >= THRESHOLD_VOLTAGE;
        boolean bHigh = inputB >= THRESHOLD_VOLTAGE;
        float outputVoltage = (aHigh || bHigh) ? HIGH_VOLTAGE : LOW_VOLTAGE;
        orGate.setInputPowered(0, aHigh);
        orGate.setInputPowered(1, bHigh);
        orGate.setOutputPowered(outputVoltage >= THRESHOLD_VOLTAGE);
        driveOutputWires(points.get(2), outputVoltage, wires);
    }

    private static void updateXORGate(XORGate xorGate, Collection<Wire> wires) {
        List<ConnectionPoint> points = xorGate.getConnectionPoints();
        if (points.size() < 3) {
            return;
        }
        float inputA = getVoltageAtConnection(points.get(0), wires);
        float inputB = getVoltageAtConnection(points.get(1), wires);
        boolean aHigh = inputA >= THRESHOLD_VOLTAGE;
        boolean bHigh = inputB >= THRESHOLD_VOLTAGE;
        float outputVoltage = (aHigh ^ bHigh) ? HIGH_VOLTAGE : LOW_VOLTAGE;
        xorGate.setInputPowered(0, aHigh);
        xorGate.setInputPowered(1, bHigh);
        xorGate.setOutputPowered(outputVoltage >= THRESHOLD_VOLTAGE);
        driveOutputWires(points.get(2), outputVoltage, wires);
    }

    private static void updateNOTGate(NOTGate notGate, Collection<Wire> wires) {
        List<ConnectionPoint> points = notGate.getConnectionPoints();
        if (points.size() < 2) {
            return;
        }
        float inputA = getVoltageAtConnection(points.get(0), wires);
        boolean aHigh = inputA >= THRESHOLD_VOLTAGE;
        float outputVoltage = aHigh ? LOW_VOLTAGE : HIGH_VOLTAGE;
        notGate.setInputPowered(0, aHigh);
        notGate.setOutputPowered(outputVoltage >= THRESHOLD_VOLTAGE);
        driveOutputWires(points.get(1), outputVoltage, wires);
    }
    
    /**
     * Gets the voltage at a connection point from connected wires.
     */
    private static float getVoltageAtConnection(ConnectionPoint point, Collection<Wire> wires) {
        int pointX = point.getOwner().getConnectionPointWorldX(point);
        int pointY = point.getOwner().getConnectionPointWorldY(point);
        Point snappedPoint = new Point(Grid.snap(pointX), Grid.snap(pointY));
        
        float maxVoltage = 0f;
        boolean hasConnection = false;
        
        for (Wire wire : wires) {
            if (wire.getStart() != null && wire.getEnd() != null) {
                Point start = new Point(Grid.snap(wire.getStart().getX()), 
                                       Grid.snap(wire.getStart().getY()));
                Point end = new Point(Grid.snap(wire.getEnd().getX()), 
                                     Grid.snap(wire.getEnd().getY()));
                
                // Check if wire connects to this point
                if (start.equals(snappedPoint) || end.equals(snappedPoint)) {
                    hasConnection = true;
                    maxVoltage = Math.max(maxVoltage, wire.getComputedVoltage());
                    if (wire.isLogicPowered()) {
                        return HIGH_VOLTAGE;
                    }
                }
            }
        }

        if (!hasConnection) {
            return 0f;
        }
        return maxVoltage;
    }
    
    /**
     * Drives output voltage to all wires connected to the given connection point.
     */
    private static void driveOutputWires(ConnectionPoint outputPoint, float voltage, Collection<Wire> wires) {
        int pointX = outputPoint.getOwner().getConnectionPointWorldX(outputPoint);
        int pointY = outputPoint.getOwner().getConnectionPointWorldY(outputPoint);
        Point snappedPoint = new Point(Grid.snap(pointX), Grid.snap(pointY));
        
        for (Wire wire : wires) {
            if (wire.getStart() != null && wire.getEnd() != null) {
                Point start = new Point(Grid.snap(wire.getStart().getX()), 
                                       Grid.snap(wire.getStart().getY()));
                Point end = new Point(Grid.snap(wire.getEnd().getX()), 
                                     Grid.snap(wire.getEnd().getY()));
                
                // Check if wire connects to this output point
                if (start.equals(snappedPoint) || end.equals(snappedPoint)) {
                    wire.setComputedVoltage(voltage);
                    wire.setComputedAmpere(voltage > THRESHOLD_VOLTAGE ? 0.001f : 0f);
                    wire.setLogicPowered(voltage > THRESHOLD_VOLTAGE);
                }
            }
        }
    }

    private static void seedLogicInputs(Collection<CircuitComponent> components, Collection<Wire> wires) {
        for (CircuitComponent component : components) {
            switch (component) {
                case CustomInputPort input -> {
                    if (input.isActive()) {
                        markWiresPowered(input.getConnectionPoints(), input, wires);
                    }
                }
                case Source source -> {
                    if (source.isActive()) {
                        markWiresPowered(source.getConnectionPoints(), source, wires);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void markWiresPowered(List<ConnectionPoint> points, CircuitComponent owner,
                                         Collection<Wire> wires) {
        if (points.isEmpty()) {
            return;
        }
        ConnectionPoint point = points.get(0);
        int pointX = owner.getConnectionPointWorldX(point);
        int pointY = owner.getConnectionPointWorldY(point);
        Point snappedPoint = new Point(Grid.snap(pointX), Grid.snap(pointY));
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Point start = new Point(Grid.snap(wire.getStart().getX()),
                    Grid.snap(wire.getStart().getY()));
            Point end = new Point(Grid.snap(wire.getEnd().getX()),
                    Grid.snap(wire.getEnd().getY()));
            if (start.equals(snappedPoint) || end.equals(snappedPoint)) {
                wire.setLogicPowered(true);
            }
        }
    }

    private static void updateCustomOutputPorts(Collection<CircuitComponent> components, Collection<Wire> wires) {
        for (CircuitComponent component : components) {
            if (!(component instanceof CustomOutputPort outputPort)) {
                continue;
            }
            List<ConnectionPoint> points = outputPort.getConnectionPoints();
            if (points.isEmpty()) {
                outputPort.setActiveIndicator(false);
                continue;
            }
            ConnectionPoint point = points.get(0);
            boolean powered = isLogicPoweredAtPoint(point, outputPort, wires);
            outputPort.setActiveIndicator(powered);
            if (powered) {
                markWiresPowered(points, outputPort, wires);
            }
        }
    }

    private static boolean isLogicPoweredAtPoint(ConnectionPoint point, CircuitComponent owner,
                                                 Collection<Wire> wires) {
        int pointX = owner.getConnectionPointWorldX(point);
        int pointY = owner.getConnectionPointWorldY(point);
        Point snappedPoint = new Point(Grid.snap(pointX), Grid.snap(pointY));
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Point start = new Point(Grid.snap(wire.getStart().getX()),
                    Grid.snap(wire.getStart().getY()));
            Point end = new Point(Grid.snap(wire.getEnd().getX()),
                    Grid.snap(wire.getEnd().getY()));
            if ((start.equals(snappedPoint) || end.equals(snappedPoint)) && wire.isLogicPowered()) {
                return true;
            }
        }
        return false;
    }

    private static void propagateLogicPower(Collection<Wire> wires) {
        if (wires == null || wires.isEmpty()) {
            return;
        }
        java.util.Map<Point, java.util.List<Wire>> nodeWires = new java.util.HashMap<>();
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Point start = new Point(Grid.snap(wire.getStart().getX()),
                    Grid.snap(wire.getStart().getY()));
            Point end = new Point(Grid.snap(wire.getEnd().getX()),
                    Grid.snap(wire.getEnd().getY()));
            nodeWires.computeIfAbsent(start, key -> new java.util.ArrayList<>()).add(wire);
            nodeWires.computeIfAbsent(end, key -> new java.util.ArrayList<>()).add(wire);
        }
        java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<Point> visited = new java.util.HashSet<>();
        for (Wire wire : wires) {
            if (!wire.isLogicPowered() || wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Point start = new Point(Grid.snap(wire.getStart().getX()),
                    Grid.snap(wire.getStart().getY()));
            Point end = new Point(Grid.snap(wire.getEnd().getX()),
                    Grid.snap(wire.getEnd().getY()));
            if (visited.add(start)) {
                queue.add(start);
            }
            if (visited.add(end)) {
                queue.add(end);
            }
        }
        while (!queue.isEmpty()) {
            Point node = queue.removeFirst();
            java.util.List<Wire> attached = nodeWires.get(node);
            if (attached == null) {
                continue;
            }
            for (Wire wire : attached) {
                if (!wire.isLogicPowered()) {
                    wire.setLogicPowered(true);
                }
                WireNode startNode = wire.getStart();
                WireNode endNode = wire.getEnd();
                if (startNode != null) {
                    Point start = new Point(Grid.snap(startNode.getX()), Grid.snap(startNode.getY()));
                    if (visited.add(start)) {
                        queue.add(start);
                    }
                }
                if (endNode != null) {
                    Point end = new Point(Grid.snap(endNode.getX()), Grid.snap(endNode.getY()));
                    if (visited.add(end)) {
                        queue.add(end);
                    }
                }
            }
        }
    }

    private static final class Seed {
        private final java.awt.Point point;
        private final boolean high;

        private Seed(java.awt.Point point, boolean high) {
            this.point = point;
            this.high = high;
        }
    }

    private static final class GateState {
        private final boolean inputAHigh;
        private final boolean inputBHigh;
        private final boolean outputHigh;

        private GateState(boolean inputAHigh, boolean inputBHigh, boolean outputHigh) {
            this.inputAHigh = inputAHigh;
            this.inputBHigh = inputBHigh;
            this.outputHigh = outputHigh;
        }
    }

    private static GateState evaluateGate(LogicGate gate, Connectivity connectivity,
                                          java.util.Map<java.awt.Point, Boolean> networkHigh) {
        if (gate == null) {
            return new GateState(false, false, false);
        }
        List<ConnectionPoint> points = gate.getConnectionPoints();
        boolean inputA = false;
        boolean inputB = false;
        if (!points.isEmpty()) {
            inputA = isHighAtPoint(gate, points.get(0), connectivity, networkHigh);
            if (points.size() > 1) {
                inputB = isHighAtPoint(gate, points.get(1), connectivity, networkHigh);
            }
        }

        boolean outputHigh;
        switch (gate) {
            case NANDGate ignored -> outputHigh = !(inputA && inputB);
            case ANDGate ignored -> outputHigh = inputA && inputB;
            case ORGate ignored -> outputHigh = inputA || inputB;
            case XORGate ignored -> outputHigh = inputA ^ inputB;
            case NOTGate ignored -> outputHigh = !inputA;
            default -> outputHigh = gate.isOutputPowered();
        }

        // For NOT gates, treat the second input marker as unused.
        boolean effectiveInputB = gate instanceof NOTGate ? false : inputB;
        return new GateState(inputA, effectiveInputB, outputHigh);
    }

    private static boolean isHighAtPoint(CircuitComponent owner, ConnectionPoint point,
                                         Connectivity connectivity,
                                         java.util.Map<java.awt.Point, Boolean> networkHigh) {
        if (owner == null || point == null) {
            return false;
        }
        java.awt.Point snapped = snapPoint(owner.getConnectionPointWorldX(point),
                owner.getConnectionPointWorldY(point));
        int root = connectivity.root(connectivity.getOrCreate(snapped));
        return Boolean.TRUE.equals(networkHigh.get(connectivity.representative(root)));
    }

    private static java.awt.Point getPrimaryPoint(CircuitComponent component) {
        if (component == null || component.getConnectionPoints().isEmpty()) {
            return null;
        }
        ConnectionPoint point = component.getConnectionPoints().get(0);
        return snapPoint(component.getConnectionPointWorldX(point),
                component.getConnectionPointWorldY(point));
    }

    private static java.awt.Point snapPoint(int x, int y) {
        return new java.awt.Point(Grid.snap(x), Grid.snap(y));
    }

    private static java.awt.Point getOutputKey(LogicGate gate) {
        if (gate == null) {
            return null;
        }
        ConnectionPoint output = gate.getOutputPoint();
        if (output == null) {
            return null;
        }
        return snapPoint(gate.getConnectionPointWorldX(output), gate.getConnectionPointWorldY(output));
    }

    private static void applyNetworkPowerToWires(Collection<Wire> wires,
                                                 Connectivity connectivity,
                                                 java.util.Map<java.awt.Point, Boolean> networkHigh) {
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            java.awt.Point start = snapPoint(wire.getStart().getX(), wire.getStart().getY());
            int root = connectivity.root(connectivity.getOrCreate(start));
            boolean high = Boolean.TRUE.equals(networkHigh.get(connectivity.representative(root)));
            wire.setLogicPowered(high);
        }
    }

    private static void updateOutputPorts(java.util.List<CustomOutputPort> outputPorts,
                                          Connectivity connectivity,
                                          java.util.Map<java.awt.Point, Boolean> networkHigh) {
        for (CustomOutputPort outputPort : outputPorts) {
            if (outputPort.getConnectionPoints().isEmpty()) {
                outputPort.setActiveIndicator(false);
                continue;
            }
            ConnectionPoint point = outputPort.getConnectionPoints().get(0);
            boolean high = isHighAtPoint(outputPort, point, connectivity, networkHigh);
            outputPort.setActiveIndicator(high);
        }
    }

    private static final class Connectivity {
        private final java.util.Map<java.awt.Point, Integer> indexByPoint = new java.util.HashMap<>();
        private final java.util.List<java.awt.Point> points = new java.util.ArrayList<>();
        private final java.util.List<Integer> parent = new java.util.ArrayList<>();

        private Connectivity(Collection<Wire> wires) {
            if (wires == null) {
                return;
            }
            for (Wire wire : wires) {
                if (wire.getStart() == null || wire.getEnd() == null) {
                    continue;
                }
                java.awt.Point start = snapPoint(wire.getStart().getX(), wire.getStart().getY());
                java.awt.Point end = snapPoint(wire.getEnd().getX(), wire.getEnd().getY());
                union(getOrCreate(start), getOrCreate(end));
            }
        }

        private int getOrCreate(java.awt.Point point) {
            if (point == null) {
                return -1;
            }
            Integer existing = indexByPoint.get(point);
            if (existing != null) {
                return existing;
            }
            int index = points.size();
            java.awt.Point stored = new java.awt.Point(point);
            points.add(stored);
            indexByPoint.put(stored, index);
            parent.add(index);
            return index;
        }

        private int root(int index) {
            if (index < 0 || index >= parent.size()) {
                return -1;
            }
            int p = parent.get(index);
            if (p == index) {
                return index;
            }
            int r = root(p);
            parent.set(index, r);
            return r;
        }

        private java.awt.Point representative(int root) {
            if (root < 0 || root >= points.size()) {
                return null;
            }
            return points.get(root);
        }

        private void union(int a, int b) {
            int ra = root(a);
            int rb = root(b);
            if (ra < 0 || rb < 0 || ra == rb) {
                return;
            }
            parent.set(rb, ra);
        }
    }
}
