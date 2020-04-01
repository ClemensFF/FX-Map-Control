/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

/**
 * Transforms geographic coordinates to cartesian coordinates according to the Azimuthal Equidistant Projection.
 */
public class AzimuthalEquidistantProjection extends AzimuthalProjection {

    public AzimuthalEquidistantProjection() {
        // No known standard or de-facto standard CRS ID
    }

    public AzimuthalEquidistantProjection(String crsId) {
        setCrsId(crsId);
    }

    @Override
    public Point2D locationToMap(Location location) {
        if (location.equals(getCenter())) {
            return new Point2D(0d, 0d);
        }

        double[] azimuthDistance = getAzimuthDistance(getCenter(), location);
        double azimuth = azimuthDistance[0];
        double distance = Wgs84EquatorialRadius * azimuthDistance[1];

        return new Point2D(distance * Math.sin(azimuth), distance * Math.cos(azimuth));
    }

    @Override
    public Location mapToLocation(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        if (x == 0d && y == 0d) {
            return getCenter();
        }

        double azimuth = Math.atan2(x, y);
        double distance = Math.sqrt(x * x + y * y) / Wgs84EquatorialRadius;

        return getLocation(getCenter(), azimuth, distance);
    }
}
