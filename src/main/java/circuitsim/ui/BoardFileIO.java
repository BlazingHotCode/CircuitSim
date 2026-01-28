package circuitsim.ui;

import circuitsim.io.BoardState;
import circuitsim.io.BoardStateIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JFileChooser;
import javax.swing.JComponent;

/**
 * Handles save/load dialogs for board JSON files.
 */
final class BoardFileIO {
    private Path lastBoardPath;

    void save(JComponent parent, Supplier<BoardState> snapshot,
              java.util.function.BiConsumer<String, Exception> showError) {
        if (parent == null || snapshot == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (lastBoardPath != null) {
            chooser.setSelectedFile(lastBoardPath.toFile());
        }
        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        Path path = ensureJsonExtension(selected);
        BoardState state = snapshot.get();
        try {
            Files.writeString(path, BoardStateIO.toJson(state));
            lastBoardPath = path;
        } catch (IOException ex) {
            if (showError != null) {
                showError.accept("Failed to save board state.", ex);
            }
        }
    }

    void load(JComponent parent,
              Function<BoardState, BoardState> boardLoadTransform,
              Consumer<BoardState> applyState,
              Consumer<BoardState> resetHistory,
              java.util.function.BiConsumer<String, Exception> showError) {
        if (parent == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (lastBoardPath != null) {
            chooser.setSelectedFile(lastBoardPath.toFile());
        }
        int result = chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            String json = Files.readString(path);
            BoardState state = BoardStateIO.fromJson(json);
            if (boardLoadTransform != null) {
                state = boardLoadTransform.apply(state);
            }
            if (applyState != null) {
                applyState.accept(state);
            }
            lastBoardPath = path;
            if (resetHistory != null) {
                resetHistory.accept(state);
            }
        } catch (IOException ex) {
            if (showError != null) {
                showError.accept("Failed to load board state.", ex);
            }
        } catch (RuntimeException ex) {
            if (showError != null) {
                showError.accept("Invalid board state file.", ex);
            }
        }
    }

    private Path ensureJsonExtension(Path selected) {
        String name = selected.getFileName().toString();
        if (name.toLowerCase().endsWith(".json")) {
            return selected;
        }
        return selected.resolveSibling(name + ".json");
    }
}

