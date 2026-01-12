package circuitsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Panel that shows a button to exit temp mode.
 */
public class TempModePanel extends JPanel {
    /**
     * @param onExit callback invoked when the button is pressed
     */
    public TempModePanel(Runnable onExit) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));
        add(createButton(onExit), BorderLayout.CENTER);
        setPreferredSize(new Dimension(140, 26));
    }

    private JButton createButton(Runnable onExit) {
        JButton button = new JButton("Go Back To Save");
        button.setFocusPainted(false);
        button.setBackground(Colors.WIRE_PALETTE_BG);
        button.setForeground(Colors.PROPERTIES_TEXT);
        button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        button.addActionListener(event -> {
            if (onExit != null) {
                onExit.run();
            }
        });
        return button;
    }
}
