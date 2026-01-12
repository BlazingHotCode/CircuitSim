package circuitsim;

import circuitsim.ui.CircuitPanel;
import circuitsim.ui.ClearBoardPanel;
import circuitsim.ui.ComponentPropertiesPanel;
import circuitsim.ui.WirePalettePanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * Application entry point for the CircuitSim UI.
 */
public class CircuitSim {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PANEL_PADDING = 8;

    /**
     * Launches the CircuitSim application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = createMainFrame();
            ComponentPropertiesPanel propertiesPanel = new ComponentPropertiesPanel();
            CircuitPanel circuitPanel = new CircuitPanel(propertiesPanel);
            propertiesPanel.setOnChange(() -> {
                circuitPanel.handlePropertyChange();
                circuitPanel.repaint();
            });

            WirePalettePanel wirePalettePanel = new WirePalettePanel(
                    circuitPanel.getActiveWireColor(),
                    circuitPanel::setActiveWireColor);
            ClearBoardPanel clearBoardPanel = new ClearBoardPanel(circuitPanel::requestClearBoard);

            JLayeredPane layeredPane = buildLayeredPane(circuitPanel, wirePalettePanel, clearBoardPanel);
            frame.add(layeredPane, BorderLayout.CENTER);
            frame.add(propertiesPanel, BorderLayout.EAST);
            frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        });
    }

    /**
     * Builds the main application frame.
     */
    private static JFrame createMainFrame() {
        JFrame frame = new JFrame("CircuitSim");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        return frame;
    }

    /**
     * Builds the layered pane that hosts the canvas and floating panels.
     */
    private static JLayeredPane buildLayeredPane(CircuitPanel circuitPanel,
                                                 WirePalettePanel wirePalettePanel,
                                                 ClearBoardPanel clearBoardPanel) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(circuitPanel, Integer.valueOf(0));
        layeredPane.add(wirePalettePanel, Integer.valueOf(1));
        layeredPane.add(clearBoardPanel, Integer.valueOf(1));
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutFloatingPanels(layeredPane, circuitPanel, wirePalettePanel, clearBoardPanel);
            }
        });
        return layeredPane;
    }

    /**
     * Positions the palette and clear button near the bottom-left corner.
     */
    private static void layoutFloatingPanels(JLayeredPane layeredPane,
                                             CircuitPanel circuitPanel,
                                             WirePalettePanel wirePalettePanel,
                                             ClearBoardPanel clearBoardPanel) {
        int width = layeredPane.getWidth();
        int height = layeredPane.getHeight();
        circuitPanel.setBounds(0, 0, width, height);

        Dimension paletteSize = wirePalettePanel.getPreferredSize();
        int paletteX = PANEL_PADDING;
        int paletteY = height - paletteSize.height - PANEL_PADDING;
        wirePalettePanel.setBounds(paletteX, Math.max(PANEL_PADDING, paletteY),
                paletteSize.width, paletteSize.height);

        Dimension clearSize = clearBoardPanel.getPreferredSize();
        int clearX = paletteX + paletteSize.width + PANEL_PADDING;
        int clearY = height - clearSize.height - PANEL_PADDING;
        clearBoardPanel.setBounds(clearX, Math.max(PANEL_PADDING, clearY),
                clearSize.width, clearSize.height);
    }
}
