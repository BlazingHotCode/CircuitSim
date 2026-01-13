package circuitsim.components.core;

/**
 * Base class for components with two terminals on the left/right edges.
 */
public abstract class TwoTerminalComponent extends CircuitComponent {
    protected TwoTerminalComponent(int x, int y, int height, int width) {
        this(x, y, height, width, true);
    }

    protected TwoTerminalComponent(int x, int y, int height, int width, boolean includeDefaultProperties) {
        super(x, y, height, width, 2, includeDefaultProperties);
        addConnectionPoint(0f, 0.5f);
        addConnectionPoint(1f, 0.5f);
    }

    /**
     * @return left terminal connection point, or null if missing
     */
    public ConnectionPoint getLeftPoint() {
        return getConnectionPoints().isEmpty() ? null : getConnectionPoints().get(0);
    }

    /**
     * @return right terminal connection point, or null if missing
     */
    public ConnectionPoint getRightPoint() {
        return getConnectionPoints().size() < 2 ? null : getConnectionPoints().get(1);
    }
}
