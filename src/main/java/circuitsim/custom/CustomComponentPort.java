package circuitsim.custom;

/**
 * Describes a named input/output port for a custom component.
 */
public final class CustomComponentPort {
    public enum Direction {
        INPUT,
        OUTPUT
    }

    private final String name;
    private final Direction direction;

    public CustomComponentPort(String name, Direction direction) {
        this.name = name == null ? "" : name.trim();
        this.direction = direction == null ? Direction.INPUT : direction;
    }

    /**
     * @return port display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return port direction
     */
    public Direction getDirection() {
        return direction;
    }
}
