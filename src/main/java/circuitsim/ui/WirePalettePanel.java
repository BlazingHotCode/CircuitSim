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
    private static final int SWATCH_SIZE = 22;
    private static final int COLLAPSED_HEIGHT = 26;
    private static final int PANEL_WIDTH = 120;
    private final JPanel swatchPanel;
    private final JPanel header;
    private final JPanel footer;
    private final JLabel title;
    private final JToggleButton collapseButton;
    private boolean collapsed;
    private int expandedHeight;

    public WirePalettePanel(WireColor initialColor, Consumer<WireColor> onSelect) {
        setLayout(new BorderLayout());
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setOpaque(false);

        swatchPanel = new JPanel(new GridLayout(0, 2, 4, 4));
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

        header = new JPanel(new BorderLayout());
        header.setBackground(Colors.WIRE_PALETTE_BG);
        header.setOpaque(true);
        title = new JLabel("Wire Color");
        title.setForeground(Colors.PROPERTIES_TEXT);
        header.add(title, BorderLayout.WEST);

        collapseButton = new JToggleButton("v");
        collapseButton.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        collapseButton.setFocusPainted(false);
        collapseButton.setOpaque(true);
        collapseButton.setBackground(Colors.WIRE_PALETTE_BG);
        collapseButton.setForeground(Colors.PROPERTIES_TEXT);
        collapseButton.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));
        collapseButton.setContentAreaFilled(true);
        collapseButton.setRolloverEnabled(false);
        collapseButton.addActionListener(event -> {
            collapsed = collapseButton.isSelected();
            collapseButton.setText(collapsed ? "^" : "v");
            collapseButton.setBackground(Colors.WIRE_PALETTE_BG);
            swatchPanel.setVisible(!collapsed);
            title.setVisible(!collapsed);
            updateCollapseButtonPlacement();
            setPreferredSize(new Dimension(PANEL_WIDTH, collapsed ? COLLAPSED_HEIGHT : expandedHeight));
            revalidate();
            repaint();
        });
        java.awt.event.MouseAdapter headerClickAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleCollapse();
            }
        };
        header.addMouseListener(headerClickAdapter);
        title.addMouseListener(headerClickAdapter);
        header.add(collapseButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(swatchPanel, BorderLayout.CENTER);
        footer = new JPanel(new BorderLayout());
        footer.setBackground(Colors.WIRE_PALETTE_BG);
        footer.setOpaque(true);
        footer.setVisible(false);
        add(footer, BorderLayout.SOUTH);
        expandedHeight = header.getPreferredSize().height + swatchPanel.getPreferredSize().height + 12;
        setPreferredSize(new Dimension(PANEL_WIDTH, expandedHeight));
    }

    private JToggleButton createSwatchButton(WireColor color) {
        JToggleButton button = new JToggleButton();
        button.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        button.setOpaque(true);
        button.setBackground(color.getColor());
        button.putClientProperty("wireColor", color);
        button.setToolTipText(color.getName());
        button.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 2));
        button.setContentAreaFilled(true);
        button.setRolloverEnabled(false);
        return button;
    }

    private void updateSelectionBorders(JPanel swatchPanel) {
        for (java.awt.Component component : swatchPanel.getComponents()) {
            if (!(component instanceof JToggleButton)) {
                continue;
            }
            JToggleButton button = (JToggleButton) component;
            Object value = button.getClientProperty("wireColor");
            if (value instanceof WireColor) {
                button.setBackground(((WireColor) value).getColor());
            }
            if (button.isSelected()) {
                button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_SELECTED, 2));
            } else {
                button.setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 2));
            }
        }
        swatchPanel.repaint();
    }

    private void updateCollapseButtonPlacement() {
        if (collapsed) {
            header.remove(collapseButton);
            footer.add(collapseButton, BorderLayout.CENTER);
            footer.setVisible(true);
        } else {
            footer.remove(collapseButton);
            header.add(collapseButton, BorderLayout.EAST);
            footer.setVisible(false);
        }
        header.revalidate();
        footer.revalidate();
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        collapseButton.setSelected(collapsed);
        collapseButton.setText(collapsed ? "^" : "v");
        collapseButton.setBackground(Colors.WIRE_PALETTE_BG);
        swatchPanel.setVisible(!collapsed);
        title.setVisible(!collapsed);
        updateCollapseButtonPlacement();
        setPreferredSize(new Dimension(PANEL_WIDTH, collapsed ? COLLAPSED_HEIGHT : expandedHeight));
        revalidate();
        repaint();
    }
}
