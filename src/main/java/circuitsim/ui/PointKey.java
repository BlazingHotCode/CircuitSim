package circuitsim.ui;

/**
 * Key used to avoid duplicate crossings.
 */
final class PointKey {
    private final int x;
    private final int y;

    PointKey(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PointKey other)) {
            return false;
        }
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return (31 * x) + y;
    }
}

