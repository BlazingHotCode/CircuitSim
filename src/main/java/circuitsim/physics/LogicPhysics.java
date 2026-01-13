package circuitsim.physics;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.electrical.Source;
import circuitsim.components.logic.ANDGate;
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
        for (Wire wire : wires) {
            wire.setLogicPowered(false);
        }
        seedLogicInputs(components, wires);
        // Ensure gate inputs can read logic state through multi-segment wire networks
        // (including custom-component expansion wires) before evaluating gates.
        propagateLogicPower(wires);
        for (CircuitComponent component : components) {
            if (component instanceof NANDGate) {
                updateNANDGate((NANDGate) component, wires);
            } else if (component instanceof ANDGate) {
                updateANDGate((ANDGate) component, wires);
            } else if (component instanceof ORGate) {
                updateORGate((ORGate) component, wires);
            } else if (component instanceof XORGate) {
                updateXORGate((XORGate) component, wires);
            } else if (component instanceof NOTGate) {
                updateNOTGate((NOTGate) component, wires);
            }
        }
        // Spread any newly-driven outputs through connected networks.
        propagateLogicPower(wires);
        updateCustomOutputPorts(components, wires);
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
        boolean hasCurrent = false;
        
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
                    if (wire.getComputedAmpere() > 0.0001f) {
                        hasCurrent = true;
                    }
                }
            }
        }

        if (!hasConnection) {
            return 0f;
        }
        if (hasCurrent) {
            return HIGH_VOLTAGE;
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
            if (component instanceof CustomInputPort) {
                CustomInputPort input = (CustomInputPort) component;
                if (input.isActive()) {
                    markWiresPowered(input.getConnectionPoints(), input, wires);
                }
            } else if (component instanceof Source) {
                Source source = (Source) component;
                if (source.isActive()) {
                    markWiresPowered(source.getConnectionPoints(), source, wires);
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
            if (!(component instanceof CustomOutputPort)) {
                continue;
            }
            CustomOutputPort outputPort = (CustomOutputPort) component;
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
}
