package circuitsim.components.core;

import circuitsim.components.wiring.Wire;
import java.util.List;

/**
 * MODULAR INTERFACE: Supports all component types with maximum flexibility.
 * SINGLE RESPONSIBILITY: Each method has one clear purpose.
 */
public interface ComponentInterface {
    
    /**
     * EXTENSIBLE: Called before physics simulation starts.
     * MODULAR: Override in components needing pre-simulation setup.
     */
    default void beforeSimulation() {
        // Default implementation - override as needed
    }
    
    /**
     * EXTENSIBLE: Updates this component's state during simulation.
     * MODULAR: Standard interface for all component types.
     * 
     * @param wires collection of wires in circuit
     */
    void updateSimulation(List<Wire> wires);
    
    /**
     * EXTENSIBLE: Called after physics simulation completes.
     * MODULAR: Override in components needing post-simulation cleanup.
     */
    default void afterSimulation() {
        // Default implementation - override as needed
    }
    
    /**
     * MODULAR: Gets number of input connections this component accepts.
     * MODULAR: Override in logic gates to return actual input count.
     * 
     * @return number of input connections
     */
    default int getInputCount() {
        return 0;
    }
    
    /**
     * MODULAR: Gets number of output connections this component provides.
     * MODULAR: Override in logic gates to return actual output count.
     * 
     * @return number of output connections
     */
    default int getOutputCount() {
        return 0;
    }
    
    /**
     * MODULAR: Checks if a connection point is an input.
     * MODULAR: Override in components with custom logic.
     * 
     * @param point connection point to check
     * @return true if this is an input point
     */
    default boolean isInputPoint(ConnectionPoint point) {
        return false;
    }
    
    /**
     * MODULAR: Checks if a connection point is an output.
     * MODULAR: Override in components with custom logic.
     * 
     * @param point connection point to check
     * @return true if this is an output point
     */
    default boolean isOutputPoint(ConnectionPoint point) {
        return false;
    }
    
    /**
     * MODULAR: Checks if this is a logic component with single-wire restrictions.
     * MODULAR: Override in logic components.
     * 
     * @return true if this component restricts input connections
     */
    default boolean isLogicComponent() {
        return false;
    }
    
    /**
     * MODULAR: Checks if a connection index is an input connection.
     * MODULAR: Override in components with custom input selection.
     * 
     * @param connectionIndex index of the connection point
     * @return true if this connection index is an input
     */
    default boolean isInputConnection(int connectionIndex) {
        return false;
    }
    
    /**
     * MODULAR: Checks if this component can be rotated in all directions.
     * MODULAR: Override in components with rotation restrictions.
     * 
     * @return true if full rotation (0-3 quarter turns) is allowed
     */
    default boolean allowFullRotation() {
        return false;
    }
    
    /**
     * MODULAR: Gets the electrical node index for this component's connection point.
     * MODULAR: Used by physics engine for circuit analysis.
     * 
     * @param point connection point to get node for
     * @return electrical node index, or -1 if not applicable
     */
    default int getNodeIndex(ConnectionPoint point) {
        return -1;
    }
    
    /**
     * MODULAR: Sets the electrical node index for this component's connection point.
     * MODULAR: Used by physics engine for circuit analysis.
     * 
     * @param point connection point to set node for
     * @param index electrical node index
     */
    default void setNodeIndex(ConnectionPoint point, int index) {
        // Default implementation - override as needed
    }
    
    /**
     * MODULAR: Gets all electrical node indices for this component.
     * MODULAR: Used by physics engine for circuit analysis.
     * 
     * @return array of node indices for all connection points
     */
    default int[] getAllNodeIndices() {
        int[] indices = new int[getConnectionPoints().size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = -1;
        }
        return indices;
    }
    
    /**
     * MODULAR: Gets the immutable list of connection points.
     * MODULAR: Property getter with proper encapsulation.
     * 
     * @return immutable list of connection points
     */
    List<ConnectionPoint> getConnectionPoints();
}
