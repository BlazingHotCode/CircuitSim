package circuitsim.components.core;

import circuitsim.components.electrical.Battery;
import circuitsim.components.electrical.Ground;
import circuitsim.components.electrical.Resistor;
import circuitsim.components.electrical.Source;
import circuitsim.components.electrical.Switch;
import circuitsim.components.instruments.Ammeter;
import circuitsim.components.instruments.Voltmeter;
import circuitsim.components.logic.LogicGateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of component types exposed to the UI.
 */
public final class ComponentRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Group> GROUPS = new ArrayList<>();
    private static final String DEFAULT_GROUP = "General";

    static {
        ensureGroup("Custom");
        register("Sources", "Battery", (x, y) -> new Battery(x, y));
        register("Sources", "Source", (x, y) -> new Source(x, y));
        registerLogicGate("Logic", "NAND", "NAND");
        registerLogicGate("Logic", "AND", "AND");
        registerLogicGate("Logic", "OR", "OR");
        registerLogicGate("Logic", "XOR", "XOR");
        registerLogicGate("Logic", "NOT", "NOT");
        register("Passive", "Resistor", (x, y) -> new Resistor(x, y));
        register("Meters", "Voltmeter", (x, y) -> new Voltmeter(x, y));
        register("Meters", "Ammeter", (x, y) -> new Ammeter(x, y));
        register("Controls", "Switch (User)", (x, y) -> new Switch(x, y));
        register("Reference", "Ground", (x, y) -> new Ground(x, y));
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
        register(DEFAULT_GROUP, name, factory);
    }

    /**
     * Registers a new component type under a group.
     *
     * @param groupName group label shown in the UI
     * @param name label shown in the UI
     * @param factory factory for creating components
     */
    public static void register(String groupName, String name, ComponentFactory factory) {
        Entry entry = new Entry(groupName, name, null, factory);
        ENTRIES.add(entry);
        getOrCreateGroup(groupName).entries.add(entry);
    }
    
    /**
     * Registers a logic gate with the factory system.
     * EXTENSIBLE: Add new logic gate types through factory.
     *
     * @param groupName group label shown in the UI
     * @param name label shown in the UI  
     * @param gateType logic gate type name
     */
    public static void registerLogicGate(String groupName, String name, String gateType) {
        Entry entry = new Entry(groupName, name, null, (x, y) -> LogicGateFactory.createGate(gateType, x, y));
        ENTRIES.add(entry);
        getOrCreateGroup(groupName).entries.add(entry);
    }

    /**
     * Registers a custom component entry under a group.
     */
    public static void registerCustom(String groupName, String name, String customId,
                                      ComponentFactory factory) {
        Entry entry = new Entry(groupName, name, customId, factory);
        ENTRIES.add(entry);
        getOrCreateGroup(groupName).entries.add(entry);
    }

    /**
     * Ensures a group exists without registering entries.
     */
    public static void ensureGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        getOrCreateGroup(groupName);
    }

    /**
     * Clears all entries for the provided group name.
     */
    public static void clearGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        Group group = null;
        for (Group existing : GROUPS) {
            if (existing.name.equals(groupName)) {
                group = existing;
                break;
            }
        }
        if (group == null) {
            return;
        }
        ENTRIES.removeIf(entry -> entry.groupName.equals(groupName));
        group.entries.clear();
    }

    /**
     * Removes a group and all of its entries.
     */
    public static void removeGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        Group group = null;
        for (Group existing : GROUPS) {
            if (existing.name.equals(groupName)) {
                group = existing;
                break;
            }
        }
        if (group == null) {
            return;
        }
        ENTRIES.removeIf(entry -> entry.groupName.equals(groupName));
        GROUPS.remove(group);
    }

    /**
     * @return immutable list of registered entries
     */
    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * @return immutable list of component groups
     */
    public static List<Group> getGroups() {
        return Collections.unmodifiableList(GROUPS);
    }

    private static Group getOrCreateGroup(String groupName) {
        for (Group group : GROUPS) {
            if (group.name.equals(groupName)) {
                return group;
            }
        }
        Group group = new Group(groupName);
        GROUPS.add(group);
        return group;
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
        private final String groupName;
        private final String name;
        private final String customId;
        private final ComponentFactory factory;

        private Entry(String groupName, String name, String customId, ComponentFactory factory) {
            this.groupName = groupName;
            this.name = name;
            this.customId = customId;
            this.factory = factory;
        }

        /**
         * @return group name used in the UI
         */
        public String getGroupName() {
            return groupName;
        }

        /**
         * @return display name used in the UI
         */
        public String getName() {
            return name;
        }

        /**
         * @return custom component id, or null for built-ins
         */
        public String getCustomId() {
            return customId;
        }

        /**
         * @return true if this entry represents a custom component
         */
        public boolean isCustom() {
            return customId != null;
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

    /**
     * Group of component entries for the palette UI.
     */
    public static final class Group {
        private final String name;
        private final List<Entry> entries = new ArrayList<>();

        private Group(String name) {
            this.name = name;
        }

        /**
         * @return group display name
         */
        public String getName() {
            return name;
        }

        /**
         * @return immutable list of entries in this group
         */
        public List<Entry> getEntries() {
            return Collections.unmodifiableList(entries);
        }
    }
}
