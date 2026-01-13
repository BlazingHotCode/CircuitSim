package circuitsim.components.logic;

import circuitsim.components.core.*;
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

        return switch (gateType.toUpperCase()) {
            case "NAND" -> new NANDGate(x, y);
            case "AND" -> new ANDGate(x, y);
            case "OR" -> new ORGate(x, y);
            case "XOR" -> new XORGate(x, y);
            case "NOT" -> new NOTGate(x, y);
            // Add new logic gate types here:
            // case "NOR" -> new NORGate(x, y);
            // case "XNOR" -> new XNORGate(x, y);
            default -> null;
        };
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

        return switch (gateType.toUpperCase()) {
            case "NAND", "AND", "OR", "XOR", "NOT" -> true;
            // Add new logic gate types here:
            // case "NOR" -> true;
            default -> false;
        };
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
