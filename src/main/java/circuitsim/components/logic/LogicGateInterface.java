package circuitsim.components.logic;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.components.wiring.*;

import circuitsim.physics.LogicPhysics.LogicGateInputs;

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
    LogicGateInputs readInputs(circuitsim.components.core.CircuitComponent gate, java.util.Collection<circuitsim.components.wiring.Wire> wires);
    
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
    circuitsim.components.core.ConnectionPoint getOutputPoint();
    
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