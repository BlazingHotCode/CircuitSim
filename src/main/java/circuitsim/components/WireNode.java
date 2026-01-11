package circuitsim.components;

import java.util.HashSet;
import java.util.Set;

public class WireNode {
    private int x;
    private int y;
    private final Set<Wire> wires = new HashSet<>();

    public WireNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void addWire(Wire wire) {
        wires.add(wire);
    }

    public void removeWire(Wire wire) {
        wires.remove(wire);
    }

    public int getWireCount() {
        return wires.size();
    }
}
