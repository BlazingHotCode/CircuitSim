package circuitsim.components.logic;

import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.wiring.Wire;
import circuitsim.physics.LogicPhysics.LogicGateInputs;
import java.util.Collection;

/**
 * Interface for all logic gates to ensure consistent behavior.
 * Makes the system easily extensible for new logic gate types.
 */
public interface LogicGateInterface {
    
    /**
     * Reads input voltages from connected wires.
     * @param gates component to read inputs from
     * @param wires collection of wires in circuit
     * @return object containing all input voltages
     */
    LogicGateInputs readInputs(CircuitComponent gate, Collection<Wire> wires);
    
    /**
     * Evaluates the gate's logic function.
     * @param inputs the input voltages to evaluate
     * @return the computed output voltage (HIGH or LOW)
     */
    float evaluateLogic(LogicGateInputs inputs);
    
    /**
     * Gets the output connection point of the logic gate.
     * @return output connection point, or null if not found
     */
    ConnectionPoint getOutputPoint();
    
    /**
     * Gets the number of input connections this gate accepts.
     * @return number of inputs
     */
    int getInputCount();
    
    /**
     * Gets the number of output connections this gate provides.
     * @return number of outputs  
     */
    int getOutputCount();
}
