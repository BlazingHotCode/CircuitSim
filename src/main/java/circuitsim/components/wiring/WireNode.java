package circuitsim.components.wiring;

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
    private Long attachedComponentId;
    private Integer attachedConnectionIndex;

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
     * Attaches this node to a component connection point so it follows when the component moves.
     */
    public void attachToComponent(long componentId, int connectionIndex) {
        this.attachedComponentId = componentId;
        this.attachedConnectionIndex = connectionIndex;
    }

    /**
     * Clears any component attachment.
     */
    public void detachFromComponent() {
        this.attachedComponentId = null;
        this.attachedConnectionIndex = null;
    }

    /**
     * @return true if this node is attached to a component point
     */
    public boolean isAttachedToComponent() {
        return attachedComponentId != null && attachedConnectionIndex != null;
    }

    /**
     * @return attached component id, or null
     */
    public Long getAttachedComponentId() {
        return attachedComponentId;
    }

    /**
     * @return attached connection point index, or null
     */
    public Integer getAttachedConnectionIndex() {
        return attachedConnectionIndex;
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
