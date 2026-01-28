package circuitsim.ui;

import circuitsim.components.wiring.Wire;

/**
 * Wire render metadata with precomputed angle.
 */
final class RenderWire {
    final Wire wire;
    final int x1;
    final int y1;
    final int x2;
    final int y2;
    final int baseX1;
    final int baseY1;
    final int baseX2;
    final int baseY2;
    final double angle;

    RenderWire(Wire wire, int x1, int y1, int x2, int y2,
               int baseX1, int baseY1, int baseX2, int baseY2) {
        this.wire = wire;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.baseX1 = baseX1;
        this.baseY1 = baseY1;
        this.baseX2 = baseX2;
        this.baseY2 = baseY2;
        this.angle = Math.atan2(y2 - y1, x2 - x1);
    }
}

