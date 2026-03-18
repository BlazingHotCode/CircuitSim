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

4. Copy `packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml` into your `flathub/flathub` submission branch root and rename it to `com.blazinghotcode.CircuitSim.yml` there.

The Flathub manifest now fetches the desktop file, metainfo, and icon from the upstream `CircuitSim` repository and uses a manifest `type: script` source for the launcher, so no extra packaging files need to be copied into the submission repo.

## Local validation

```sh
flatpak run --command=flathub-build org.flatpak.Builder --install packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml
flatpak run com.blazinghotcode.CircuitSim
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml
```
