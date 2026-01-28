package circuitsim.ui;

import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireNode;

/**
 * Result of splitting a wire, including the new node and anchor.
 */
final class WireSplitResult {
    final WireNode node;
    final Wire anchorWire;

    WireSplitResult(WireNode node, Wire anchorWire) {
        this.node = node;
        this.anchorWire = anchorWire;
    }
}

