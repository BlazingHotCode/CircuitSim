package circuitsim.components.electrical;

import circuitsim.components.core.BuiltinComponent;
import circuitsim.components.core.CircuitComponent;
import circuitsim.components.core.ConnectionPoint;
import circuitsim.components.properties.ComputedFloatProperty;
import circuitsim.components.properties.FloatProperty;
import circuitsim.ui.Colors;
import circuitsim.ui.Grid;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Point2D;

/**
 * 3-terminal variable resistor (potentiometer).
 * Left/right are the fixed ends, top is the wiper terminal.
 */
@BuiltinComponent(group = "Passive", paletteName = "Variable Resistor", aliases = { "SlidingResistor" },
        groupOrder = 30, paletteOrder = 40)
public class VariableResistor extends CircuitComponent {
    private static final int BASE_WIDTH = Grid.SIZE * 4;
    private static final int BASE_HEIGHT = Grid.SIZE * 2;
    private static final float DEFAULT_OHMS = 10f;

    private static final int SLIDER_HIT_RADIUS = 16;
    private static final int HANDLE_GAP = 0;

    private float resistance;
    private float wiperPosition = 0.5f; // 0..1

    private float computedVoltage;
    private float computedAmpere;
    private float computedPowerWatt;

    public VariableResistor(int x, int y) {
        this(x, y, DEFAULT_OHMS);
    }

    public VariableResistor(int x, int y, float resistance) {
        super(x, y, BASE_HEIGHT, BASE_WIDTH, 3);
        this.resistance = resistance;
        addConnectionPoint(0f, 0.5f);   // left
        addConnectionPoint(1f, 0.5f);   // right
        addConnectionPoint(0.5f, 0f);   // top (wiper terminal)
        addProperty(new FloatProperty("Resistance (Ω)", this::getResistance, this::setResistance, true));
        addProperty(new FloatProperty("Wiper (0..1)", this::getWiperPosition, this::setWiperPosition, true));
        addProperty(new ComputedFloatProperty("Voltage (V)", this::getComputedVoltage, false));
        addProperty(new ComputedFloatProperty("Ampere (A)", this::getComputedAmpere, false));
        addProperty(new ComputedFloatProperty("Power (W)", this::getComputedPowerWatt, false));
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        snapWiperToGrid();
    }

