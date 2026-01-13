package circuitsim.components.core;

/**
 * Shared interface for components that behave like switches.
 */
public interface SwitchLike {
    /**
     * @return true when the switch is closed
     */
    boolean isClosed();

    /**
     * Sets computed current for display.
     */
    void setComputedAmpere(float computedAmpere);
}
