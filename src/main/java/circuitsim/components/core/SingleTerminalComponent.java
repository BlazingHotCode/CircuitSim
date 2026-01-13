package circuitsim.components.core;

/**
 * Base class for components with a single terminal.
 */
public abstract class SingleTerminalComponent extends CircuitComponent {
    protected SingleTerminalComponent(int x, int y, int height, int width, float relativeX, float relativeY) {
        this(x, y, height, width, relativeX, relativeY, true);
    }

    protected SingleTerminalComponent(int x, int y, int height, int width, float relativeX, float relativeY,
                                      boolean includeDefaultProperties) {
        super(x, y, height, width, 1, includeDefaultProperties);
        addConnectionPoint(relativeX, relativeY);
    }

    /**
     * @return the single terminal connection point, or null if missing
     */
    public ConnectionPoint getTerminal() {
        return getConnectionPoints().isEmpty() ? null : getConnectionPoints().get(0);
    }
}
