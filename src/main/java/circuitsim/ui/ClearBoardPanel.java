package circuitsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Compact panel that exposes a single action to clear the circuit board.
 */
public class ClearBoardPanel extends JPanel {
    /**
     * @param onClear callback invoked when the clear button is pressed
     */
    public ClearBoardPanel(Runnable onClear) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));

        add(createClearButton(onClear), BorderLayout.CENTER);
        setPreferredSize(new Dimension(56, 24));
    }

    /**
     * Creates the clear action button.
     */
    private JButton createClearButton(Runnable onClear) {
        JButton clearButton = new JButton("Clear");
        clearButton.setFocusPainted(false);
        clearButton.setBackground(Colors.WIRE_PALETTE_BG);
        clearButton.setForeground(Colors.PROPERTIES_TEXT);
        clearButton.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        clearButton.addActionListener(event -> {
            if (onClear != null) {
                onClear.run();
            }
        });
        return clearButton;
    }
}
