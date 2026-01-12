package circuitsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel with controls for the custom component editor.
 */
public class CustomEditorPanel extends JPanel {
    private final JLabel titleLabel = new JLabel("Custom Editor");
    private final JLabel subtitleLabel = new JLabel("");

    public CustomEditorPanel(Runnable onSave, Runnable onCancel) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        titleLabel.setForeground(Colors.PROPERTIES_TEXT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        topRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton exitButton = createButton("Exit", onSave);
        buttons.add(exitButton);
        topRow.add(buttons, BorderLayout.EAST);
        add(topRow, BorderLayout.NORTH);
        setPreferredSize(new Dimension(360, 52));

        subtitleLabel.setForeground(Colors.PROPERTIES_TEXT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        add(subtitleLabel, BorderLayout.SOUTH);
    }

    /**
     * Updates the title displayed in the editor panel.
     */
    public void setTitleText(String title) {
        String text = title == null ? "Custom Editor" : title;
        titleLabel.setText("Editor");
        subtitleLabel.setText(text);
    }

    private JButton createButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setBackground(Colors.WIRE_PALETTE_BG);
        button.setForeground(Colors.PROPERTIES_TEXT);
        button.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        button.addActionListener(event -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }
}
