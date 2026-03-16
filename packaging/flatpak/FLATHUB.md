# Flathub Submission Notes

Use `packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml` for Flathub.

It differs from the local bundle manifest because it downloads the versioned GitHub release jar instead of using a workspace-local file.

Both manifests build a minimal Java runtime with `jlink` during the Flatpak build, so the resulting app does not depend on the OpenJDK SDK extension at launch time.

## Before submitting

1. Build the versioned jar:

```sh
./scripts/build-jar.sh
```

2. Update the Flathub manifest URL and checksum for the current version:

```sh
./scripts/update-flathub-manifest.sh
```

3. Make sure the matching GitHub Release includes `CircuitSim-<version>.jar`.

4. Copy these files into your `flathub/flathub` submission branch under `com.blazinghotcode.CircuitSim/`:

- `packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml` (rename to `com.blazinghotcode.CircuitSim.yml` there)
- `packaging/flatpak/com.blazinghotcode.CircuitSim.desktop`
- `packaging/flatpak/com.blazinghotcode.CircuitSim.metainfo.xml`
- `packaging/flatpak/circuitsim.sh`
- `packaging/circuitsim.png`

## Local validation

```sh
flatpak run --command=flathub-build org.flatpak.Builder --install packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml
flatpak run com.blazinghotcode.CircuitSim
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml
```
