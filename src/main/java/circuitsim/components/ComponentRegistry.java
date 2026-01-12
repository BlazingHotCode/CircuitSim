package circuitsim.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of component types exposed to the UI.
 */
public final class ComponentRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        register("Battery", (x, y) -> new Battery(x, y));
        register("Resistor", (x, y) -> new Resistor(x, y));
        register("Voltmeter", (x, y) -> new Voltmeter(x, y));
        register("Ammeter", (x, y) -> new Ammeter(x, y));
        register("Switch (User)", (x, y) -> new Switch(x, y));
        register("Ground", (x, y) -> new Ground(x, y));
    }

    private ComponentRegistry() {
    }

    /**
     * Registers a new component type.
     *
     * @param name label shown in the UI
     * @param factory factory for creating components
     */
    public static void register(String name, ComponentFactory factory) {
        ENTRIES.add(new Entry(name, factory));
    }

    /**
     * @return immutable list of registered entries
     */
    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * Creates components at a world position.
     */
    public interface ComponentFactory {
        /**
         * @param x world X coordinate
         * @param y world Y coordinate
         * @return new component instance
         */
        CircuitComponent create(int x, int y);
    }

    /**
     * Registry entry with a display name and factory.
     */
    public static final class Entry {
        private final String name;
        private final ComponentFactory factory;

        private Entry(String name, ComponentFactory factory) {
            this.name = name;
            this.factory = factory;
        }

        /**
         * @return display name used in the UI
         */
        public String getName() {
            return name;
        }

        /**
         * @param x world X coordinate
         * @param y world Y coordinate
         * @return new component instance
         */
        public CircuitComponent create(int x, int y) {
            return factory.create(x, y);
        }
    }
}
