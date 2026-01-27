package circuitsim.io;

import circuitsim.components.wiring.WireColor;
import circuitsim.custom.CustomComponentDefinition;
import circuitsim.custom.CustomComponentPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON serializer and parser for {@link BoardState}.
 */
public final class BoardStateIO {
    /**
     * Prevent instantiation.
     */
    private BoardStateIO() {
    }

    /**
     * Serializes the board state to JSON.
     *
     * @param state board state
     * @return JSON string
     */
    public static String toJson(BoardState state) {
        if (state == null) {
            return "{}";
        }
        StringBuilder out = new StringBuilder(1024);
        out.append('{');
        boolean[] first = new boolean[] { true };
        appendNumberField(out, first, "version", state.getVersion());
        appendStringField(out, first, "activeWireColor", state.getActiveWireColor().name());
        appendCustomComponents(out, first, state.getCustomComponents());
        appendComponents(out, first, state.getComponents());
        appendWires(out, first, state.getWires());
        out.append('}');
        return out.toString();
    }

    /**
     * Parses JSON into a {@link BoardState}.
     *
     * @param json JSON string
     * @return parsed board state
     */
    public static BoardState fromJson(String json) {
        JsonParser parser = new JsonParser(json == null ? "" : json);
        Object parsed = parser.parseValue();
        if (!(parsed instanceof Map)) {
            return new BoardState(BoardState.CURRENT_VERSION, WireColor.WHITE,
                    new ArrayList<>(), new ArrayList<>());
        }
        Map<String, Object> root = castObject(parsed);
        int version = getInt(root, "version", BoardState.CURRENT_VERSION);
        WireColor activeWireColor = parseWireColor(getString(root, "activeWireColor", WireColor.WHITE.name()));
        List<CustomComponentDefinition> customComponents = parseCustomComponents(root.get("customComponents"));
        List<BoardState.ComponentState> components = parseComponents(root.get("components"));
        List<BoardState.WireState> wires = parseWires(root.get("wires"));
        return new BoardState(version, activeWireColor, components, wires, customComponents);
    }

    /**
     * Appends the components array to the JSON output.
     */
    private static void appendComponents(StringBuilder out, boolean[] first,
                                         List<BoardState.ComponentState> components) {
        appendFieldStart(out, first, "components");
        out.append('[');
        boolean[] firstItem = new boolean[] { true };
        for (BoardState.ComponentState component : components) {
            appendSeparator(out, firstItem);
            out.append('{');
            boolean[] firstField = new boolean[] { true };
            appendStringField(out, firstField, "type", component.getType());
            appendNumberField(out, firstField, "x", component.getX());
            appendNumberField(out, firstField, "y", component.getY());
            appendNumberField(out, firstField, "width", component.getWidth());
            appendNumberField(out, firstField, "height", component.getHeight());
            appendNumberField(out, firstField, "rotationQuarterTurns", component.getRotationQuarterTurns());
            appendStringField(out, firstField, "displayName", component.getDisplayName());
            appendOptionalStringField(out, firstField, "customId", component.getCustomId());
            appendBooleanField(out, firstField, "showTitle", component.isShowTitle());
            appendBooleanField(out, firstField, "showValues", component.isShowValues());
            appendOptionalNumberField(out, firstField, "voltage", component.getVoltage());
            appendOptionalNumberField(out, firstField, "internalResistance", component.getInternalResistance());
            appendOptionalNumberField(out, firstField, "resistance", component.getResistance());
            appendOptionalNumberField(out, firstField, "powerWatt", component.getPowerWatt());
            appendOptionalBooleanField(out, firstField, "closed", component.getClosed());
            out.append('}');
        }
        out.append(']');
    }

    /**
     * Appends custom component definitions to the JSON output.
     */
    private static void appendCustomComponents(StringBuilder out, boolean[] first,
                                               List<CustomComponentDefinition> customComponents) {
        appendFieldStart(out, first, "customComponents");
        out.append('[');
        boolean[] firstItem = new boolean[] { true };
        for (CustomComponentDefinition definition : customComponents) {
            appendSeparator(out, firstItem);
            out.append('{');
            boolean[] firstField = new boolean[] { true };
            appendStringField(out, firstField, "id", definition.getId());
            appendStringField(out, firstField, "name", definition.getName());
            appendPorts(out, firstField, "inputs", definition.getInputs());
            appendPorts(out, firstField, "outputs", definition.getOutputs());
            BoardState boardState = definition.getBoardState();
            String boardJson = boardState == null ? "{}" : toJson(boardState);
            appendStringField(out, firstField, "boardJson", boardJson);
            out.append('}');
        }
        out.append(']');
    }

    /**
     * Appends port definitions to a JSON array.
     */
    private static void appendPorts(StringBuilder out, boolean[] first, String name,
                                    List<CustomComponentPort> ports) {
        appendFieldStart(out, first, name);
        out.append('[');
        boolean[] firstItem = new boolean[] { true };
        for (CustomComponentPort port : ports) {
            appendSeparator(out, firstItem);
            out.append('{');
            boolean[] firstField = new boolean[] { true };
            appendStringField(out, firstField, "name", port.getName());
            out.append('}');
        }
        out.append(']');
    }

