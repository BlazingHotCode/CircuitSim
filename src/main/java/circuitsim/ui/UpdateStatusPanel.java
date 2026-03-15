package circuitsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Small top-bar button that shows installed and latest app versions.
 */
public class UpdateStatusPanel extends JPanel {
    private final JButton button;

    public UpdateStatusPanel(Runnable onClick) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Colors.WIRE_PALETTE_BG);
        setBorder(BorderFactory.createLineBorder(Colors.WIRE_PALETTE_BORDER, 1));
        button = new JButton();
        button.setFocusPainted(false);
        button.setBackground(Colors.WIRE_PALETTE_BG);
        button.setForeground(Colors.PROPERTIES_TEXT);
        button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        button.addActionListener(event -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        add(button, BorderLayout.CENTER);
        setVersionText("?", "checking...");
    }

    public void setVersionText(String currentVersion, String latestVersion) {
        String current = normalizeVersion(currentVersion, "?");
        String latest = normalizeVersion(latestVersion, "checking...");
        button.setText("v" + current + " / v" + latest);
        updateSize();
    }

    public void setStatusText(String currentVersion, String statusText) {
        String current = normalizeVersion(currentVersion, "?");
        String status = normalizeVersion(statusText, "?");
        button.setText("v" + current + " / " + status);
        updateSize();
    }

    private void updateSize() {
        Dimension size = button.getPreferredSize();
        setPreferredSize(new Dimension(size.width + 2, 26));
        revalidate();
    }

    public void setUnknownLatest(String currentVersion) {
        setStatusText(currentVersion, "unavailable");
    }

    private String normalizeVersion(String version, String fallback) {
        if (version == null || version.isBlank()) {
            return fallback;
        }
        return version;
    }
}
