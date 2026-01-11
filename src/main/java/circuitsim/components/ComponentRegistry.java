package circuitsim.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComponentRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        register("Battery", (x, y) -> new Battery(x, y));
        register("Resistor", (x, y) -> new Resistor(x, y));
    }

    private ComponentRegistry() {
    }

    public static void register(String name, ComponentFactory factory) {
        ENTRIES.add(new Entry(name, factory));
    }

    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public interface ComponentFactory {
        CircuitComponent create(int x, int y);
    }

    public static final class Entry {
        private final String name;
        private final ComponentFactory factory;

        private Entry(String name, ComponentFactory factory) {
            this.name = name;
            this.factory = factory;
        }

        public String getName() {
            return name;
        }

        public CircuitComponent create(int x, int y) {
            return factory.create(x, y);
        }
    }
}
