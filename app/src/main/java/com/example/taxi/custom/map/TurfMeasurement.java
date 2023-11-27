package com.example.taxi.custom.map;

import static com.example.taxi.custom.map.TurfConversion.degreesToRadians;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Point;

public class TurfMeasurement {

    public static double distance(@NonNull Point point1, @NonNull Point point2,
                                  @NonNull @TurfConstants.TurfUnitCriteria String units) {
        double difLat = degreesToRadians((point2.latitude() - point1.latitude()));
        double difLon = degreesToRadians((point2.longitude() - point1.longitude()));
        double lat1 = degreesToRadians(point1.latitude());
        double lat2 = degreesToRadians(point2.latitude());

        double value = Math.pow(Math.sin(difLat / 2), 2)
                + Math.pow(Math.sin(difLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);

        return TurfConversion.radiansToLength(
                2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value)), units);
    }
}

