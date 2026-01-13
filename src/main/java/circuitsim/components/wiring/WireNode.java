package circuitsim.components.wiring;

import circuitsim.components.core.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a wire endpoint that can be shared by multiple wires.
 */
public class WireNode {
    private int x;
    private int y;
    private final Set<Wire> wires = new HashSet<>();

    /**
     * @param x world X coordinate
     * @param y world Y coordinate
     */
    public WireNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return world X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * @return world Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Updates the node position.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Adds a wire reference.
     */
    public void addWire(Wire wire) {
        wires.add(wire);
    }

    /**
     * Removes a wire reference.
     */
    public void removeWire(Wire wire) {
        wires.remove(wire);
    }

    /**
     * @return number of attached wires
     */
    public int getWireCount() {
        return wires.size();
    }

    /**
     * @return immutable set of attached wires
     */
    public Set<Wire> getWires() {
        return Collections.unmodifiableSet(wires);
    }
}
