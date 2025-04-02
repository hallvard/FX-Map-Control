/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Base class for azimuthal map projections.
 */
public abstract class AzimuthalProjection extends MapProjection {

    @Override
    public Bounds boundingBoxToBounds(MapBoundingBox boundingBox) {
        if (boundingBox instanceof CenteredBoundingBox cbbox) {
            Point2D center = locationToMap(cbbox.getCenter());
            double width = cbbox.getWidth();
            double height = cbbox.getHeight();
            return new BoundingBox(center.getX() - width / 2d, center.getY() - height / 2d, width, height);
        } else {
            return super.boundingBoxToBounds(boundingBox);
        }
    }

    @Override
    public MapBoundingBox boundsToBoundingBox(Bounds bounds) {
        Location center = mapToLocation(new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2d,
                bounds.getMinY() + bounds.getHeight() / 2d));

        return new CenteredBoundingBox(center, bounds.getWidth(), bounds.getHeight()); // width and height in meters
    }

    /**
     * Calculates azimuth and distance in radians from location1 to location2.
     */
    public static double[] getAzimuthDistance(Location location1, Location location2) {
        double lat1 = location1.latitude() * Math.PI / 180d;
        double lon1 = location1.longitude() * Math.PI / 180d;
        double lat2 = location2.latitude() * Math.PI / 180d;
        double lon2 = location2.longitude() * Math.PI / 180d;
        double cosLat1 = Math.cos(lat1);
        double sinLat1 = Math.sin(lat1);
        double cosLat2 = Math.cos(lat2);
        double sinLat2 = Math.sin(lat2);
        double cosLon12 = Math.cos(lon2 - lon1);
        double sinLon12 = Math.sin(lon2 - lon1);
        double cosDistance = sinLat1 * sinLat2 + cosLat1 * cosLat2 * cosLon12;
        double azimuth = Math.atan2(sinLon12, cosLat1 * sinLat2 / cosLat2 - sinLat1 * cosLon12);
        double distance = Math.acos(Math.max(Math.min(cosDistance, 1d), -1d));

        return new double[]{azimuth, distance};
    }

    /**
     * Calculates the Location of the point given by azimuth and distance in radians from location.
     */
    public static Location getLocation(Location location, double azimuth, double distance) {
        double lat = location.latitude() * Math.PI / 180d;
        double sinDistance = Math.sin(distance);
        double cosDistance = Math.cos(distance);
        double cosAzimuth = Math.cos(azimuth);
        double sinAzimuth = Math.sin(azimuth);
        double cosLat1 = Math.cos(lat);
        double sinLat1 = Math.sin(lat);
        double sinLat2 = sinLat1 * cosDistance + cosLat1 * sinDistance * cosAzimuth;
        double lat2 = Math.asin(Math.max(Math.min(sinLat2, 1d), -1d));
        double dLon = Math.atan2(sinDistance * sinAzimuth, cosLat1 * cosDistance - sinLat1 * sinDistance * cosAzimuth);

        return new Location(180d / Math.PI * lat2, location.longitude() + 180d / Math.PI * dLon);
    }
}
