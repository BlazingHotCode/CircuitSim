package circuitsim;

import circuitsim.ui.CircuitPanel;
import circuitsim.ui.ComponentPropertiesPanel;
import circuitsim.ui.WirePalettePanel;
import circuitsim.ui.ClearBoardPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import javax.swing.JLayeredPane;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class CircuitSim {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CircuitSim");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            ComponentPropertiesPanel propertiesPanel = new ComponentPropertiesPanel();
            CircuitPanel circuitPanel = new CircuitPanel(propertiesPanel);
            propertiesPanel.setOnChange(circuitPanel::repaint);
            WirePalettePanel wirePalettePanel = new WirePalettePanel(
                    circuitPanel.getActiveWireColor(),
                    circuitPanel::setActiveWireColor);
            ClearBoardPanel clearBoardPanel = new ClearBoardPanel(circuitPanel::requestClearBoard);
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.add(circuitPanel, Integer.valueOf(0));
            layeredPane.add(wirePalettePanel, Integer.valueOf(1));
            layeredPane.add(clearBoardPanel, Integer.valueOf(1));
            layeredPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int width = layeredPane.getWidth();
                    int height = layeredPane.getHeight();
                    circuitPanel.setBounds(0, 0, width, height);
                    java.awt.Dimension paletteSize = wirePalettePanel.getPreferredSize();
                    int paletteX = 8;
                    int paletteY = height - paletteSize.height - 8;
                    wirePalettePanel.setBounds(paletteX, Math.max(8, paletteY),
                            paletteSize.width, paletteSize.height);
                    java.awt.Dimension clearSize = clearBoardPanel.getPreferredSize();
                    int clearX = paletteX + paletteSize.width + 8;
                    int clearY = height - clearSize.height - 8;
                    clearBoardPanel.setBounds(clearX, Math.max(8, clearY),
                            clearSize.width, clearSize.height);
                }
            });
            frame.add(layeredPane, BorderLayout.CENTER);
            frame.add(propertiesPanel, BorderLayout.EAST);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        });
    }
}
