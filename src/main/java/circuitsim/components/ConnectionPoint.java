package circuitsim.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a connection point on a circuit component.
 */
public class ConnectionPoint {
    private final CircuitComponent owner;
    private final float relativeX;
    private final float relativeY;
    private final List<ConnectionPoint> connectedPoints = new ArrayList<>();

    /**
     * @param owner owning component
     * @param relativeX normalized X position (0..1)
     * @param relativeY normalized Y position (0..1)
     */
    public ConnectionPoint(CircuitComponent owner, float relativeX, float relativeY) {
        this.owner = owner;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    /**
     * @return owning component
     */
    public CircuitComponent getOwner() {
        return owner;
    }

    /**
     * @return normalized X position
     */
    public float getRelativeX() {
        return relativeX;
    }

    /**
     * @return normalized Y position
     */
    public float getRelativeY() {
        return relativeY;
    }

    /**
     * @return world-space X coordinate
     */
    public int getX() {
        return owner.getX() + Math.round(owner.getWidth() * relativeX);
    }

    /**
     * @return world-space Y coordinate
     */
    public int getY() {
        return owner.getY() + Math.round(owner.getHeight() * relativeY);
    }

    /**
     * @return first connected point, or null if none
     */
    public ConnectionPoint getConnectedPoint() {
        return connectedPoints.isEmpty() ? null : connectedPoints.get(0);
    }

    /**
     * @return all connected points
     */
    public List<ConnectionPoint> getConnectedPoints() {
        return Collections.unmodifiableList(connectedPoints);
    }

    /**
     * @return true if any connection exists
     */
    public boolean isConnected() {
        return !connectedPoints.isEmpty();
    }

    /**
     * Adds a connection if it's not already present.
     *
     * @param point point to connect
     */
    public void connect(ConnectionPoint point) {
        if (point != null && point != this && !connectedPoints.contains(point)) {
            connectedPoints.add(point);
        }
    }

    /**
     * Removes all connections.
     */
    public void disconnect() {
        connectedPoints.clear();
    }

    /**
     * Removes a specific connection.
     *
     * @param point point to disconnect
     */
    public void disconnect(ConnectionPoint point) {
        connectedPoints.remove(point);
    }
}
