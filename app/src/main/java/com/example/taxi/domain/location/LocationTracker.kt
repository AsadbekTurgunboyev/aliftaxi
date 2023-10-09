package com.example.taxi.domain.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log

class LocationTracker private constructor(private val context: Context) {

    private var lastLocation: Location? = null
    private var pausedLocation: Location? = null
    private var locationUpdateListener: LocationUpdateListener? = null
    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    private var isFirst = true
    interface LocationUpdateListener {
        fun onLocationChanged(location: Location?)
        fun onDistanceChanged(distance: Float)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            location.let {
                if (isFirst){
                    pausedLocation = it
                    isFirst = false
                }
                Log.d("masofa", "onLocationChanged: pauselocation $pausedLocation")
                if (pausedLocation != null) {
                    val distance = pausedLocation!!.distanceTo(it)

                    Log.d("masofa", "onLocationChanged: $distance")
                    if (distance > 80) {
                        isFirst = true
                        locationUpdateListener?.onDistanceChanged(distance)
                        stopLocationUpdates()
                    }
                }
                lastLocation = it
                locationUpdateListener?.onLocationChanged(it)
            }
        }

        // ... other methods remain unchanged
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(listener: LocationUpdateListener) {
        this.locationUpdateListener = listener
        Log.d("masofa", "startLocationUpdates: start")
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            locationListener
        )
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        this.locationUpdateListener = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: LocationTracker? = null

        fun getInstance(context: Context): LocationTracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationTracker(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun pauseTracking() {
        pausedLocation = lastLocation
//        stopLocationUpdates()
    }

    fun resumeTracking(listener: LocationUpdateListener) {
        this.locationUpdateListener = listener
        startLocationUpdates(listener)
    }
}
