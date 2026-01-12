package circuitsim.io;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves OS-specific data directories for CircuitSim.
 */
public final class DataPaths {
    private static final String DATA_DIR_NAME = "CircuitSimData";

    private DataPaths() {
    }

    /**
     * @return base data directory for the current OS
     */
    public static Path getBaseDataDir() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", "");
        if (osName.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path baseDir = localAppData == null || localAppData.isEmpty()
                    ? Paths.get(userHome, "AppData", "Local")
                    : Paths.get(localAppData);
            return baseDir.resolve(DATA_DIR_NAME);
        }
        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", DATA_DIR_NAME);
        }
        return Paths.get(userHome, ".local", "share", DATA_DIR_NAME);
    }

    /**
     * @return autosave file path
     */
    public static Path getAutosavePath() {
        return getBaseDataDir().resolve("autosave.json");
    }

    /**
     * @return autosave file path for temp mode
     */
    public static Path getTempAutosavePath() {
        return getTempDataDir().resolve("autosave.json");
    }

    /**
     * @return directory holding custom component definitions
     */
    public static Path getCustomComponentsDir() {
        return getBaseDataDir().resolve("CustomComponents");
    }

    /**
     * @return directory holding temporary data
     */
    public static Path getTempDataDir() {
        return getBaseDataDir().resolve("TempData");
    }
}
