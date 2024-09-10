package com.example.taxi.utils

import com.example.taxi.domain.model.map.MapLocation

object ConstantsUtils {

    const val REQUEST_CODE_STT_START = 11
    const val REQUEST_CODE_STT_END = 12
    const val LOCATION_PERMISSIONS_REQUEST_CODE = 34

    var locationStart: MapLocation = MapLocation(0.0, 0.0)
    var locationDestination: MapLocation = MapLocation(0.0, 0.0)
    var locationDestination2: MapLocation? = MapLocation(0.0,0.0)

    const val STATUS_GOING_TO_CLIENT = 1;
    const val STATUS_WAITING_FOR_CLIENT = 2;
    const val STATUS_ON_THE_WAY = 3;
    const val STATUS_CANCELLED = 4;
    const val STATUS_FINISHED = 5;
}