package circuitsim.custom;

import circuitsim.io.BoardState;
import circuitsim.io.BoardStateIO;
import circuitsim.io.DataPaths;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and stores custom component definitions from disk.
 */
public final class CustomComponentLibrary {
    private final Map<String, CustomComponentDefinition> definitions = new LinkedHashMap<>();
    private final Path localDir = DataPaths.getCustomComponentsDir();
    private final Path tempDir = DataPaths.getTempDataDir();
    private Path activeDir;
    private boolean tempMode;

    public CustomComponentLibrary() {
        initializeActiveDir();
        loadActiveDefinitions();
    }

    /**
     * @return true if temp storage is active
     */
    public boolean isTempMode() {
        return tempMode;
    }

    /**
     * @return active storage directory
     */
    public Path getActiveDir() {
        return activeDir;
    }

    /**
     * @return immutable list of custom component definitions
     */
    public List<CustomComponentDefinition> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    /**
     * Returns a definition by id.
     */
    public CustomComponentDefinition getDefinition(String id) {
        return id == null ? null : definitions.get(id);
    }

    /**
     * Adds or updates a custom component definition in the active storage.
     */
    public void saveDefinition(CustomComponentDefinition definition) throws IOException {
        if (definition == null) {
            return;
        }
        ensureDir(activeDir);
        Path file = activeDir.resolve(definition.getId() + ".json");
        BoardState wrapper = new BoardState(BoardState.CURRENT_VERSION, circuitsim.components.WireColor.WHITE,
                Collections.emptyList(), Collections.emptyList(),
                Collections.singletonList(definition));
        Files.writeString(file, BoardStateIO.toJson(wrapper));
        definitions.put(definition.getId(), definition);
    }

    /**
     * Deletes a custom component definition from storage.
     */
    public void deleteDefinition(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        Path file = activeDir.resolve(id + ".json");
        Files.deleteIfExists(file);
        definitions.remove(id);
    }

    /**
     * Merges definitions into local storage.
     */
    public void mergeIntoLocal(List<CustomComponentDefinition> incoming) throws IOException {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        ensureDir(localDir);
        for (CustomComponentDefinition definition : incoming) {
            if (definition == null || definition.getId() == null) {
                continue;
            }
            Path file = localDir.resolve(definition.getId() + ".json");
            if (!Files.exists(file)) {
                BoardState wrapper = new BoardState(BoardState.CURRENT_VERSION,
                        circuitsim.components.WireColor.WHITE, Collections.emptyList(),
                        Collections.emptyList(), Collections.singletonList(definition));
                Files.writeString(file, BoardStateIO.toJson(wrapper));
            }
        }
        if (!tempMode) {
            loadFromDir(localDir, definitions);
        }
    }

    /**
     * Activates temp storage and copies local definitions into it.
     */
    public void activateTempMode(List<CustomComponentDefinition> incoming) throws IOException {
        if (incoming == null) {
            incoming = Collections.emptyList();
        }
        if (Files.exists(tempDir)) {
            clearDirectory(tempDir);
        }
        ensureDir(tempDir);
        for (CustomComponentDefinition definition : incoming) {
            if (definition == null || definition.getId() == null) {
                continue;
            }
            writeDefinitionFile(tempDir, definition);
        }
        copyMissingDefinitions(localDir, tempDir);
        tempMode = true;
        activeDir = tempDir;
        loadActiveDefinitions();
    }

    /**
     * Clears temp storage and switches back to local storage.
     */
    public void clearTempMode() throws IOException {
        if (Files.exists(tempDir)) {
            clearDirectory(tempDir);
            Files.deleteIfExists(tempDir);
        }
        tempMode = false;
        activeDir = localDir;
        loadActiveDefinitions();
    }

    private void initializeActiveDir() {
        if (Files.exists(tempDir)) {
            tempMode = true;
            activeDir = tempDir;
            try {
                copyMissingDefinitions(localDir, tempDir);
            } catch (IOException ignored) {
                // Best-effort to keep temp in sync with local.
            }
        } else {
            tempMode = false;
            activeDir = localDir;
        }
    }

    private void loadActiveDefinitions() {
        definitions.clear();
        loadFromDir(activeDir, definitions);
    }

    private void loadFromDir(Path dir, Map<String, CustomComponentDefinition> target) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                CustomComponentDefinition definition = readDefinition(file);
                if (definition != null) {
                    target.put(definition.getId(), definition);
                }
            }
        } catch (IOException ignored) {
            // Best-effort.
        }
    }

    private CustomComponentDefinition readDefinition(Path file) {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            String json = Files.readString(file);
            BoardState wrapper = BoardStateIO.fromJson(json);
            if (wrapper.getCustomComponents().isEmpty()) {
                return null;
            }
            return wrapper.getCustomComponents().get(0);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private void copyMissingDefinitions(Path sourceDir, Path targetDir) throws IOException {
        if (sourceDir == null || !Files.exists(sourceDir)) {
            return;
        }
        ensureDir(targetDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "*.json")) {
            for (Path file : stream) {
                Path target = targetDir.resolve(file.getFileName());
                if (!Files.exists(target)) {
                    Files.copy(file, target);
                }
            }
        }
    }

    private void writeDefinitionFile(Path dir, CustomComponentDefinition definition) throws IOException {
        ensureDir(dir);
        Path file = dir.resolve(definition.getId() + ".json");
        BoardState wrapper = new BoardState(BoardState.CURRENT_VERSION, circuitsim.components.WireColor.WHITE,
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList(definition));
        Files.writeString(file, BoardStateIO.toJson(wrapper));
    }

    private void ensureDir(Path dir) throws IOException {
        if (dir != null) {
            Files.createDirectories(dir);
        }
    }

    private void clearDirectory(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    clearDirectory(file);
                }
                Files.deleteIfExists(file);
            }
        }
    }
}
