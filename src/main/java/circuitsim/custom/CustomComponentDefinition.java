package circuitsim.custom;

import circuitsim.io.BoardState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Stores metadata and internal layout for a custom component.
 */
public final class CustomComponentDefinition {
    private final String id;
    private final String name;
    private final List<CustomComponentPort> inputs;
    private final List<CustomComponentPort> outputs;
    private final BoardState boardState;

    public CustomComponentDefinition(String name, List<CustomComponentPort> inputs,
                                     List<CustomComponentPort> outputs, BoardState boardState) {
        this(UUID.randomUUID().toString(), name, inputs, outputs, boardState);
    }

    public CustomComponentDefinition(String id, String name, List<CustomComponentPort> inputs,
                                     List<CustomComponentPort> outputs, BoardState boardState) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id.trim();
        this.name = name == null ? "Custom Component" : name.trim();
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs == null
                ? Collections.emptyList()
                : inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs == null
                ? Collections.emptyList()
                : outputs));
        this.boardState = boardState;
    }

    /**
     * @return unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return input port definitions
     */
    public List<CustomComponentPort> getInputs() {
        return inputs;
    }

    /**
     * @return output port definitions
     */
    public List<CustomComponentPort> getOutputs() {
        return outputs;
    }

    /**
     * @return internal board state
     */
    public BoardState getBoardState() {
        return boardState;
    }
}
