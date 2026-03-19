# Flathub Submission Notes

Use `packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml` for Flathub.

It differs from the local bundle manifest because it downloads a pinned GitHub source archive and builds `CircuitSim.jar` during the Flatpak build instead of using a workspace-local jar.

Both manifests build a minimal Java runtime with `jlink` during the Flatpak build, so the resulting app does not depend on the OpenJDK SDK extension at launch time.

## Before submitting

1. Build the versioned jar:

```sh
./scripts/build-jar.sh
```

2. From the commit you want Flathub to build, update the Flathub manifest source archive URL and checksum:

```sh
./scripts/update-flathub-manifest.sh
```

3. Push that commit to GitHub before submitting the Flathub manifest update so the pinned archive URL is reachable.

4. Copy `packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml` into your `flathub/flathub` submission branch root and rename it to `io.github.BlazingHotCode.CircuitSim.yml` there.

The Flathub manifest now fetches the desktop file, metainfo, and icon from the upstream `CircuitSim` repository and uses a manifest `type: script` source for the launcher, so no extra packaging files need to be copied into the submission repo.

## Local validation

```sh
flatpak run --command=flathub-build org.flatpak.Builder --install packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml
flatpak run io.github.BlazingHotCode.CircuitSim
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml
```