    /**
     * Appends the wires array to the JSON output.
     */
    private static void appendWires(StringBuilder out, boolean[] first,
                                    List<BoardState.WireState> wires) {
        appendFieldStart(out, first, "wires");
        out.append('[');
        boolean[] firstItem = new boolean[] { true };
        for (BoardState.WireState wire : wires) {
            appendSeparator(out, firstItem);
            out.append('{');
            boolean[] firstField = new boolean[] { true };
            appendNumberField(out, firstField, "startX", wire.getStartX());
            appendNumberField(out, firstField, "startY", wire.getStartY());
            appendNumberField(out, firstField, "endX", wire.getEndX());
            appendNumberField(out, firstField, "endY", wire.getEndY());
            appendStringField(out, firstField, "color", wire.getColor().name());
            appendBooleanField(out, firstField, "showData", wire.isShowData());
            out.append('}');
        }
        out.append(']');
    }

    /**
     * Parses component entries from a JSON array.
     */
    private static List<BoardState.ComponentState> parseComponents(Object raw) {
        List<BoardState.ComponentState> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<?> items = (List<?>) raw;
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> map = castObject(item);
            String type = getString(map, "type", null);
            if (type == null || type.trim().isEmpty()) {
                continue;
            }
            int x = getInt(map, "x", 0);
            int y = getInt(map, "y", 0);
            int width = getInt(map, "width", 0);
            int height = getInt(map, "height", 0);
            int rotation = getInt(map, "rotationQuarterTurns", 0);
            String displayName = getString(map, "displayName", type);
            String customId = getString(map, "customId", null);
            boolean showTitle = getBoolean(map, "showTitle", false);
            boolean showValues = getBoolean(map, "showValues", false);
            Float voltage = getFloat(map, "voltage");
            Float internalResistance = getFloat(map, "internalResistance");
            Float resistance = getFloat(map, "resistance");
            Float powerWatt = getFloat(map, "powerWatt");
            Boolean closed = getBooleanObject(map, "closed");
            result.add(new BoardState.ComponentState(type, x, y, width, height, rotation,
                    displayName, customId, showTitle, showValues, voltage, internalResistance, resistance,
                    powerWatt, closed));
        }
        return result;
    }

    /**
     * Parses custom component definitions from a JSON array.
     */
    private static List<CustomComponentDefinition> parseCustomComponents(Object raw) {
        List<CustomComponentDefinition> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<?> items = (List<?>) raw;
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> map = castObject(item);
            String id = getString(map, "id", null);
            String name = getString(map, "name", "Custom Component");
            List<CustomComponentPort> inputs = parsePorts(map.get("inputs"), CustomComponentPort.Direction.INPUT);
            List<CustomComponentPort> outputs = parsePorts(map.get("outputs"), CustomComponentPort.Direction.OUTPUT);
            String boardJson = getString(map, "boardJson", "{}");
            BoardState boardState = fromJson(boardJson);
            result.add(new CustomComponentDefinition(id, name, inputs, outputs, boardState));
        }
        return result;
    }

    /**
     * Parses port definitions from a JSON array.
     */
    private static List<CustomComponentPort> parsePorts(Object raw, CustomComponentPort.Direction direction) {
        List<CustomComponentPort> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<?> items = (List<?>) raw;
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> map = castObject(item);
            String name = getString(map, "name", "");
            result.add(new CustomComponentPort(name, direction));
        }
        return result;
    }

    /**
     * Parses wire entries from a JSON array.
     */
    private static List<BoardState.WireState> parseWires(Object raw) {
        List<BoardState.WireState> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<?> items = (List<?>) raw;
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> map = castObject(item);
            int startX = getInt(map, "startX", 0);
            int startY = getInt(map, "startY", 0);
            int endX = getInt(map, "endX", 0);
            int endY = getInt(map, "endY", 0);
            WireColor color = parseWireColor(getString(map, "color", WireColor.WHITE.name()));
            boolean showData = getBoolean(map, "showData", false);
            result.add(new BoardState.WireState(startX, startY, endX, endY, color, showData));
        }
        return result;
    }

    /**
     * Writes a field name and separator.
     */
    private static void appendFieldStart(StringBuilder out, boolean[] first, String name) {
        appendSeparator(out, first);
        writeString(out, name);
        out.append(':');
    }

    /**
     * Appends a comma separator when needed.
     */
    private static void appendSeparator(StringBuilder out, boolean[] first) {
        if (!first[0]) {
            out.append(',');
        } else {
            first[0] = false;
        }
    }

    /**
     * Writes a string field.
     */
    private static void appendStringField(StringBuilder out, boolean[] first, String name, String value) {
        appendFieldStart(out, first, name);
        writeString(out, value == null ? "" : value);
    }

    /**
     * Writes a string field when the value is present.
     */
    private static void appendOptionalStringField(StringBuilder out, boolean[] first, String name, String value) {
        if (value == null) {
            return;
        }
        appendFieldStart(out, first, name);
        writeString(out, value);
    }

    /**
     * Writes an integer field.
     */
    private static void appendNumberField(StringBuilder out, boolean[] first, String name, int value) {
        appendFieldStart(out, first, name);
        out.append(value);
    }

    /**
     * Writes a boolean field.
     */
    private static void appendBooleanField(StringBuilder out, boolean[] first, String name, boolean value) {
        appendFieldStart(out, first, name);
        out.append(value);
    }

    /**
     * Writes a numeric field when the value is present.
     */
    private static void appendOptionalNumberField(StringBuilder out, boolean[] first, String name, Float value) {
        if (value == null) {
            return;
        }
        appendFieldStart(out, first, name);
        out.append(trimFloat(value));
    }

    /**
     * Writes a boolean field when the value is present.
     */
    private static void appendOptionalBooleanField(StringBuilder out, boolean[] first, String name, Boolean value) {
        if (value == null) {
            return;
        }
        appendFieldStart(out, first, name);
        out.append(value);
    }

    /**
     * Formats a float with minimal trailing zeros.
     */
    private static String trimFloat(Float value) {
        if (value == null) {
            return "0";
        }
        if (value.floatValue() == value.longValue()) {
            return String.valueOf(value.longValue());
        }
        return value.toString();
    }

    /**
     * Writes a JSON-escaped string.
     */
    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        out.append('"');
    }

    /**
     * Casts a parsed object to a string-keyed map.
     */
    private static Map<String, Object> castObject(Object value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    /**
     * Reads a string value from a map.
     */
    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value instanceof String string ? string : fallback;
    }

    /**
     * Reads an int value from a map.
     */
    private static int getInt(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    /**
     * Reads a boolean value from a map.
     */
    private static boolean getBoolean(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    /**
     * Reads a nullable boolean value from a map.
     */
    private static Boolean getBooleanObject(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean bool ? bool : null;
    }

    /**
     * Reads a float value from a map.
     */
    private static Float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    /**
     * Parses a wire color name with a safe fallback.
     */
    private static WireColor parseWireColor(String name) {
        if (name == null) {
            return WireColor.WHITE;
        }
        try {
            return WireColor.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return WireColor.WHITE;
        }
    }

    /**
     * Minimal JSON parser for objects, arrays, numbers, booleans, and strings.
     */
    private static final class JsonParser {
        private final String input;
        private int index;

        /**
         * @param input JSON input
         */
        private JsonParser(String input) {
            this.input = input == null ? "" : input;
            this.index = 0;
        }

        /**
         * Parses a JSON value.
         */
        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                return null;
            }
            char c = input.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't' || c == 'f' || c == 'n') {
                return parseLiteral();
            }
            return parseNumber();
        }

        /**
         * Parses a JSON object into a map.
         */
        private Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == '}') {
                index++;
                return result;
            }
            while (index < input.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ':') {
                    index++;
                }
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    continue;
                }
                if (index < input.length() && input.charAt(index) == '}') {
                    index++;
                    break;
                }
            }
            return result;
        }

        /**
         * Parses a JSON array into a list.
         */
        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            index++;
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == ']') {
                index++;
                return result;
            }
            while (index < input.length()) {
                Object value = parseValue();
                result.add(value);
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                    continue;
                }
                if (index < input.length() && input.charAt(index) == ']') {
                    index++;
                    break;
                }
            }
            return result;
        }

        /**
         * Parses a JSON string token.
         */
        private String parseString() {
            if (index >= input.length() || input.charAt(index) != '"') {
                return "";
            }
            index++;
            StringBuilder out = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    break;
                }
                if (c == '\\' && index < input.length()) {
                    char esc = input.charAt(index++);
                    switch (esc) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (index + 3 < input.length()) {
                                String hex = input.substring(index, index + 4);
                                try {
                                    out.append((char) Integer.parseInt(hex, 16));
                                } catch (NumberFormatException ignored) {
                                    out.append('?');
                                }
                                index += 4;
                            }
                        }
                        default -> out.append(esc);
                    }
                    continue;
                }
                out.append(c);
            }
            return out.toString();
        }

        /**
         * Parses JSON literals: true, false, or null.
         */
        private Object parseLiteral() {
            if (input.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            if (input.startsWith("null", index)) {
                index += 4;
                return null;
            }
            return null;
        }

        /**
         * Parses a JSON number.
         */
        @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
        private Number parseNumber() {
            int start = index;
            boolean hasDecimalOrExponent = false;
            while (index < input.length()) {
                char c = input.charAt(index);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    if (c == '.' || c == 'e' || c == 'E') {
                        hasDecimalOrExponent = true;
                    }
                    index++;
                } else {
                    break;
                }
            }
            if (index == start) {
                return 0;
            }
            String number = input.substring(start, index);
            if (number.isEmpty()) {
                return 0;
            }
            if (hasDecimalOrExponent) {
                try {
                    return Double.parseDouble(number);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
            try {
                return Long.parseLong(number);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        /**
         * Advances past whitespace characters.
         */
        private void skipWhitespace() {
            while (index < input.length()) {
                char c = input.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
