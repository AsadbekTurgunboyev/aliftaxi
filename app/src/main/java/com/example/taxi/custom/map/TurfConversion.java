package com.example.taxi.custom.map;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class TurfConversion {
    private static final Map<String, Double> FACTORS;

    static {
        FACTORS = new HashMap<>();
        FACTORS.put(TurfConstants.UNIT_MILES, 3960d);
        FACTORS.put(TurfConstants.UNIT_NAUTICAL_MILES, 3441.145d);
        FACTORS.put(TurfConstants.UNIT_DEGREES, 57.2957795d);
        FACTORS.put(TurfConstants.UNIT_RADIANS, 1d);
        FACTORS.put(TurfConstants.UNIT_INCHES, 250905600d);
        FACTORS.put(TurfConstants.UNIT_YARDS, 6969600d);
        FACTORS.put(TurfConstants.UNIT_METERS, 6373000d);
        FACTORS.put(TurfConstants.UNIT_CENTIMETERS, 6.373e+8d);
        FACTORS.put(TurfConstants.UNIT_KILOMETERS, 6373d);
        FACTORS.put(TurfConstants.UNIT_FEET, 20908792.65d);
        FACTORS.put(TurfConstants.UNIT_CENTIMETRES, 6.373e+8d);
        FACTORS.put(TurfConstants.UNIT_METRES, 6373000d);
        FACTORS.put(TurfConstants.UNIT_KILOMETRES, 6373d);
    }

    private TurfConversion() {
        // Private constructor preventing initialization of this class
    }

    public static double radiansToLength(double radians, @NonNull @TurfConstants.TurfUnitCriteria String units) {
        return radians * FACTORS.get(units);
    }

    public static double degreesToRadians(double degrees) {
        double radians = degrees % 360;
        return radians * Math.PI / 180;
    }
}
