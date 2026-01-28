package circuitsim.ui;

import circuitsim.components.wiring.Wire;
import circuitsim.components.wiring.WireNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes wire render geometry (parallel offsets and crossing markers).
 */
final class WireRenderPlanner {
    private WireRenderPlanner() {
    }

    static List<RenderWire> buildRenderWires(List<Wire> wires, int wireOffsetStep) {
        List<RenderWire> renderWires = new ArrayList<>();
        Map<Wire, Offset> offsets = computeWireOffsets(wires, wireOffsetStep);
        Map<WireNode, Offset> nodeOffsets = computeNodeOffsets(wires, offsets);
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            Offset wireOffset = offsets.getOrDefault(wire, new Offset(0, 0));
            Offset startOffset = resolveEndpointOffset(wire, wire.getStart(), true, wireOffset, offsets, nodeOffsets);
            Offset endOffset = resolveEndpointOffset(wire, wire.getEnd(), false, wireOffset, offsets, nodeOffsets);
            int baseStartX = wire.getStart().getX();
            int baseStartY = wire.getStart().getY();
            int baseEndX = wire.getEnd().getX();
            int baseEndY = wire.getEnd().getY();
            int startX = baseStartX + startOffset.dx;
            int startY = baseStartY + startOffset.dy;
            int endX = baseEndX + endOffset.dx;
            int endY = baseEndY + endOffset.dy;
            renderWires.add(new RenderWire(wire, startX, startY, endX, endY,
                    baseStartX, baseStartY, baseEndX, baseEndY));
        }
        return renderWires;
    }

    static Map<Wire, List<WireCrossing>> computeWireCrossings(List<RenderWire> renderWires) {
        Map<Wire, List<WireCrossing>> crossings = new HashMap<>();
        Map<Wire, Set<PointKey>> seen = new HashMap<>();
        for (int i = 0; i < renderWires.size(); i++) {
            RenderWire first = renderWires.get(i);
            for (int j = i + 1; j < renderWires.size(); j++) {
                RenderWire second = renderWires.get(j);
                if (sharesNode(first.wire, second.wire)) {
                    continue;
                }
                if (areColinear(first, second)) {
                    continue;
                }
                java.awt.Point intersection = getIntersectionPoint(first, second);
                if (intersection == null) {
                    continue;
                }
                RenderWire overWire = chooseOverWire(first, second);
                PointKey key = new PointKey(intersection.x, intersection.y);
                Set<PointKey> seenPoints = seen.computeIfAbsent(overWire.wire, wire -> new HashSet<>());
                if (seenPoints.add(key)) {
                    crossings.computeIfAbsent(overWire.wire, wire -> new ArrayList<>())
                            .add(new WireCrossing(intersection.x, intersection.y, overWire.angle));
                }
            }
        }
        return crossings;
    }

    private static RenderWire chooseOverWire(RenderWire first, RenderWire second) {
        boolean firstVertical = isMostlyVertical(first);
        boolean secondVertical = isMostlyVertical(second);
        if (firstVertical != secondVertical) {
            return firstVertical ? first : second;
        }
        return second;
    }

    private static boolean isMostlyVertical(RenderWire wire) {
        int dx = Math.abs(wire.x2 - wire.x1);
        int dy = Math.abs(wire.y2 - wire.y1);
        return dy > dx;
    }

    private static boolean sharesNode(Wire first, Wire second) {
        if (first == null || second == null) {
            return false;
        }
        WireNode aStart = first.getStart();
        WireNode aEnd = first.getEnd();
        WireNode bStart = second.getStart();
        WireNode bEnd = second.getEnd();
        return (aStart != null && (aStart == bStart || aStart == bEnd))
                || (aEnd != null && (aEnd == bStart || aEnd == bEnd));
    }

    private static boolean areColinear(RenderWire first, RenderWire second) {
        long dx1 = first.x2 - first.x1;
        long dy1 = first.y2 - first.y1;
        long dx2 = second.x2 - second.x1;
        long dy2 = second.y2 - second.y1;
        return (dx1 * dy2) - (dy1 * dx2) == 0;
    }

    private static java.awt.Point getIntersectionPoint(RenderWire first, RenderWire second) {
        double x1 = first.x1;
        double y1 = first.y1;
        double x2 = first.x2;
        double y2 = first.y2;
        double x3 = second.x1;
        double y3 = second.y1;
        double x4 = second.x2;
        double y4 = second.y2;
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 0.0001) {
            return null;
        }
        double det1 = (x1 * y2) - (y1 * x2);
        double det2 = (x3 * y4) - (y3 * x4);
        double px = (det1 * (x3 - x4) - (x1 - x2) * det2) / denom;
        double py = (det1 * (y3 - y4) - (y1 - y2) * det2) / denom;
        if (!isWithinSegment(px, py, x1, y1, x2, y2)
                || !isWithinSegment(px, py, x3, y3, x4, y4)) {
            return null;
        }
        return new java.awt.Point((int) Math.round(px), (int) Math.round(py));
    }

    private static boolean isWithinSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2) - 0.1;
        double maxX = Math.max(x1, x2) + 0.1;
        double minY = Math.min(y1, y2) - 0.1;
        double maxY = Math.max(y1, y2) + 0.1;
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }

    private static Map<Wire, Offset> computeWireOffsets(List<Wire> wires, int wireOffsetStep) {
        Map<Wire, Offset> offsets = new HashMap<>();
        List<List<Wire>> groups = new ArrayList<>();
        for (Wire wire : wires) {
            if (wire.getStart() == null || wire.getEnd() == null) {
                continue;
            }
            List<Wire> targetGroup = null;
            for (List<Wire> group : groups) {
                if (!group.isEmpty() && areColinearOverlap(wire, group.get(0))) {
                    targetGroup = group;
                    break;
                }
            }
            if (targetGroup == null) {
                targetGroup = new ArrayList<>();
                groups.add(targetGroup);
            }
            targetGroup.add(wire);
        }
        for (List<Wire> group : groups) {
            if (group.size() <= 1) {
                Wire wire = group.get(0);
                offsets.put(wire, new Offset(0, 0));
                continue;
            }
            Wire base = group.get(0);
            int dx = base.getEnd().getX() - base.getStart().getX();
            int dy = base.getEnd().getY() - base.getStart().getY();
            double length = Math.hypot(dx, dy);
            if (length == 0) {
                for (Wire wire : group) {
                    offsets.put(wire, new Offset(0, 0));
                }
                continue;
            }
            double normalX = -dy / length;
            double normalY = dx / length;
            int count = group.size();
            for (int i = 0; i < count; i++) {
                double index = i - (count - 1) / 2.0;
                int offsetX = (int) Math.round(normalX * index * wireOffsetStep);
                int offsetY = (int) Math.round(normalY * index * wireOffsetStep);
                offsets.put(group.get(i), new Offset(offsetX, offsetY));
            }
        }
        return offsets;
    }

    private static Map<WireNode, Offset> computeNodeOffsets(List<Wire> wires, Map<Wire, Offset> wireOffsets) {
        Map<WireNode, OffsetAccumulator> accumulators = new HashMap<>();
        for (Wire wire : wires) {
            Offset offset = wireOffsets.getOrDefault(wire, new Offset(0, 0));
            if (offset.isZero()) {
                continue;
            }
            addOffsetToNode(accumulators, wire.getStart(), offset);
            addOffsetToNode(accumulators, wire.getEnd(), offset);
        }
        Map<WireNode, Offset> nodeOffsets = new HashMap<>();
        for (Map.Entry<WireNode, OffsetAccumulator> entry : accumulators.entrySet()) {
            OffsetAccumulator accumulator = entry.getValue();
            if (accumulator.count == 0) {
                continue;
            }
            int dx = Math.round(accumulator.sumDx / (float) accumulator.count);
            int dy = Math.round(accumulator.sumDy / (float) accumulator.count);
            nodeOffsets.put(entry.getKey(), new Offset(dx, dy));
        }
        return nodeOffsets;
    }

    private static Offset resolveEndpointOffset(Wire wire, WireNode node, boolean isStart, Offset wireOffset,
            Map<Wire, Offset> wireOffsets, Map<WireNode, Offset> nodeOffsets) {
        if (!wireOffset.isZero()) {
            return wireOffset;
        }
        Wire anchor = isStart ? wire.getStartAnchorWire() : wire.getEndAnchorWire();
        if (anchor != null) {
            Offset anchorOffset = wireOffsets.getOrDefault(anchor, new Offset(0, 0));
            if (!anchorOffset.isZero()) {
                return anchorOffset;
            }
        }
        return nodeOffsets.getOrDefault(node, wireOffset);
    }

    private static void addOffsetToNode(Map<WireNode, OffsetAccumulator> accumulators, WireNode node, Offset offset) {
        if (node == null) {
            return;
        }
        OffsetAccumulator accumulator = accumulators.get(node);
        if (accumulator == null) {
            accumulator = new OffsetAccumulator();
            accumulators.put(node, accumulator);
        }
        accumulator.sumDx += offset.dx;
        accumulator.sumDy += offset.dy;
        accumulator.count++;
    }

    private static boolean areColinearOverlap(Wire first, Wire second) {
        if (!areColinear(first, second)) {
            return false;
        }
        if (sharesNode(first, second)) {
            return false;
        }
        return segmentsOverlap(first, second);
    }

    private static boolean areColinear(Wire first, Wire second) {
        int x1 = first.getStart().getX();
        int y1 = first.getStart().getY();
        int x2 = first.getEnd().getX();
        int y2 = first.getEnd().getY();
        int x3 = second.getStart().getX();
        int y3 = second.getStart().getY();
        int x4 = second.getEnd().getX();
        int y4 = second.getEnd().getY();
        long dx1 = x2 - x1;
        long dy1 = y2 - y1;
        long dx3 = x3 - x1;
        long dy3 = y3 - y1;
        long dx4 = x4 - x1;
        long dy4 = y4 - y1;
        return (dx1 * dy3) - (dy1 * dx3) == 0 && (dx1 * dy4) - (dy1 * dx4) == 0;
    }

    private static boolean segmentsOverlap(Wire first, Wire second) {
        int x1 = first.getStart().getX();
        int y1 = first.getStart().getY();
        int x2 = first.getEnd().getX();
        int y2 = first.getEnd().getY();
        int x3 = second.getStart().getX();
        int y3 = second.getStart().getY();
        int x4 = second.getEnd().getX();
        int y4 = second.getEnd().getY();
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = (dx * dx) + (dy * dy);
        if (lengthSq == 0) {
            return false;
        }
        double t1 = ((x3 - x1) * dx + (y3 - y1) * dy) / lengthSq;
        double t2 = ((x4 - x1) * dx + (y4 - y1) * dy) / lengthSq;
        double minA = Math.min(0, 1);
        double maxA = Math.max(0, 1);
        double minB = Math.min(t1, t2);
        double maxB = Math.max(t1, t2);
        return maxB > minA && minB < maxA;
    }
}

