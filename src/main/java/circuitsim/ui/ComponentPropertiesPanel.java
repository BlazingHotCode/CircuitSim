package circuitsim.ui;

import circuitsim.components.ComponentProperty;
import circuitsim.components.ComponentPropertyType;
import circuitsim.components.PropertyOwner;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import circuitsim.ui.Colors;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class ComponentPropertiesPanel extends JPanel {
    private final JLabel titleLabel = new JLabel("Properties");
    private final JTextField titleEditor = new JTextField();
    private final JPanel formPanel = new JPanel(new GridBagLayout());
    private final Map<ComponentProperty, Component> editors = new LinkedHashMap<>();
    private PropertyOwner owner;
    private Runnable onChange = () -> {};
    public ComponentPropertiesPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Colors.PROPERTIES_BG);
        setVisible(false);
        formPanel.setBackground(Colors.PROPERTIES_BG);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        titleLabel.setForeground(Colors.PROPERTIES_TEXT);
        titleEditor.setVisible(false);
        titleEditor.setColumns(12);
        titleEditor.setCaretColor(Colors.TITLE_CARET);
        titleEditor.setSelectionColor(Colors.TITLE_SELECTION);
        titleEditor.setSelectedTextColor(Colors.TITLE_SELECTED_TEXT);
        titleEditor.setCaret(new ThickCaret(2));
        titleEditor.setOpaque(true);
        ((DefaultCaret) titleEditor.getCaret()).setSelectionVisible(true);
        styleTextField(titleEditor);
        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(220, 0));
        showNoSelection();
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    beginTitleEdit();
                }
            }
        });
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange == null ? () -> {} : onChange;
    }

    public void setOwner(PropertyOwner owner) {
        this.owner = owner;
        if (owner == null || !hasEditableProperties(owner)) {
            setVisible(false);
            showNoSelection();
            return;
        }
        setVisible(true);
        rebuildForm();
    }

    public PropertyOwner getOwner() {
        return owner;
    }

    private void showNoSelection() {
        formPanel.removeAll();
        editors.clear();
        titleLabel.setText("Properties");
        titleEditor.setVisible(false);
        JLabel emptyLabel = new JLabel("No selection");
        emptyLabel.setForeground(Colors.PROPERTIES_TEXT);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        formPanel.add(emptyLabel, constraints);
        revalidate();
        repaint();
    }

    private boolean hasEditableProperties(PropertyOwner owner) {
        if (owner == null) {
            return false;
        }
        for (ComponentProperty property : owner.getProperties()) {
            if (property.isEditable()) {
                return true;
            }
        }
        return false;
    }

    private void rebuildForm() {
        if (owner == null) {
            showNoSelection();
            return;
        }
        formPanel.removeAll();
        editors.clear();
        titleLabel.setText(owner.getDisplayName());
        if (!owner.isTitleEditable() && titleEditor.isVisible()) {
            remove(titleEditor);
            add(titleLabel, BorderLayout.NORTH);
            titleEditor.setVisible(false);
        }
        ComponentProperty showTitleProperty = null;
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridy = 0;
        for (ComponentProperty property : owner.getProperties()) {
            if ("Show Title".equals(property.getName())) {
                showTitleProperty = property;
                continue;
            }
            Component editor = createEditor(property);
            if (editor == null) {
                continue;
            }
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(property.getName());
            label.setForeground(Colors.PROPERTIES_TEXT);
            formPanel.add(label, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(editor, constraints);
            constraints.gridy++;
            editors.put(property, editor);
        }
        if (showTitleProperty != null) {
            Component editor = createEditor(showTitleProperty);
            if (editor != null) {
                constraints.gridx = 0;
                constraints.weightx = 0;
                constraints.fill = GridBagConstraints.NONE;
                JLabel label = new JLabel(showTitleProperty.getName());
                label.setForeground(Colors.PROPERTIES_TEXT);
                formPanel.add(label, constraints);
                constraints.gridx = 1;
                constraints.weightx = 1;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                formPanel.add(editor, constraints);
                constraints.gridy++;
                editors.put(showTitleProperty, editor);
            }
        }
        revalidate();
        repaint();
    }

    private Component createEditor(ComponentProperty property) {
        if (!property.isEditable()) {
            return null;
        }
        if (property.getType() == ComponentPropertyType.BOOLEAN) {
            JCheckBox checkBox = new JCheckBox();
            Object value = property.getEditorValue();
            checkBox.setSelected(Boolean.TRUE.equals(value));
            checkBox.setBackground(Colors.PROPERTIES_BG);
            checkBox.setForeground(Colors.PROPERTIES_TEXT);
            checkBox.addItemListener(event -> {
                property.setValueFromEditor(checkBox.isSelected());
                onChange.run();
            });
            return checkBox;
        }
        if (property.getType() == ComponentPropertyType.FLOAT) {
            JTextField field = new JTextField(10);
            Object value = property.getEditorValue();
            field.setText(value == null ? "" : value.toString());
            styleTextField(field);
            if (field.getDocument() instanceof AbstractDocument) {
                ((AbstractDocument) field.getDocument()).setDocumentFilter(new NumericDocumentFilter());
            }
            field.getDocument().addDocumentListener(new PropertyDocumentListener(() -> {
                property.setValueFromEditor(field.getText());
                onChange.run();
                repaint();
            }));
            return field;
        }
        if (property.getType() == ComponentPropertyType.STRING) {
            JTextField field = new JTextField(12);
            Object value = property.getEditorValue();
            field.setText(value == null ? "" : value.toString());
            styleTextField(field);
            field.getDocument().addDocumentListener(new PropertyDocumentListener(() -> {
                property.setValueFromEditor(field.getText());
                onChange.run();
                repaint();
            }));
            return field;
        }
        return null;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(Colors.PROPERTIES_INPUT_BG);
        field.setForeground(Colors.PROPERTIES_TEXT);
        field.setCaretColor(Colors.PROPERTIES_TEXT);
        field.setBorder(BorderFactory.createLineBorder(Colors.PROPERTIES_INPUT_BORDER));
    }

    private void beginTitleEdit() {
        if (owner == null || !owner.isTitleEditable()) {
            return;
        }
        titleEditor.setText(owner.getDisplayName());
        remove(titleLabel);
        add(titleEditor, BorderLayout.NORTH);
        titleEditor.setVisible(true);
        titleEditor.requestFocusInWindow();
        titleEditor.selectAll();
        for (java.awt.event.ActionListener listener : titleEditor.getActionListeners()) {
            titleEditor.removeActionListener(listener);
        }
        for (java.awt.event.FocusListener listener : titleEditor.getFocusListeners()) {
            titleEditor.removeFocusListener(listener);
        }
        javax.swing.text.Document document = titleEditor.getDocument();
        if (document instanceof javax.swing.text.AbstractDocument) {
            DocumentListener[] listeners = ((javax.swing.text.AbstractDocument) document)
                    .getDocumentListeners();
            for (DocumentListener listener : listeners) {
                document.removeDocumentListener(listener);
            }
        }
        titleEditor.addActionListener(event -> commitTitleEdit());
        titleEditor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                commitTitleEdit();
            }
        });
        titleEditor.getDocument().addDocumentListener(new PropertyDocumentListener(() -> {
            if (owner != null) {
                owner.setDisplayName(titleEditor.getText());
                titleLabel.setText(owner.getDisplayName());
                onChange.run();
            }
        }));
        titleEditor.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "commitTitle");
        titleEditor.getActionMap().put("commitTitle", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                commitTitleEdit();
            }
        });
        titleEditor.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "commitTitle");
        revalidate();
        repaint();
    }

    private void commitTitleEdit() {
        if (owner != null) {
            owner.setDisplayName(titleEditor.getText());
            titleLabel.setText(owner.getDisplayName());
            onChange.run();
        }
        remove(titleEditor);
        add(titleLabel, BorderLayout.NORTH);
        titleEditor.setVisible(false);
        revalidate();
        repaint();
    }

    private static class PropertyDocumentListener implements DocumentListener {
        private final Runnable onChange;

        private PropertyDocumentListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange.run();
        }
    }

    private static class ThickCaret extends DefaultCaret {
        private final int caretWidth;

        private ThickCaret(int caretWidth) {
            this.caretWidth = caretWidth;
        }

        @Override
        protected synchronized void damage(java.awt.Rectangle r) {
            if (r == null) {
                return;
            }
            x = r.x - (caretWidth / 2);
            y = r.y;
            width = caretWidth;
            height = r.height;
            repaint();
        }

        @Override
        public void paint(java.awt.Graphics g) {
            JTextComponent component = getComponent();
            if (component == null) {
                return;
            }
            java.awt.Rectangle caretRect;
            try {
                caretRect = component.modelToView(getDot());
            } catch (javax.swing.text.BadLocationException e) {
                return;
            }
            if (caretRect == null) {
                return;
            }
            if (getDot() != getMark()) {
                return;
            }
            g.setColor(component.getCaretColor());
            g.fillRect(caretRect.x - (caretWidth / 2), caretRect.y, caretWidth, caretRect.height);
        }
    }

    private static class NumericDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) {
                return;
            }
            StringBuilder builder = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            builder.insert(offset, string);
            if (isValidNumber(builder.toString())) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            StringBuilder builder = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            builder.replace(offset, offset + length, text == null ? "" : text);
            if (isValidNumber(builder.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            StringBuilder builder = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            builder.delete(offset, offset + length);
            if (isValidNumber(builder.toString())) {
                super.remove(fb, offset, length);
            }
        }

        private boolean isValidNumber(String text) {
            if (text.isEmpty()) {
                return true;
            }
            int start = 0;
            if (text.charAt(0) == '-') {
                if (text.length() == 1) {
                    return true;
                }
                start = 1;
            }
            boolean dotSeen = false;
            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '.') {
                    if (dotSeen) {
                        return false;
                    }
                    dotSeen = true;
                    continue;
                }
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }
    }
}