    public float getResistance() {
        return resistance;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    public float getWiperPosition() {
        return wiperPosition;
    }

    public void setWiperPosition(float wiperPosition) {
        if (Float.isNaN(wiperPosition) || Float.isInfinite(wiperPosition)) {
            return;
        }
        this.wiperPosition = Math.max(0f, Math.min(1f, wiperPosition));
        snapWiperToGrid();
    }

    public float getComputedVoltage() {
        return computedVoltage;
    }

    public void setComputedVoltage(float computedVoltage) {
        this.computedVoltage = computedVoltage;
    }

    public float getComputedAmpere() {
        return computedAmpere;
    }

    public void setComputedAmpere(float computedAmpere) {
        this.computedAmpere = computedAmpere;
    }

    public float getComputedPowerWatt() {
        return computedPowerWatt;
    }

    public void setComputedPowerWatt(float computedPowerWatt) {
        this.computedPowerWatt = computedPowerWatt;
    }

    @Override
    public boolean allowFullRotation() {
        return true;
    }

    @Override
    public void rotate90() {
        super.rotate90();
        snapWiperToGrid();
    }

    @Override
    protected int getMinimumWidth() {
        return (getRotationQuarterTurns() % 2) == 0 ? BASE_WIDTH : BASE_HEIGHT;
    }

    @Override
    protected int getMinimumHeight() {
        return (getRotationQuarterTurns() % 2) == 0 ? BASE_HEIGHT : BASE_WIDTH;
    }

    public boolean isSliderHandleHit(int worldX, int worldY) {
        HandleGeometry geometry = getHandleGeometryWorld();
        if (geometry == null) {
            return false;
        }
        TriangleWorld triangle = getHandleTriangleWorld();
        if (triangle != null && triangle.contains(worldX, worldY)) {
            return true;
        }
        int dx = worldX - geometry.handleCenter.x;
        int dy = worldY - geometry.handleCenter.y;
        if ((dx * dx) + (dy * dy) <= (SLIDER_HIT_RADIUS * SLIDER_HIT_RADIUS)) {
            return true;
        }
        // Allow grabbing the wiper lead as well.
        return distancePointToSegmentSquared(worldX, worldY,
                geometry.topTerminal.x, geometry.topTerminal.y,
                geometry.trackPoint.x, geometry.trackPoint.y) <= (SLIDER_HIT_RADIUS * SLIDER_HIT_RADIUS);
    }

    public void setWiperFromWorld(int worldX, int worldY) {
        java.awt.Point a = getTerminalWorld(0);
        java.awt.Point b = getTerminalWorld(1);
        if (a == null || b == null) {
            return;
        }
        double abx = b.x - a.x;
        double aby = b.y - a.y;
        double denom = (abx * abx) + (aby * aby);
        if (denom < 1e-9) {
            return;
        }
        double apx = worldX - a.x;
        double apy = worldY - a.y;
        double t = ((apx * abx) + (apy * aby)) / denom;
        setWiperPosition((float) t);
        snapWiperToGrid();
    }

    private void snapWiperToGrid() {
        java.awt.Point a = getTerminalWorld(0);
        java.awt.Point b = getTerminalWorld(1);
        if (a == null || b == null) {
            return;
        }
        double trackLength = Math.hypot(b.x - a.x, b.y - a.y);
        int steps = Math.max(1, (int) Math.round(trackLength / Grid.SIZE));
        float snapped = (float) (Math.round(wiperPosition * steps) / (double) steps);
        this.wiperPosition = Math.max(0f, Math.min(1f, snapped));
    }

    private java.awt.Point getTerminalWorld(int index) {
        if (getConnectionPoints().size() <= index) {
            return null;
        }
        ConnectionPoint point = getConnectionPoints().get(index);
        return new java.awt.Point(getConnectionPointWorldX(point), getConnectionPointWorldY(point));
    }

    private HandleGeometry getHandleGeometryWorld() {
        java.awt.Point left = getTerminalWorld(0);
        java.awt.Point right = getTerminalWorld(1);
        java.awt.Point top = getWiperTerminalWorld();
        java.awt.Point handleCenter = getSliderHandleWorld();
        if (left == null || right == null || top == null || handleCenter == null) {
            return null;
        }
        double trackX = left.x + ((right.x - left.x) * wiperPosition);
        double trackY = left.y + ((right.y - left.y) * wiperPosition);
        java.awt.Point trackPoint = new java.awt.Point((int) Math.round(trackX), (int) Math.round(trackY));
        return new HandleGeometry(top, trackPoint, handleCenter);
    }

    private java.awt.Point getWiperTerminalWorld() {
        TrackFrame frame = getTrackFrameWorld();
        if (frame == null) {
            return null;
        }
        double t = Math.max(0.0, Math.min(1.0, wiperPosition));
        double trackX = frame.left.x + (frame.ux * (t * frame.trackLength));
        double trackY = frame.left.y + (frame.uy * (t * frame.trackLength));
        double wx = trackX - (frame.nx * frame.halfThickness);
        double wy = trackY - (frame.ny * frame.halfThickness);
        return new java.awt.Point(Grid.snap((int) Math.round(wx)), Grid.snap((int) Math.round(wy)));
    }

    private java.awt.Point getSliderHandleWorld() {
        TrackFrame frame = getTrackFrameWorld();
        if (frame == null) {
            return null;
        }
        double t = Math.max(0.0, Math.min(1.0, wiperPosition));
        double trackX = frame.left.x + (frame.ux * (t * frame.trackLength));
        double trackY = frame.left.y + (frame.uy * (t * frame.trackLength));
        double hx = trackX + (frame.nx * (frame.halfThickness + HANDLE_GAP));
        double hy = trackY + (frame.ny * (frame.halfThickness + HANDLE_GAP));
        return new java.awt.Point(Grid.snap((int) Math.round(hx)), Grid.snap((int) Math.round(hy)));
    }

    private TrackFrame getTrackFrameWorld() {
        java.awt.Point left = getTerminalWorld(0);
        java.awt.Point right = getTerminalWorld(1);
        if (left == null || right == null) {
            return null;
        }
        double dx = right.x - left.x;
        double dy = right.y - left.y;
        double length = Math.hypot(dx, dy);
        if (length < 1e-6) {
            return null;
        }
        double ux = dx / length;
        double uy = dy / length;
        double nx = -uy;
        double ny = ux;
        int turns = getRotationQuarterTurns();
        int unrotatedThickness = (turns % 2 != 0) ? width : height;
        double halfThickness = unrotatedThickness / 2.0;
        return new TrackFrame(left, right, ux, uy, nx, ny, length, halfThickness);
    }

    private record TrackFrame(java.awt.Point left, java.awt.Point right, double ux, double uy, double nx, double ny,
                              double trackLength, double halfThickness) {
    }

    @Override
    public int getConnectionPointWorldX(ConnectionPoint point) {
        int index = getConnectionPointIndex(point);
        if (index == 2) {
            java.awt.Point p = getWiperTerminalWorld();
            return p == null ? super.getConnectionPointWorldX(point) : p.x;
        }
        return super.getConnectionPointWorldX(point);
    }

    @Override
    public int getConnectionPointWorldY(ConnectionPoint point) {
        int index = getConnectionPointIndex(point);
        if (index == 2) {
            java.awt.Point p = getWiperTerminalWorld();
            return p == null ? super.getConnectionPointWorldY(point) : p.y;
        }
        return super.getConnectionPointWorldY(point);
    }

    private static int distancePointToSegmentSquared(int px, int py, int ax, int ay, int bx, int by) {
        long abx = (long) bx - ax;
        long aby = (long) by - ay;
        long apx = (long) px - ax;
        long apy = (long) py - ay;
        long denom = (abx * abx) + (aby * aby);
        if (denom <= 0) {
            long dx = (long) px - ax;
            long dy = (long) py - ay;
            return (int) Math.min(Integer.MAX_VALUE, (dx * dx) + (dy * dy));
        }
        double t = ((double) (apx * abx) + (double) (apy * aby)) / (double) denom;
        if (t < 0) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }
        double cx = ax + (abx * t);
        double cy = ay + (aby * t);
        double dx = px - cx;
        double dy = py - cy;
        double d2 = (dx * dx) + (dy * dy);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(d2));
    }

    @Override
    protected void drawComponent(Graphics2D g2) {
        Stroke componentStroke = g2.getStroke();
        // Override CircuitComponent scaling: keep this component's stroke width consistent while resizing.
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Colors.COMPONENT_STROKE);
        if (getConnectionPoints().size() < 3) {
            g2.setStroke(componentStroke);
            return;
        }
        ConnectionPoint left = getConnectionPoints().get(0);
        ConnectionPoint right = getConnectionPoints().get(1);
        int lx = left.getX();
        int ly = left.getY();
        int rx = right.getX();
        int ry = right.getY();
        int wiperX = x + Math.round(width * wiperPosition);
        int wiperTerminalY = y;
        int handleTipY = y + height;

        // Body between left/right: match the normal resistor zig-zag style.
        double dx = rx - lx;
        double dy = ry - ly;
        double length = Math.hypot(dx, dy);
        java.util.List<Point2D.Double> zigPolyline = new java.util.ArrayList<>();
        if (length >= 1) {
            double ux = dx / length;
            double uy = dy / length;
            double nx = -uy;
            double ny = ux;

            double lead = Math.max(Grid.SIZE / 2.0, length * 0.15);
            double zigStart = lead;
            double zigEnd = length - lead;
            int zigzagCount = Math.max(6, (int) Math.round((zigEnd - zigStart) / (Grid.SIZE * 0.6)));
            zigzagCount = Math.min(24, zigzagCount);
            double step = (zigEnd - zigStart) / zigzagCount;
            double maxAmplitude = (Math.min(width, height) / 2.0) - 4;
            double amplitude = Math.max(Grid.SIZE * 0.4, Math.min(width, height) * 0.4);
            amplitude = Math.max(0, Math.min(amplitude, maxAmplitude));

            double lastX = lx + (ux * zigStart);
            double lastY = ly + (uy * zigStart);
            zigPolyline.add(new Point2D.Double(lx, ly));
            zigPolyline.add(new Point2D.Double(lastX, lastY));
            g2.drawLine(lx, ly, (int) Math.round(lastX), (int) Math.round(lastY));
            int direction = -1;
            for (int i = 1; i <= zigzagCount; i++) {
                double t = zigStart + (step * i);
                double offset = amplitude * direction;
                double zx = lx + (ux * t) + (nx * offset);
                double zy = ly + (uy * t) + (ny * offset);
                zigPolyline.add(new Point2D.Double(zx, zy));
                g2.drawLine((int) Math.round(lastX), (int) Math.round(lastY),
                        (int) Math.round(zx), (int) Math.round(zy));
                lastX = zx;
                lastY = zy;
                direction *= -1;
            }
            zigPolyline.add(new Point2D.Double(rx, ry));
            g2.drawLine((int) Math.round(lastX), (int) Math.round(lastY), rx, ry);

            // Preserve geometry for wiper contact calculations below.
            thisGeometryUx = ux;
            thisGeometryUy = uy;
            thisGeometryNx = nx;
            thisGeometryNy = ny;
            thisGeometryLead = lead;
            thisGeometryZigStart = zigStart;
            thisGeometryZigEnd = zigEnd;
            thisGeometryStep = step;
            thisGeometryZigzagCount = zigzagCount;
            thisGeometryAmplitude = amplitude;
        } else {
            // Degenerate: still draw a baseline.
            g2.drawLine(lx, ly, rx, ry);
            zigPolyline.add(new Point2D.Double(lx, ly));
            zigPolyline.add(new Point2D.Double(rx, ry));
            thisGeometryUx = 1.0;
            thisGeometryUy = 0.0;
            thisGeometryNx = 0.0;
            thisGeometryNy = 1.0;
            thisGeometryLead = 0.0;
            thisGeometryZigStart = 0.0;
            thisGeometryZigEnd = 0.0;
            thisGeometryStep = 0.0;
            thisGeometryZigzagCount = 0;
            thisGeometryAmplitude = 0.0;
        }

        // Compute where the wiper lead should touch the zig-zag outline by intersecting a ray
        // from the wiper terminal toward the centerline at the current slider position.
        double contactX;
        double contactY;
        if (length >= 1) {
            double tLen = Math.max(0.0, Math.min(1.0, wiperPosition)) * length;
            double baseX = lx + (thisGeometryUx * tLen);
            double baseY = ly + (thisGeometryUy * tLen);
            Point2D.Double hit = intersectRayWithPolyline(wiperX, wiperTerminalY,
                    baseX, baseY, zigPolyline);
            if (hit != null) {
                contactX = hit.x;
                contactY = hit.y;
            } else {
                contactX = baseX;
                contactY = baseY;
            }
        } else {
            contactX = lx + ((rx - lx) * wiperPosition);
            contactY = ly + ((ry - ly) * wiperPosition);
        }

        // Wiper lead from top terminal down to slider
        drawLeadWithPowerEffect(g2, wiperX, wiperTerminalY,
                (int) Math.round(contactX), (int) Math.round(contactY));

        // Slider triangle handle outside the body:
        // flat side down, point up, with the point sitting on the component edge.
        int triSize = Math.max(8, Math.min(width, height) / 3);
        int half = triSize / 2;
        int triHeight = Math.max(6, triSize);
        Polygon poly = new Polygon();
        poly.addPoint(wiperX, handleTipY);
        poly.addPoint(wiperX - half, handleTipY + triHeight);
        poly.addPoint(wiperX + half, handleTipY + triHeight);
        g2.fillPolygon(poly);
        g2.setStroke(componentStroke);
    }

    // Scratch space for the last-resolved body geometry (avoids recomputing everything twice).
    private double thisGeometryUx;
    private double thisGeometryUy;
    private double thisGeometryNx;
    private double thisGeometryNy;
    private double thisGeometryLead;
    private double thisGeometryZigStart;
    private double thisGeometryZigEnd;
    private double thisGeometryStep;
    private int thisGeometryZigzagCount;
    private double thisGeometryAmplitude;

    private static double zigOffsetAtIndex(int index, double amplitude) {
        if (index <= 0) {
            return 0.0;
        }
        return amplitude * (index % 2 == 1 ? -1.0 : 1.0);
    }

    private static Point2D.Double intersectRayWithPolyline(double rayX1, double rayY1, double towardX, double towardY,
            java.util.List<Point2D.Double> polyline) {
        if (polyline == null || polyline.size() < 2) {
            return null;
        }
        double dirX = towardX - rayX1;
        double dirY = towardY - rayY1;
        double len = Math.hypot(dirX, dirY);
        if (len < 1e-9) {
            return null;
        }
        dirX /= len;
        dirY /= len;
        double rayX2 = rayX1 + (dirX * 10000.0);
        double rayY2 = rayY1 + (dirY * 10000.0);

        Point2D.Double best = null;
        double bestT = Double.POSITIVE_INFINITY;
        for (int i = 0; i < polyline.size() - 1; i++) {
            Point2D.Double a = polyline.get(i);
            Point2D.Double b = polyline.get(i + 1);
            Intersection hit = segmentIntersection(rayX1, rayY1, rayX2, rayY2, a.x, a.y, b.x, b.y);
            if (hit == null) {
                continue;
            }
            if (hit.t >= 0.0 && hit.t < bestT) {
                bestT = hit.t;
                best = new Point2D.Double(hit.x, hit.y);
            }
        }
        return best;
    }

    private static Intersection segmentIntersection(double ax, double ay, double bx, double by,
            double cx, double cy, double dx, double dy) {
        double rX = bx - ax;
        double rY = by - ay;
        double sX = dx - cx;
        double sY = dy - cy;
        double denom = (rX * sY) - (rY * sX);
        if (Math.abs(denom) < 1e-9) {
            return null;
        }
        double qpx = cx - ax;
        double qpy = cy - ay;
        double t = ((qpx * sY) - (qpy * sX)) / denom;
        double u = ((qpx * rY) - (qpy * rX)) / denom;
        if (t < 0.0 || t > 1.0 || u < 0.0 || u > 1.0) {
            return null;
        }
        return new Intersection(ax + (t * rX), ay + (t * rY), t);
    }

    private record Intersection(double x, double y, double t) {
    }

    private void drawLeadWithPowerEffect(Graphics2D g2, int x1, int y1, int x2, int y2) {
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();
        g2.setColor(Colors.COMPONENT_STROKE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x1, y1, x2, y2);
        if (computedAmpere > 0.0001f) {
            g2.setColor(new Color(220, 60, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setColor(originalColor);
        g2.setStroke(originalStroke);
    }

    private record HandleGeometry(java.awt.Point topTerminal, java.awt.Point trackPoint, java.awt.Point handleCenter) {
    }

    private TriangleWorld getHandleTriangleWorld() {
        TrackFrame frame = getTrackFrameWorld();
        java.awt.Point tip = getSliderHandleWorld();
        if (frame == null || tip == null) {
            return null;
        }
        double ux = frame.nx;
        double uy = frame.ny;
        double px = frame.ux;
        double py = frame.uy;
        int triSize = Math.max(8, Math.min(width, height) / 3);
        int half = triSize / 2;
        int triHeight = Math.max(6, triSize);
        int baseCx = (int) Math.round(tip.x + (ux * triHeight));
        int baseCy = (int) Math.round(tip.y + (uy * triHeight));
        java.awt.Point baseLeft = new java.awt.Point((int) Math.round(baseCx - (px * half)),
                (int) Math.round(baseCy - (py * half)));
        java.awt.Point baseRight = new java.awt.Point((int) Math.round(baseCx + (px * half)),
                (int) Math.round(baseCy + (py * half)));
        return new TriangleWorld(tip, baseLeft, baseRight);
    }

    private record TriangleWorld(java.awt.Point tip, java.awt.Point baseLeft, java.awt.Point baseRight) {
        boolean contains(int x, int y) {
            // Barycentric technique.
            double x1 = tip.x;
            double y1 = tip.y;
            double x2 = baseLeft.x;
            double y2 = baseLeft.y;
            double x3 = baseRight.x;
            double y3 = baseRight.y;
            double denom = ((y2 - y3) * (x1 - x3)) + ((x3 - x2) * (y1 - y3));
            if (Math.abs(denom) < 1e-9) {
                return false;
            }
            double a = (((y2 - y3) * (x - x3)) + ((x3 - x2) * (y - y3))) / denom;
            double b = (((y3 - y1) * (x - x3)) + ((x1 - x3) * (y - y3))) / denom;
            double c = 1.0 - a - b;
            return a >= 0 && b >= 0 && c >= 0;
        }
    }
}
