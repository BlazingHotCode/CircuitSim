package circuitsim.components.logic;

import circuitsim.components.core.*;
import circuitsim.components.properties.*;
import circuitsim.components.wiring.*;

import circuitsim.ui.Grid;

/**
 * Factory for creating logic gates.
 * Makes it easy to add new logic gate types to the system.
 * EXTENSIBLE: Just add new gate types and factory methods here.
 */
public final class LogicGateFactory {
    
    private LogicGateFactory() {
    }
    
    /**
     * Creates a logic gate by type name.
     * EXTENSIBLE: Add new gate types here.
     * 
     * @param gateType name of logic gate (e.g., "NAND", "AND", "OR")
     * @param x world X coordinate
     * @param y world Y coordinate
     * @return new logic gate instance, or null if type not found
     */
    public static CircuitComponent createGate(String gateType, int x, int y) {
        if (gateType == null || gateType.trim().isEmpty()) {
            return null;
        }
        
        switch (gateType.toUpperCase()) {
            case "NAND":
                return new NANDGate(x, y);
            case "AND":
                return new ANDGate(x, y);
            case "OR":
                return new ORGate(x, y);
            case "XOR":
                return new XORGate(x, y);
            case "NOT":
                return new NOTGate(x, y);
            // Add new logic gate types here:
            // case "AND":
            //     return new ANDGate(x, y);
            // case "OR":
            //     return new ORGate(x, y);
            // case "NOT":
            //     return new NOTGate(x, y);
            // case "XOR":
            //     return new XORGate(x, y);
            // case "NOR":
            //     return new NORGate(x, y);
            // case "XNOR":
            //     return new XNORGate(x, y);
            default:
                return null;
        }
    }
    
    /**
     * Gets the size for logic gates (consistent sizing).
     * MODULAR: Change in one place to affect all gates.
     * 
     * @return default width for logic gates
     */
    public static int getDefaultWidth() {
        return Grid.SIZE * 3;
    }
    
    /**
     * Gets the size for logic gates (consistent sizing).
     * MODULAR: Change in one place to affect all gates.
     * 
     * @return default height for logic gates
     */
    public static int getDefaultHeight() {
        return Grid.SIZE * 3;
    }
    
    /**
     * Gets the number of connections for logic gates.
     * MODULAR: Configure connection patterns here.
     * 
     * @return default number of connection points
     */
    public static int getDefaultConnectionCount() {
        return 3; // Most logic gates have 2 inputs + 1 output
    }
    
    /**
     * Checks if a gate type is a logic gate.
     * EXTENSIBLE: Add new logic gate types to this list.
     * 
     * @param gateType name of gate type to check
     * @return true if this is a logic gate type
     */
    public static boolean isLogicGateType(String gateType) {
        if (gateType == null) {
            return false;
        }
        
        switch (gateType.toUpperCase()) {
            case "NAND":
                return true;
            case "AND":
            case "OR":
            case "XOR":
            case "NOT":
                return true;
            // Add new logic gate types here:
            // case "AND":
            //     return true;
            // case "OR":
            //     return true;
            // case "NOT":
            //     return true;
            // case "XOR":
            //     return true;
            // case "NOR":
            //     return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets all available logic gate types.
     * EXTENSIBLE: New gate types automatically included.
     * 
     * @return array of available logic gate type names
     */
    public static String[] getAvailableGateTypes() {
        return new String[]{
            "NAND",
            "AND",
            "OR",
            "XOR",
            "NOT"
            // Add new logic gate types here:
            // ,"AND","OR","NOT","XOR","NOR","XNOR"
        };
    }
}
