# CircuitSim

Interactive, real-time circuit simulator built with Java Swing. Design circuits on a grid, wire them up,
and watch the simulation update live (analog voltages/currents + basic digital logic gates).

CircuitSim is designed as a “sandbox” editor: you place components from a palette, connect them with
wires, then the solver continuously recomputes values as you move/rotate/edit the circuit.

## Features

- **Analog simulation**: continuous DC-style solver for voltages/currents with short-circuit detection.
- **Digital logic simulation**: built-in logic gates (NAND/AND/OR/XOR/NOT) driving wires as 0V/5V signals.
- **Custom components**: dedicated editor with input/output ports, nesting, and local + temporary libraries.
- **Editor UX**: grid-snapped placement, multi-select, rotate, resize, pan, zoom, context menus.
- **Component + wire palettes**: grouped components, drag-and-drop placement, selectable wire colors.
- **Properties panel**: edit component parameters and titles in real time.
- **Persistence**: save/load JSON boards, autosave, undo/redo, temp custom-component mode.

## Requirements

- JDK 21+

If you're using VS Code, point the Java extension at a JDK 21 install (workspace defaults live in `.vscode/settings.json`).

## Quick Start

**Run the prebuilt JAR**

```sh
java -jar dist/CircuitSim-<version>.jar
```

**Run from source**

```sh
javac --release 21 -d out $(find src/main/java -name "*.java")
java -cp out circuitsim.CircuitSim
```

**Build a runnable JAR**

```sh
rm -rf out && mkdir -p out
javac --release 21 -d out $(find src/main/java -name "*.java")
jar cfm dist/CircuitSim.jar dist/manifest.txt -C out .
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
- Logic: NAND, AND, OR, XOR, NOT.
- Passive: Resistor.
- Meters: Voltmeter, Ammeter.
- Controls: Switch.
- Reference: Ground.

**Custom-component editor**
- Ports: Input Port, Output Port (used for defining a custom component’s interface).

**Custom components**
- Create, edit, and delete custom components from the Custom group.
- Editor mode adds Input/Output ports for defining custom interfaces.
- Custom components can be nested and reused like built-ins.

## Digital Logic Notes

- Logic gates output either ~0V (LOW) or ~5V (HIGH) with a ~2.5V threshold.
- Gate outputs drive connected wires; those wires are marked as “logic powered” internally so logic can
  propagate even when the analog solver would otherwise show ~0 current.
- Logic inputs enforce a single-wire-per-input connection rule to prevent ambiguous fan-in at the pin.

## Custom Components And Temp Mode

When you load a board that includes custom components, CircuitSim asks where to store them:
- Add to Local: saves definitions in your local library.
- Use Temp: keeps definitions in a temporary library until you exit temp mode.

Use the "Go Back To Save" button to exit temp mode and return to local storage.

## Saves And Data Locations

**Board files**
- Saved/loaded as JSON via the file chooser.
- Example boards live in `saves/` (try loading `saves/1.json` or `saves/2.json`).

**Autosave**
- Windows: `%LOCALAPPDATA%\CircuitSimData\autosave.json`
- macOS: `~/Library/Application Support/CircuitSimData/autosave.json`
- Linux: `~/.local/share/CircuitSimData/autosave.json`

**Custom component storage**
- Local: `.../CircuitSimData/CustomComponents`
- Temp mode: `.../CircuitSimData/TempData`

## Project Structure

- `src/main/java/circuitsim/ui`: Swing UI (canvas, palettes, properties panel, custom editor UI).
- `src/main/java/circuitsim/components`: Component types (electrical, instruments, wiring, logic, ports).
- `src/main/java/circuitsim/physics`: Simulation code (analog solver + logic update pass).
- `src/main/java/circuitsim/io` and `src/main/java/circuitsim/custom`: JSON persistence + custom-component library.

## License

Apache License 2.0. See `LICENSE`.
