package circuitsim.components.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of component types exposed to the UI.
 */
public final class ComponentRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Group> GROUPS = new ArrayList<>();
    private static final List<BuiltinComponentDiscovery.Definition> BUILTINS;
    private static final Map<String, ComponentFactory> TYPE_FACTORIES = new HashMap<>();
    private static final String DEFAULT_GROUP = "General";

    static {
        ensureGroup("Custom");
        BUILTINS = Collections.unmodifiableList(BuiltinComponentDiscovery.discover());
        for (BuiltinComponentDiscovery.Definition builtin : BUILTINS) {
            if (builtin == null) {
                continue;
            }
            registerBuiltin(builtin);
        }
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
        Entry entry = new Entry(groupName, name, null, null, factory);
        ENTRIES.add(entry);
        getOrCreateGroup(groupName).entries.add(entry);
    }
    
    /**
     * Registers a custom component entry under a group.
     */
    public static void registerCustom(String groupName, String name, String customId,
                                      ComponentFactory factory) {
        Entry entry = new Entry(groupName, name, "Custom", customId, factory);
        ENTRIES.add(entry);
        getOrCreateGroup(groupName).entries.add(entry);
    }

    /**
     * Restores the given group entries to the discovered built-ins for that group.
     * Useful when the palette differs between main mode and editor mode.
     */
    public static void restoreGroupToBuiltins(String groupName) {
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
            group = getOrCreateGroup(groupName);
        }
        final String target = groupName;
        ENTRIES.removeIf(entry -> target.equals(entry.groupName));
        group.entries.clear();
        for (BuiltinComponentDiscovery.Definition builtin : BUILTINS) {
            if (builtin == null) {
                continue;
            }
            if (!target.equals(builtin.group())) {
                continue;
            }
            registerBuiltin(builtin);
        }
    }

    /**
     * Creates a built-in component instance by serialized type id.
     * This works even if the component is not currently visible in the palette.
     */
    public static CircuitComponent createBuiltinFromType(String type, int x, int y) {
        if (type == null) {
            return null;
        }
        ComponentFactory factory = TYPE_FACTORIES.get(type);
        if (factory == null) {
            return null;
        }
        return factory.create(x, y);
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
        private final String type;
        private final String customId;
        private final ComponentFactory factory;

        private Entry(String groupName, String name, String type, String customId, ComponentFactory factory) {
            this.groupName = groupName;
            this.name = name;
            this.type = type;
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
         * @return serialized type id for this entry, or null when not applicable
         */
        public String getType() {
            return type;
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

    private static void registerBuiltin(BuiltinComponentDiscovery.Definition builtin) {
        if (builtin == null) {
            return;
        }
        // Always install type factories for deserialization, even if not palette-visible.
        TYPE_FACTORIES.putIfAbsent(builtin.type, builtin.factory);
        for (String alias : builtin.aliases) {
            TYPE_FACTORIES.putIfAbsent(alias, builtin.factory);
        }
        if (!builtin.paletteVisible) {
            return;
        }
        Entry entry = new Entry(builtin.group, builtin.paletteName, builtin.type, null, builtin.factory);
        ENTRIES.add(entry);
        getOrCreateGroup(builtin.group).entries.add(entry);
    }
}
