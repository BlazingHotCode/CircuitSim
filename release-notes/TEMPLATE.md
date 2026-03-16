# CircuitSim <version>

Date: YYYY-MM-DD

## Highlights
- Add the main user-facing improvements here.
- Keep this section short and release-focused.

## Download / Update
- Windows installer: download and run the latest `.exe` installer to install or update in place.
- Linux Debian/Ubuntu (`.deb`) install:
  ```sh
  sudo apt install ./circuitsim_<version>-1_<deb-arch>.deb
  ```
- Linux Debian/Ubuntu (`.deb`) update:
  ```sh
  sudo apt install ./circuitsim_<version>-1_<deb-arch>.deb
  ```
- Linux Fedora/RHEL/openSUSE (`.rpm`) install:
  ```sh
  sudo rpm -Uvh ./circuitsim-<version>-1.<rpm-arch>.rpm
  ```
- Linux Fedora/RHEL/openSUSE (`.rpm`) update:
  ```sh
  sudo rpm -Uvh ./circuitsim-<version>-1.<rpm-arch>.rpm
  ```
- Linux portable (`.tar.gz`): extract the archive and run `CircuitSim` from the unpacked folder.
  ```sh
  tar -xzf CircuitSim-linux-portable-<version>-<archive-arch>.tar.gz
  ./CircuitSim/bin/CircuitSim
  ```
- Linux Flatpak install:
  ```sh
  flatpak install --user ./CircuitSim-linux-<version>-<archive-arch>.flatpak
  ```
- Linux Flatpak update:
  ```sh
  flatpak update com.blazinghotcode.CircuitSim
  ```

## Notes
- Add any packaging, migration, or compatibility notes here.
- Common architecture values: `<deb-arch>` is `amd64` or `arm64`, `<rpm-arch>` is `x86_64` or `aarch64`, and `<archive-arch>` is `x86_64` or `arm64`.
