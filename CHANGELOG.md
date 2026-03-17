# Changelog

## 1.1.8 - 2026-03-17

### Changed
- Linux GitHub Release assets now publish separate `x86_64` and `arm64` package variants.
- Flatpak builds now bundle a minimal Java runtime and use a more reliable Linux display launcher path.
- Short-circuit warnings are positioned away from the wire color palette.

## 1.1.5 - 2026-03-15

### Added
- Linux releases now include `.rpm`, portable `.tar.gz`, and Flatpak packaging alongside the existing bundled-runtime formats.
- Added a Flathub-ready Flatpak manifest and helper script for updating its release jar URL and checksum.
- GitHub Releases now publish the versioned application jar needed by Flathub.

### Changed
- Added a reusable release-notes template with install and update instructions for every supported package format.

## 1.1.4 - 2026-03-15

### Added
- Windows installers now upgrade existing CircuitSim installs in place instead of requiring a manual uninstall.
- Windows packaged builds now bundle the app icon into the running launcher and Start menu metadata.
- GitHub Releases can now be published automatically from version tags using the matching release notes file.

### Changed
- Windows Start menu shortcuts are now grouped under `BlazingHotCode` instead of `Unknown`.
- GitHub Release descriptions now come only from `release-notes/<version>.md`.

## 1.1.3 - 2026-01-28

### Changed
- Internal refactor to improve maintainability (CircuitPanel and CircuitSim structure).
- No intended user-facing behavior changes.

## 1.1.2 - 2026-01-27

### Added
- Double-click a custom component to open its editor.
- Wires attached to connection points now follow components when moved/resized/rotated.
- Hold Shift while dragging a component to move it without dragging attached wires.

### Fixed
- Custom-component editor output indicators now update reliably (no “click to refresh” behavior).
- SR latch / flip-flop behavior in custom components is more stable on startup.
