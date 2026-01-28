package circuitsim.ui;

import circuitsim.components.wiring.Wire;

/**
 * Hit result for a wire segment.
 */
final class WireHit {
    final Wire wire;

    WireHit(Wire wire) {
        this.wire = wire;
    }
}

