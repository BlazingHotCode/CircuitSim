package circuitsim.ui;

/**
 * Offset for drawing parallel wires.
 */
final class Offset {
    final int dx;
    final int dy;

    Offset(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    boolean isZero() {
        return dx == 0 && dy == 0;
    }
}

