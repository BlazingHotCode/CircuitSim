package circuitsim.ui;

import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireNode;

/**
 * Hit result for a wire endpoint.
 */
final class WireEndpointHit {
    final Wire wire;
    final WireNode node;

    WireEndpointHit(Wire wire, WireNode node) {
        this.wire = wire;
        this.node = node;
    }
}

