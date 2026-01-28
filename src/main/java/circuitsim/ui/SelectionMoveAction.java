package circuitsim.ui;

import java.awt.event.ActionEvent;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractAction;

/**
 * Swing action that moves the selection when any selection exists.
 */
final class SelectionMoveAction extends AbstractAction {
    private final BooleanSupplier hasSelection;
    private final Runnable move;

    SelectionMoveAction(BooleanSupplier hasSelection, Runnable move) {
        this.hasSelection = hasSelection == null ? () -> false : hasSelection;
        this.move = move == null ? () -> {} : move;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (hasSelection.getAsBoolean()) {
            move.run();
        }
    }
}

