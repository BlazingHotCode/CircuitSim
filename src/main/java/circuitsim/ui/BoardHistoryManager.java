package circuitsim.ui;

import circuitsim.io.BoardState;
import circuitsim.io.BoardStateIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages undo/redo stacks and autosave for a board.
 */
final class BoardHistoryManager {
    private final int maxHistory;
    private final Deque<BoardState> undoStack = new ArrayDeque<>();
    private final Deque<BoardState> redoStack = new ArrayDeque<>();
    private Path autosavePath;
    private Runnable changeListener = () -> {};

    BoardHistoryManager(int maxHistory) {
        this.maxHistory = Math.max(1, maxHistory);
    }

    void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener == null ? () -> {} : changeListener;
    }

    void initializeAutosavePath() {
        autosavePath = circuitsim.io.DataPaths.getAutosavePath();
        try {
            Files.createDirectories(autosavePath.getParent());
        } catch (IOException ex) {
            autosavePath = null;
        }
    }

    void setAutosavePath(Path autosavePath, boolean load, Consumer<BoardState> onLoad, Consumer<BoardState> onResetHistory) {
        this.autosavePath = autosavePath;
        if (this.autosavePath != null) {
            try {
                Files.createDirectories(this.autosavePath.getParent());
            } catch (IOException ex) {
                this.autosavePath = null;
            }
        }
        if (load) {
            attemptLoadAutosave(onLoad, onResetHistory);
        }
    }

    void attemptLoadAutosave(Consumer<BoardState> onLoad, Consumer<BoardState> onResetHistory) {
        if (autosavePath == null || !Files.exists(autosavePath)) {
            return;
        }
        try {
            String json = Files.readString(autosavePath);
            BoardState state = BoardStateIO.fromJson(json);
            if (onLoad != null) {
                onLoad.accept(state);
            }
            if (onResetHistory != null) {
                onResetHistory.accept(state);
            }
        } catch (IOException | RuntimeException ignored) {
            // Autosave is best-effort; ignore failures.
        }
    }

    void flushAutosave(Supplier<BoardState> snapshot) {
        if (snapshot == null) {
            return;
        }
        writeAutosave(snapshot.get());
    }

    boolean isHistoryEmpty() {
        return undoStack.isEmpty();
    }

    void recordHistoryState(Supplier<BoardState> snapshot) {
        if (snapshot == null) {
            return;
        }
        BoardState state = snapshot.get();
        undoStack.push(state);
        redoStack.clear();
        trimHistory(undoStack);
        writeAutosave(state);
        changeListener.run();
    }

    void resetHistoryState(BoardState state) {
        undoStack.clear();
        redoStack.clear();
        if (state != null) {
            undoStack.push(state);
            trimHistory(undoStack);
        }
        writeAutosave(state);
    }

    void undo(Consumer<BoardState> applyState) {
        if (undoStack.size() < 2) {
            return;
        }
        BoardState current = undoStack.pop();
        redoStack.push(current);
        BoardState previous = undoStack.peek();
        if (applyState != null && previous != null) {
            applyState.accept(previous);
        }
        writeAutosave(previous);
    }

    void redo(Consumer<BoardState> applyState) {
        if (redoStack.isEmpty()) {
            return;
        }
        BoardState next = redoStack.pop();
        undoStack.push(next);
        trimHistory(undoStack);
        if (applyState != null && next != null) {
            applyState.accept(next);
        }
        writeAutosave(next);
    }

    private void writeAutosave(BoardState state) {
        if (autosavePath == null) {
            return;
        }
        try {
            Files.writeString(autosavePath, BoardStateIO.toJson(state));
        } catch (IOException ignored) {
            // Autosave is best-effort; ignore failures.
        }
    }

    private void trimHistory(Deque<BoardState> stack) {
        while (stack.size() > maxHistory) {
            stack.removeLast();
        }
    }
}

