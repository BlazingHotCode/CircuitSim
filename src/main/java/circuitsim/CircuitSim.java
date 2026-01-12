package circuitsim;

import circuitsim.ui.CircuitPanel;
import circuitsim.ui.ComponentPropertiesPanel;
import circuitsim.ui.WirePalettePanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

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
            frame.add(circuitPanel, BorderLayout.CENTER);
            frame.add(propertiesPanel, BorderLayout.EAST);
            frame.add(wirePalettePanel, BorderLayout.WEST);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        });
    }
}
