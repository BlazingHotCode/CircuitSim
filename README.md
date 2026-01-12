# CircuitSim

Interactive, real-time circuit simulator built with Java Swing. Design circuits on a grid, wire them up,
and watch live current flow with short-circuit detection and custom component support.

## Features

- Powerful custom component system with a dedicated editor, input/output ports, nesting, and local/temp libraries.
- Real-time circuit solver with current/voltage updates and short-circuit detection.
- Drag-and-drop component palette with grouped components and custom components.
- Wire palette with selectable colors plus optional current labels.
- Grid-snapped placement, multi-select, rotate, resize, and move tools.
- Properties panel for editing component parameters and titles.
- Save/load JSON files, autosave, undo/redo, and temporary custom-component mode.

## Requirements

- JDK 11+

## Run

```sh
javac -d out $(find src/main/java -name "*.java")
java -cp out circuitsim.CircuitSim
```

## How To Use

1. Open the component bar (Tab) and pick a component group.
2. Click a component to place it, or drag it from the dropdown onto the canvas.
3. Click a connection point and drag to another point to create a wire.
4. Use the properties panel (right side) to edit values like resistance, voltage, and labels.
5. Save your circuit with Ctrl+S and load with Ctrl+O.

## Controls

**Mouse**
- Left click: select components/wires.
- Drag: move components, wires, or a multi-selection.
- Drag empty space: box-select multiple items.
- Right drag: pan the view.
- Right click: context menu (add/delete, wire data toggle).
- Double click wire: delete wire.
- Drag resize handle: resize component.
- Click rotate handle or press R: rotate selection.
- Ctrl + mouse wheel: zoom in/out.
- Shift while starting a wire: lock wire creation and finish with a click.

**Keyboard**
- Ctrl+S: save board (JSON).
- Ctrl+O: load board (JSON).
- Ctrl+Z: undo.
- Ctrl+Shift+Z: redo.
- Delete/Backspace: delete selection.
- Arrow keys: move selection by one grid step.
- R: rotate selection.
- Ctrl++ / Ctrl+-: zoom in/out.
- F1: reset view.
- F2: clear board.
- Tab: toggle component bar.

## Components

**Built-in groups**
- Sources: Battery, Source (toggleable).
- Passive: Resistor.
- Meters: Voltmeter, Ammeter.
- Controls: Switch.
- Reference: Ground.

**Custom components**
- Create, edit, and delete custom components from the Custom group.
- Editor mode adds Input/Output ports for defining custom interfaces.
- Custom components can be nested and reused like built-ins.

## Custom Components And Temp Mode

When you load a board that includes custom components, CircuitSim asks where to store them:
- Add to Local: saves definitions in your local library.
- Use Temp: keeps definitions in a temporary library until you exit temp mode.

Use the "Go Back To Save" button to exit temp mode and return to local storage.

## Saves And Data Locations

**Board files**
- Saved/loaded as JSON via the file chooser.

**Autosave**
- Windows: `%LOCALAPPDATA%\CircuitSimData\autosave.json`
- macOS: `~/Library/Application Support/CircuitSimData/autosave.json`
- Linux: `~/.local/share/CircuitSimData/autosave.json`

**Custom component storage**
- Local: `.../CircuitSimData/CustomComponents`
- Temp mode: `.../CircuitSimData/TempData`

## License

Apache License 2.0. See `LICENSE`.
