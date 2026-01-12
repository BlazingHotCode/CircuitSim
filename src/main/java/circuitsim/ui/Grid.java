package circuitsim.ui;

/**
 * Utilities for snapping coordinates to the grid.
 */
public final class Grid {
    public static final int SIZE = 30;

    /**
     * Prevent instantiation.
     */
    private Grid() {
    }

    /**
     * Snaps a value to the nearest grid point.
     */
    public static int snap(int value) {
        return Math.round(value / (float) SIZE) * SIZE;
    }

    /**
     * Snaps a size to the grid with a minimum size of one grid cell.
     */
    public static int snapSize(int value) {
        int snapped = Math.round(value / (float) SIZE) * SIZE;
        return Math.max(SIZE, snapped);
    }
}
