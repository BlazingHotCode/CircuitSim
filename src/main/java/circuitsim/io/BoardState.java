package circuitsim.io;

import circuitsim.components.WireColor;
import circuitsim.custom.CustomComponentDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializable snapshot of the circuit board state.
 */
public final class BoardState {
    /**
     * Current schema version.
     */
    public static final int CURRENT_VERSION = 1;

    private final int version;
    private final WireColor activeWireColor;
    private final List<ComponentState> components;
    private final List<WireState> wires;
    private final List<CustomComponentDefinition> customComponents;

    /**
     * @param version schema version
     * @param activeWireColor active wire color
     * @param components component states
     * @param wires wire states
     */
    public BoardState(int version, WireColor activeWireColor,
                      List<ComponentState> components, List<WireState> wires) {
        this(version, activeWireColor, components, wires, Collections.emptyList());
    }

    /**
     * @param version schema version
     * @param activeWireColor active wire color
     * @param components component states
     * @param wires wire states
     * @param customComponents embedded custom component definitions
     */
    public BoardState(int version, WireColor activeWireColor,
                      List<ComponentState> components, List<WireState> wires,
                      List<CustomComponentDefinition> customComponents) {
        this.version = version;
        this.activeWireColor = activeWireColor == null ? WireColor.WHITE : activeWireColor;
        this.components = Collections.unmodifiableList(new ArrayList<>(components == null
                ? Collections.emptyList()
                : components));
        this.wires = Collections.unmodifiableList(new ArrayList<>(wires == null
                ? Collections.emptyList()
                : wires));
        this.customComponents = Collections.unmodifiableList(new ArrayList<>(customComponents == null
                ? Collections.emptyList()
                : customComponents));
    }

    /**
     * @return schema version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return active wire color
     */
    public WireColor getActiveWireColor() {
        return activeWireColor;
    }

    /**
     * @return immutable list of component states
     */
    public List<ComponentState> getComponents() {
        return components;
    }

    /**
     * @return immutable list of wire states
     */
    public List<WireState> getWires() {
        return wires;
    }

    /**
     * @return embedded custom component definitions
     */
    public List<CustomComponentDefinition> getCustomComponents() {
        return customComponents;
    }

    /**
     * Serialized component metadata.
     */
    public static final class ComponentState {
        private final String type;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int rotationQuarterTurns;
        private final String displayName;
        private final String customId;
        private final boolean showTitle;
        private final boolean showValues;
        private final Float voltage;
        private final Float internalResistance;
        private final Float resistance;
        private final Boolean closed;

        /**
         * @param type component type name
         * @param x left position
         * @param y top position
         * @param width width in pixels
         * @param height height in pixels
         * @param rotationQuarterTurns rotation in quarter turns
         * @param displayName display name
         * @param showTitle show title flag
         * @param showValues show values flag
         * @param voltage battery voltage (optional)
         * @param internalResistance battery internal resistance (optional)
         * @param resistance resistor resistance (optional)
         * @param closed switch closed state (optional)
         */
        public ComponentState(String type, int x, int y, int width, int height, int rotationQuarterTurns,
                              String displayName, boolean showTitle, boolean showValues,
                              Float voltage, Float internalResistance, Float resistance, Boolean closed) {
            this(type, x, y, width, height, rotationQuarterTurns, displayName, null,
                    showTitle, showValues, voltage, internalResistance, resistance, closed);
        }

        /**
         * @param type component type name
         * @param x left position
         * @param y top position
         * @param width width in pixels
         * @param height height in pixels
         * @param rotationQuarterTurns rotation in quarter turns
         * @param displayName display name
         * @param customId custom component id (optional)
         * @param showTitle show title flag
         * @param showValues show values flag
         * @param voltage battery voltage (optional)
         * @param internalResistance battery internal resistance (optional)
         * @param resistance resistor resistance (optional)
         * @param closed switch closed state (optional)
         */
        public ComponentState(String type, int x, int y, int width, int height, int rotationQuarterTurns,
                              String displayName, String customId, boolean showTitle, boolean showValues,
                              Float voltage, Float internalResistance, Float resistance, Boolean closed) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rotationQuarterTurns = rotationQuarterTurns;
            this.displayName = displayName;
            this.customId = customId;
            this.showTitle = showTitle;
            this.showValues = showValues;
            this.voltage = voltage;
            this.internalResistance = internalResistance;
            this.resistance = resistance;
            this.closed = closed;
        }

        /**
         * @return component type name
         */
        public String getType() {
            return type;
        }

        /**
         * @return left position
         */
        public int getX() {
            return x;
        }

        /**
         * @return top position
         */
        public int getY() {
            return y;
        }

        /**
         * @return width in pixels
         */
        public int getWidth() {
            return width;
        }

        /**
         * @return height in pixels
         */
        public int getHeight() {
            return height;
        }

        /**
         * @return rotation in quarter turns
         */
        public int getRotationQuarterTurns() {
            return rotationQuarterTurns;
        }

        /**
         * @return display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return custom component id, if any
         */
        public String getCustomId() {
            return customId;
        }

        /**
         * @return true if title should be shown
         */
        public boolean isShowTitle() {
            return showTitle;
        }

        /**
         * @return true if values should be shown
         */
        public boolean isShowValues() {
            return showValues;
        }

        /**
         * @return battery voltage, if applicable
         */
        public Float getVoltage() {
            return voltage;
        }

        /**
         * @return battery internal resistance, if applicable
         */
        public Float getInternalResistance() {
            return internalResistance;
        }

        /**
         * @return resistor resistance, if applicable
         */
        public Float getResistance() {
            return resistance;
        }

        /**
         * @return switch closed state, if applicable
         */
        public Boolean getClosed() {
            return closed;
        }
    }

    /**
     * Serialized wire metadata.
     */
    public static final class WireState {
        private final int startX;
        private final int startY;
        private final int endX;
        private final int endY;
        private final WireColor color;
        private final boolean showData;

        /**
         * @param startX start X coordinate
         * @param startY start Y coordinate
         * @param endX end X coordinate
         * @param endY end Y coordinate
         * @param color wire color
         * @param showData show data label flag
         */
        public WireState(int startX, int startY, int endX, int endY, WireColor color, boolean showData) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color == null ? WireColor.WHITE : color;
            this.showData = showData;
        }

        /**
         * @return start X coordinate
         */
        public int getStartX() {
            return startX;
        }

        /**
         * @return start Y coordinate
         */
        public int getStartY() {
            return startY;
        }

        /**
         * @return end X coordinate
         */
        public int getEndX() {
            return endX;
        }

        /**
         * @return end Y coordinate
         */
        public int getEndY() {
            return endY;
        }

        /**
         * @return wire color
         */
        public WireColor getColor() {
            return color;
        }

        /**
         * @return true if data labels are shown
         */
        public boolean isShowData() {
            return showData;
        }
    }
}
