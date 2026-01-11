package circuitsim.ui;

public final class Grid {
    public static final int SIZE = 30;

    private Grid() {
    }

    public static int snap(int value) {
        return Math.round(value / (float) SIZE) * SIZE;
    }

    public static int snapSize(int value) {
        int snapped = Math.round(value / (float) SIZE) * SIZE;
        return Math.max(SIZE, snapped);
    }
}
