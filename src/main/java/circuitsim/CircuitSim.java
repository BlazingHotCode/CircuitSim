package circuitsim;

import circuitsim.components.ComponentRegistry;
import circuitsim.components.CustomComponent;
import circuitsim.components.CustomInputPort;
import circuitsim.components.CustomOutputPort;
import circuitsim.custom.CustomComponentDefinition;
import circuitsim.custom.CustomComponentLibrary;
import circuitsim.custom.CustomComponentPort;
import circuitsim.io.BoardState;
import circuitsim.ui.CircuitPanel;
import circuitsim.ui.ClearBoardPanel;
import circuitsim.ui.ComponentBarPanel;
import circuitsim.ui.ComponentPropertiesPanel;
import circuitsim.ui.CustomEditorPanel;
import circuitsim.ui.TempModePanel;
import circuitsim.ui.WirePalettePanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
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
            CustomComponentLibrary library = new CustomComponentLibrary();

            CircuitPanel mainPanel = new CircuitPanel(propertiesPanel);
            CircuitPanel editorPanel = new CircuitPanel(propertiesPanel, false);
            editorPanel.setTreatCustomOutputsAsGround(true);

            CircuitPanel[] activePanel = new CircuitPanel[] { mainPanel };
            CustomComponentDefinition[] editingDefinition = new CustomComponentDefinition[1];
            propertiesPanel.setOnChange(() -> {
                activePanel[0].handlePropertyChange();
                activePanel[0].repaint();
                if (editorPanel.isVisible() && editingDefinition[0] != null) {
                    saveEditorDefinition(editorPanel, editingDefinition[0], library, frame);
                }
            });

            configureRegistryForMain(library);
            ComponentBarPanel mainBar = new ComponentBarPanel(mainPanel);
            ComponentBarPanel editorBar = new ComponentBarPanel(editorPanel);
            mainPanel.setComponentBarToggle(mainBar::toggleVisibility);
            editorPanel.setComponentBarToggle(editorBar::toggleVisibility);

            WirePalettePanel mainPalette = new WirePalettePanel(
                    mainPanel.getActiveWireColor(),
                    mainPanel::setActiveWireColor);
            WirePalettePanel editorPalette = new WirePalettePanel(
                    editorPanel.getActiveWireColor(),
                    editorPanel::setActiveWireColor);
            ClearBoardPanel mainClear = new ClearBoardPanel(mainPanel::requestClearBoard);
            ClearBoardPanel editorClear = new ClearBoardPanel(editorPanel::requestClearBoard);

            TempModePanel[] tempModePanelHolder = new TempModePanel[1];
            tempModePanelHolder[0] = new TempModePanel(() -> {
                try {
                    library.clearTempMode();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to clear temp data.",
                            "CircuitSim", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                configureRegistryForMain(library);
                mainBar.refreshGroups();
                mainPanel.setAutosavePath(circuitsim.io.DataPaths.getAutosavePath(), true);
                tempModePanelHolder[0].setVisible(false);
            });
            TempModePanel tempModePanel = tempModePanelHolder[0];
            tempModePanel.setVisible(library.isTempMode());
            if (library.isTempMode()) {
                mainPanel.setAutosavePath(circuitsim.io.DataPaths.getTempAutosavePath(), true);
            } else {
                mainPanel.setAutosavePath(circuitsim.io.DataPaths.getAutosavePath(), false);
            }

            CustomEditorPanel[] editorPanelControlsHolder = new CustomEditorPanel[1];
            editorPanelControlsHolder[0] = new CustomEditorPanel(() -> {
                CustomComponentDefinition updated = saveEditorDefinition(editorPanel, editingDefinition[0],
                        library, frame);
                if (updated == null) {
                    return;
                }
                mainPanel.updateCustomComponentInstances(updated.getId(), updated);
                editingDefinition[0] = null;
                configureRegistryForMain(library);
                mainBar.refreshGroups();
                exitEditor(activePanel, mainPanel, editorPanel, mainPalette, mainClear,
                        mainBar, editorPalette, editorClear, editorBar, editorPanelControlsHolder[0]);
            }, () -> {
                editingDefinition[0] = null;
                configureRegistryForMain(library);
                mainBar.refreshGroups();
                exitEditor(activePanel, mainPanel, editorPanel, mainPalette, mainClear,
                        mainBar, editorPalette, editorClear, editorBar, editorPanelControlsHolder[0]);
            });
            CustomEditorPanel editorPanelControls = editorPanelControlsHolder[0];
            editorPanelControls.setVisible(false);

            mainPanel.setCustomDefinitionsSupplier(library::getDefinitions);
            mainPanel.setCustomDefinitionResolver(library::getDefinition);
            editorPanel.setCustomDefinitionResolver(library::getDefinition);
            editorPanel.setCustomDefinitionsSupplier(java.util.Collections::emptyList);
            editorPanel.setChangeListener(() -> {
                if (!editorPanel.isVisible() || editingDefinition[0] == null) {
                    return;
                }
                CustomComponentDefinition updated = saveEditorDefinition(editorPanel, editingDefinition[0],
                        library, frame);
                if (updated != null) {
                    editingDefinition[0] = updated;
                }
            });

            mainPanel.setBoardLoadTransform(state -> handleBoardLoad(state, library, tempModePanel,
                    mainBar, mainPanel, frame));

            mainPanel.setCustomComponentHandlers(() -> {
                String name = JOptionPane.showInputDialog(frame, "New custom component name:",
                        "New Custom Component", JOptionPane.PLAIN_MESSAGE);
                if (name == null || name.trim().isEmpty()) {
                    return;
                }
                CustomComponentDefinition definition = new CustomComponentDefinition(name.trim(),
                        new ArrayList<>(), new ArrayList<>(),
                        new BoardState(BoardState.CURRENT_VERSION,
                                mainPanel.getActiveWireColor(), new ArrayList<>(), new ArrayList<>()));
                editingDefinition[0] = definition;
                enterEditor(definition, library, activePanel, mainPanel, editorPanel, mainPalette, mainClear,
                        mainBar, editorPalette, editorClear, editorBar, editorPanelControlsHolder[0]);
            }, customId -> {
                CustomComponentDefinition definition = library.getDefinition(customId);
                if (definition == null) {
                    return;
                }
                editingDefinition[0] = definition;
                enterEditor(definition, library, activePanel, mainPanel, editorPanel, mainPalette, mainClear,
                        mainBar, editorPalette, editorClear, editorBar, editorPanelControlsHolder[0]);
            }, customId -> {
                CustomComponentDefinition definition = library.getDefinition(customId);
                if (definition == null) {
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Delete custom component \"" + definition.getName() + "\"?",
                        "Delete Custom Component", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    library.deleteDefinition(customId);
                    removeDeletedCustomComponentUsages(library, definition, mainPanel, editorPanel);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to delete custom component.",
                            "CircuitSim", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                mainPanel.removeCustomComponentInstances(customId);
                if (editingDefinition[0] != null
                        && customId.equals(editingDefinition[0].getId())) {
                    editingDefinition[0] = null;
                    configureRegistryForMain(library);
                    mainBar.refreshGroups();
                    exitEditor(activePanel, mainPanel, editorPanel, mainPalette, mainClear,
                            mainBar, editorPalette, editorClear, editorBar, editorPanelControlsHolder[0]);
                    return;
                }
                configureRegistryForMain(library);
                mainBar.refreshGroups();
            });
            editorPanel.setCustomComponentHandlers(
                    () -> mainPanel.requestCreateCustomComponent(),
                    mainPanel::requestEditCustomComponent,
                    mainPanel::requestDeleteCustomComponent);

            JLayeredPane layeredPane = buildLayeredPane(mainPanel, editorPanel, mainPalette, editorPalette,
                    mainClear, editorClear, mainBar, editorBar, tempModePanel, editorPanelControls);
            frame.add(layeredPane, BorderLayout.CENTER);
            frame.add(propertiesPanel, BorderLayout.EAST);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    if (!editorPanel.isVisible() || editingDefinition[0] == null) {
                        return;
                    }
                    saveEditorDefinition(editorPanel, editingDefinition[0], library, frame);
                }
            });
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
                                                 ClearBoardPanel clearBoardPanel,
                                                 ComponentBarPanel componentBarPanel) {
        return buildLayeredPane(circuitPanel, null, wirePalettePanel, null, clearBoardPanel,
                null, componentBarPanel, null, null, null);
    }

    private static JLayeredPane buildLayeredPane(CircuitPanel mainPanel,
                                                 CircuitPanel editorPanel,
                                                 WirePalettePanel mainPalette,
                                                 WirePalettePanel editorPalette,
                                                 ClearBoardPanel mainClear,
                                                 ClearBoardPanel editorClear,
                                                 ComponentBarPanel mainBar,
                                                 ComponentBarPanel editorBar,
                                                 TempModePanel tempModePanel,
                                                 CustomEditorPanel editorPanelControls) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(mainPanel, Integer.valueOf(0));
        if (editorPanel != null) {
            layeredPane.add(editorPanel, Integer.valueOf(0));
            editorPanel.setVisible(false);
        }
        layeredPane.add(mainPalette, Integer.valueOf(1));
        layeredPane.add(mainClear, Integer.valueOf(1));
        layeredPane.add(mainBar, Integer.valueOf(2));
        if (editorPalette != null) {
            layeredPane.add(editorPalette, Integer.valueOf(1));
            editorPalette.setVisible(false);
        }
        if (editorClear != null) {
            layeredPane.add(editorClear, Integer.valueOf(1));
            editorClear.setVisible(false);
        }
        if (editorBar != null) {
            layeredPane.add(editorBar, Integer.valueOf(2));
            editorBar.setVisible(false);
        }
        if (tempModePanel != null) {
            layeredPane.add(tempModePanel, Integer.valueOf(3));
        }
        if (editorPanelControls != null) {
            layeredPane.add(editorPanelControls, Integer.valueOf(3));
        }
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutFloatingPanels(layeredPane, mainPanel, editorPanel, mainPalette, editorPalette,
                        mainClear, editorClear, mainBar, editorBar, tempModePanel, editorPanelControls);
            }
        });
        return layeredPane;
    }

    /**
     * Positions the palette and clear button near the bottom-left corner.
     */
    private static void layoutFloatingPanels(JLayeredPane layeredPane,
                                             CircuitPanel mainPanel,
                                             CircuitPanel editorPanel,
                                             WirePalettePanel mainPalette,
                                             WirePalettePanel editorPalette,
                                             ClearBoardPanel mainClear,
                                             ClearBoardPanel editorClear,
                                             ComponentBarPanel mainBar,
                                             ComponentBarPanel editorBar,
                                             TempModePanel tempModePanel,
                                             CustomEditorPanel editorPanelControls) {
        int width = layeredPane.getWidth();
        int height = layeredPane.getHeight();
        mainPanel.setBounds(0, 0, width, height);
        if (editorPanel != null) {
            editorPanel.setBounds(0, 0, width, height);
        }

        mainBar.applyLayout(width, 0);
        if (editorBar != null) {
            editorBar.applyLayout(width, 0);
        }

        Dimension paletteSize = mainPalette.getPreferredSize();
        int paletteX = PANEL_PADDING;
        int paletteY = height - paletteSize.height - PANEL_PADDING;
        mainPalette.setBounds(paletteX, Math.max(PANEL_PADDING, paletteY),
                paletteSize.width, paletteSize.height);

        Dimension clearSize = mainClear.getPreferredSize();
        int clearX = paletteX + paletteSize.width + PANEL_PADDING;
        int clearY = height - clearSize.height - PANEL_PADDING;
        mainClear.setBounds(clearX, Math.max(PANEL_PADDING, clearY),
                clearSize.width, clearSize.height);

        if (editorPalette != null) {
            editorPalette.setBounds(paletteX, Math.max(PANEL_PADDING, paletteY),
                    paletteSize.width, paletteSize.height);
        }
        if (editorClear != null) {
            editorClear.setBounds(clearX, Math.max(PANEL_PADDING, clearY),
                    clearSize.width, clearSize.height);
        }
        if (tempModePanel != null) {
            Dimension tempSize = tempModePanel.getPreferredSize();
            tempModePanel.setBounds(width - tempSize.width - PANEL_PADDING, PANEL_PADDING,
                    tempSize.width, tempSize.height);
        }
        if (editorPanelControls != null) {
            Dimension editorSize = editorPanelControls.getPreferredSize();
            int editorY = PANEL_PADDING + (editorBar == null ? 0 : editorBar.getHeight()) + PANEL_PADDING;
            editorPanelControls.setBounds(PANEL_PADDING, editorY,
                    editorSize.width, editorSize.height);
        }
    }

    private static void configureRegistryForMain(CustomComponentLibrary library) {
        ComponentRegistry.ensureGroup("Custom");
        ComponentRegistry.clearGroup("Custom");
        for (CustomComponentDefinition definition : library.getDefinitions()) {
            ComponentRegistry.registerCustom("Custom", definition.getName(), definition.getId(),
                    (x, y) -> new CustomComponent(x, y, definition));
        }
        ComponentRegistry.removeGroup("IO");
    }

    private static void configureRegistryForEditor(CustomComponentLibrary library, String excludeCustomId) {
        ComponentRegistry.ensureGroup("Custom");
        ComponentRegistry.clearGroup("Custom");
        for (CustomComponentDefinition definition : library.getDefinitions()) {
            if (definition.getId().equals(excludeCustomId)) {
                continue;
            }
            ComponentRegistry.registerCustom("Custom", definition.getName(), definition.getId(),
                    (x, y) -> new CustomComponent(x, y, definition));
        }
        ComponentRegistry.ensureGroup("IO");
        ComponentRegistry.clearGroup("IO");
        ComponentRegistry.register("IO", "Input", (x, y) -> new CustomInputPort(x, y));
        ComponentRegistry.register("IO", "Output", (x, y) -> new CustomOutputPort(x, y));
    }

    private static void enterEditor(CustomComponentDefinition definition, CustomComponentLibrary library,
                                    CircuitPanel[] activePanel, CircuitPanel mainPanel, CircuitPanel editorPanel,
                                    WirePalettePanel mainPalette, ClearBoardPanel mainClear,
                                    ComponentBarPanel mainBar, WirePalettePanel editorPalette,
                                    ClearBoardPanel editorClear, ComponentBarPanel editorBar,
                                    CustomEditorPanel editorPanelControls) {
        configureRegistryForEditor(library, definition.getId());
        editorBar.refreshGroups();
        BoardState state = definition.getBoardState();
        if (state == null) {
            state = new BoardState(BoardState.CURRENT_VERSION,
                    mainPanel.getActiveWireColor(), new ArrayList<>(), new ArrayList<>());
        }
        editorPanel.importBoardState(state, true);
        editorPanelControls.setTitleText("Editing: " + definition.getName());
        editorPanelControls.setVisible(true);
        swapPanels(activePanel, mainPanel, editorPanel, mainPalette, mainClear, mainBar,
                editorPalette, editorClear, editorBar, true);
        editorPanel.requestFocusInWindow();
    }

    private static void exitEditor(CircuitPanel[] activePanel, CircuitPanel mainPanel, CircuitPanel editorPanel,
                                   WirePalettePanel mainPalette, ClearBoardPanel mainClear, ComponentBarPanel mainBar,
                                   WirePalettePanel editorPalette, ClearBoardPanel editorClear,
                                   ComponentBarPanel editorBar, CustomEditorPanel editorPanelControls) {
        editorPanelControls.setVisible(false);
        swapPanels(activePanel, mainPanel, editorPanel, mainPalette, mainClear, mainBar,
                editorPalette, editorClear, editorBar, false);
    }

    private static void swapPanels(CircuitPanel[] activePanel, CircuitPanel mainPanel, CircuitPanel editorPanel,
                                   WirePalettePanel mainPalette, ClearBoardPanel mainClear, ComponentBarPanel mainBar,
                                   WirePalettePanel editorPalette, ClearBoardPanel editorClear,
                                   ComponentBarPanel editorBar, boolean editorActive) {
        mainPanel.setVisible(!editorActive);
        mainPalette.setVisible(!editorActive);
        mainClear.setVisible(!editorActive);
        mainBar.setVisible(!editorActive);
        editorPanel.setVisible(editorActive);
        editorPalette.setVisible(editorActive);
        editorClear.setVisible(editorActive);
        editorBar.setVisible(editorActive);
        if (!editorActive) {
            activePanel[0] = mainPanel;
            return;
        }
        activePanel[0] = editorPanel;
    }

    private static CustomComponentDefinition buildDefinitionFromEditor(CircuitPanel editorPanel,
                                                                       CustomComponentDefinition original) {
        if (original == null) {
            return null;
        }
        List<CustomComponentPort> inputs = new ArrayList<>();
        List<CustomComponentPort> outputs = new ArrayList<>();
        for (circuitsim.components.CircuitComponent component : editorPanel.getComponentsSnapshot()) {
            if (component instanceof CustomInputPort) {
                inputs.add(new CustomComponentPort(component.getDisplayName(),
                        CustomComponentPort.Direction.INPUT));
            } else if (component instanceof CustomOutputPort) {
                outputs.add(new CustomComponentPort(component.getDisplayName(),
                        CustomComponentPort.Direction.OUTPUT));
            }
        }
        BoardState boardState = editorPanel.exportBoardState();
        return new CustomComponentDefinition(original.getId(), original.getName(), inputs, outputs, boardState);
    }

    private static BoardState handleBoardLoad(BoardState state, CustomComponentLibrary library,
                                              TempModePanel tempModePanel, ComponentBarPanel mainBar,
                                              CircuitPanel mainPanel, JFrame frame) {
        if (state == null || state.getCustomComponents().isEmpty()) {
            return state;
        }
        String[] options = new String[] { "Add to Local", "Use Temp" };
        int choice = JOptionPane.showOptionDialog(frame,
                "This board contains custom components. Where should they be stored?",
                "Custom Components", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        try {
            if (choice == 0) {
                library.mergeIntoLocal(state.getCustomComponents());
                if (mainPanel != null) {
                    mainPanel.setAutosavePath(circuitsim.io.DataPaths.getAutosavePath(), false);
                }
            } else {
                library.activateTempMode(state.getCustomComponents());
                if (mainPanel != null) {
                    mainPanel.setAutosavePath(circuitsim.io.DataPaths.getTempAutosavePath(), false);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to load custom components.",
                    "CircuitSim", JOptionPane.ERROR_MESSAGE);
        }
        configureRegistryForMain(library);
        mainBar.refreshGroups();
        tempModePanel.setVisible(library.isTempMode());
        return state;
    }

    private static CustomComponentDefinition saveEditorDefinition(CircuitPanel editorPanel,
                                                                  CustomComponentDefinition original,
                                                                  CustomComponentLibrary library,
                                                                  JFrame frame) {
        CustomComponentDefinition updated = buildDefinitionFromEditor(editorPanel, original);
        if (updated == null) {
            return null;
        }
        try {
            library.saveDefinition(updated);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to auto-save custom component.",
                    "CircuitSim", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return updated;
    }

    private static void removeDeletedCustomComponentUsages(CustomComponentLibrary library,
                                                           CustomComponentDefinition removedDefinition,
                                                           CircuitPanel mainPanel,
                                                           CircuitPanel editorPanel) throws IOException {
        if (library == null || removedDefinition == null) {
            return;
        }
        List<CustomComponentDefinition> definitions = library.getDefinitions();
        for (CustomComponentDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            CustomComponentDefinition updated = stripNestedCustomComponent(definition, removedDefinition);
            if (updated == definition) {
                continue;
            }
            library.saveDefinition(updated);
            if (mainPanel != null) {
                mainPanel.updateCustomComponentInstances(definition.getId(), updated);
            }
            if (editorPanel != null) {
                editorPanel.updateCustomComponentInstances(definition.getId(), updated);
            }
        }
    }

    private static CustomComponentDefinition stripNestedCustomComponent(CustomComponentDefinition definition,
                                                                        CustomComponentDefinition removedDefinition) {
        BoardState state = definition.getBoardState();
        if (state == null) {
            return definition;
        }
        List<BoardState.ComponentState> components = new ArrayList<>();
        java.util.Set<java.awt.Point> removedPoints = new java.util.HashSet<>();
        boolean removedAny = false;
        for (BoardState.ComponentState componentState : state.getComponents()) {
            if ("Custom".equals(componentState.getType())
                    && removedDefinition.getId().equals(componentState.getCustomId())) {
                removedAny = true;
                removedPoints.addAll(getCustomComponentConnectionPoints(componentState, removedDefinition));
                continue;
            }
            components.add(componentState);
        }
        if (!removedAny) {
            return definition;
        }
        List<BoardState.WireState> wires = new ArrayList<>();
        for (BoardState.WireState wireState : state.getWires()) {
            java.awt.Point start = new java.awt.Point(wireState.getStartX(), wireState.getStartY());
            java.awt.Point end = new java.awt.Point(wireState.getEndX(), wireState.getEndY());
            if (removedPoints.contains(start) || removedPoints.contains(end)) {
                continue;
            }
            wires.add(wireState);
        }
        BoardState nextState = new BoardState(state.getVersion(), state.getActiveWireColor(),
                components, wires, state.getCustomComponents());
        return new CustomComponentDefinition(definition.getId(), definition.getName(),
                definition.getInputs(), definition.getOutputs(), nextState);
    }

    private static java.util.Set<java.awt.Point> getCustomComponentConnectionPoints(
            BoardState.ComponentState componentState, CustomComponentDefinition definition) {
        java.util.Set<java.awt.Point> points = new java.util.HashSet<>();
        if (componentState == null || definition == null) {
            return points;
        }
        circuitsim.components.CustomComponent shell =
                new circuitsim.components.CustomComponent(componentState.getX(), componentState.getY(), definition);
        if (componentState.getWidth() > 0 && componentState.getHeight() > 0) {
            shell.setSize(componentState.getWidth(), componentState.getHeight());
        }
        shell.setRotationQuarterTurns(componentState.getRotationQuarterTurns());
        for (circuitsim.components.ConnectionPoint point : shell.getConnectionPoints()) {
            points.add(new java.awt.Point(shell.getConnectionPointWorldX(point),
                    shell.getConnectionPointWorldY(point)));
        }
        return points;
    }
}
