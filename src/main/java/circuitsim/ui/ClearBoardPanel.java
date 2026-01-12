package circuitsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ClearBoardPanel extends JPanel {
    public ClearBoardPanel(Runnable onClear) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));

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
        add(clearButton, BorderLayout.CENTER);
        setPreferredSize(new Dimension(56, 24));
    }
}
