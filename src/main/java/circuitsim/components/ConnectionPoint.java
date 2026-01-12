package circuitsim.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionPoint {
    private final CircuitComponent owner;
    private final float relativeX;
    private final float relativeY;
    private final List<ConnectionPoint> connectedPoints = new ArrayList<>();

    public ConnectionPoint(CircuitComponent owner, float relativeX, float relativeY) {
        this.owner = owner;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    public CircuitComponent getOwner() {
        return owner;
    }

    public float getRelativeX() {
        return relativeX;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public int getX() {
        return owner.getX() + Math.round(owner.getWidth() * relativeX);
    }

    public int getY() {
        return owner.getY() + Math.round(owner.getHeight() * relativeY);
    }

    public ConnectionPoint getConnectedPoint() {
        return connectedPoints.isEmpty() ? null : connectedPoints.get(0);
    }

    public List<ConnectionPoint> getConnectedPoints() {
        return Collections.unmodifiableList(connectedPoints);
    }

    public boolean isConnected() {
        return !connectedPoints.isEmpty();
    }

    public void connect(ConnectionPoint point) {
        if (point != null && point != this && !connectedPoints.contains(point)) {
            connectedPoints.add(point);
        }
    }

    public void disconnect() {
        connectedPoints.clear();
    }

    public void disconnect(ConnectionPoint point) {
        connectedPoints.remove(point);
    }
}
