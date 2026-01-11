package circuitsim.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import circuitsim.ui.Colors;

public class ShortCircuitPopup extends JPanel {
    private final JButton closeButton;

    public ShortCircuitPopup() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBackground(Colors.SHORT_POPUP_BG);
        setOpaque(true);
        JLabel label = new JLabel("Short circuit detected");
        label.setForeground(Colors.SHORT_POPUP_TEXT);
        closeButton = new JButton("X");
        closeButton.setFocusable(false);
        add(label);
        add(closeButton);
        setVisible(false);
        setPreferredSize(new Dimension(220, 32));
        closeButton.addActionListener(event -> hidePopup());
    }

    public void showPopup() {
        setVisible(true);
    }

    public void hidePopup() {
        setVisible(false);
    }
}
