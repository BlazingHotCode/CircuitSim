package circuitsim.ui;

import circuitsim.components.WireColor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class WirePalettePanel extends JPanel {
    private static final int SWATCH_SIZE = 28;
    private static final int COLLAPSED_WIDTH = 28;
    private final JPanel swatchPanel;
    private boolean collapsed;

    public WirePalettePanel(WireColor initialColor, Consumer<WireColor> onSelect) {
        setLayout(new BorderLayout());
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.WIRE_PALETTE_BG);
        JLabel title = new JLabel("Wire Color");
        title.setForeground(Colors.PROPERTIES_TEXT);
        header.add(title, BorderLayout.WEST);

        JToggleButton collapseButton = new JToggleButton("<<");
        collapseButton.setFocusPainted(false);
        collapseButton.setBackground(Colors.WIRE_PALETTE_BG);
        collapseButton.setForeground(Colors.PROPERTIES_TEXT);
        collapseButton.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));
        collapseButton.addActionListener(event -> {
            collapsed = collapseButton.isSelected();
            collapseButton.setText(collapsed ? ">>" : "<<");
            swatchPanel.setVisible(!collapsed);
            title.setVisible(!collapsed);
            setPreferredSize(new Dimension(collapsed ? COLLAPSED_WIDTH : 140, 0));
            revalidate();
            repaint();
        });
        header.add(collapseButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        swatchPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        swatchPanel.setBackground(Colors.WIRE_PALETTE_BG);
        ButtonGroup group = new ButtonGroup();

        for (WireColor color : WireColor.values()) {
            JToggleButton button = createSwatchButton(color);
            button.addActionListener(event -> {
                if (onSelect != null) {
                    onSelect.accept(color);
                }
                updateSelectionBorders(swatchPanel);
            });
            if (color == initialColor) {
                button.setSelected(true);
            }
            group.add(button);
            swatchPanel.add(button);
        }

        updateSelectionBorders(swatchPanel);
        add(swatchPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(140, 0));
    }

    private JToggleButton createSwatchButton(WireColor color) {
        JToggleButton button = new JToggleButton();
        button.setOpaque(true);
        button.setBackground(color.getColor());
        button.setToolTipText(color.getName());
        button.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 2));
        return button;
    }

    private void updateSelectionBorders(JPanel swatchPanel) {
        for (java.awt.Component component : swatchPanel.getComponents()) {
            if (!(component instanceof JToggleButton)) {
                continue;
            }
            JToggleButton button = (JToggleButton) component;
            if (button.isSelected()) {
                button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_SELECTED, 2));
            } else {
                button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 2));
            }
        }
        swatchPanel.repaint();
    }
}
