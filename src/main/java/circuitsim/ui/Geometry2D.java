package circuitsim.ui;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Small 2D geometry helpers shared across UI and component rendering.
 */
public final class Geometry2D {
    private Geometry2D() {
    }

    public static double distancePointToSegment(int px, int py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / ((dx * dx) + (dy * dy));
        t = Math.max(0, Math.min(1, t));
        double projX = x1 + (t * dx);
        double projY = y1 + (t * dy);
        return Math.hypot(px - projX, py - projY);
    }

    public static int distancePointToSegmentSquared(int px, int py, int ax, int ay, int bx, int by) {
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

    public static Point2D.Double intersectRayWithPolyline(double rayX1, double rayY1,
                                                          double towardX, double towardY,
                                                          List<Point2D.Double> polyline) {
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

    public static boolean pointInTriangle(double px, double py,
                                          double ax, double ay,
                                          double bx, double by,
                                          double cx, double cy) {
        double denom = ((by - cy) * (ax - cx)) + ((cx - bx) * (ay - cy));
        if (Math.abs(denom) < 1e-9) {
            return false;
        }
        double a = (((by - cy) * (px - cx)) + ((cx - bx) * (py - cy))) / denom;
        double b = (((cy - ay) * (px - cx)) + ((ax - cx) * (py - cy))) / denom;
        double c = 1.0 - a - b;
        return a >= 0 && b >= 0 && c >= 0;
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
}

