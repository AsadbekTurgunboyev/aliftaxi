package com.example.taxi.components.service

import android.annotation.SuppressLint
import android.location.Criteria
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.koin.core.context.GlobalContext

@RequiresApi(Build.VERSION_CODES.N)
class LocationProvider: LocationListener, GnssStatus.Callback() {

    private var locationChangeCallback: (Location) -> Unit = {}
    private var gpsSignalCallback: (signalStrength: Int) -> Unit = {}
    private var startTime = System.currentTimeMillis()
    private var currentGPSStrength = 0

    private val locationManager by lazy {
        GlobalContext.get().get<LocationManager>()
    }


    override fun onLocationChanged(location: Location) {
        if (isValidLocation(location)) {
            locationChangeCallback(location)
        }else{
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun subscribe(
        locationChangeCallback: (Location) -> Unit,
        gpsSignalCallback: (gpsSignalStrength: Int) -> Unit
    ) {
        this.locationChangeCallback = locationChangeCallback
        this.gpsSignalCallback = gpsSignalCallback
//        getLastKnownLocation()
        startTime = System.currentTimeMillis()
        val criteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
            isCostAllowed = true // This allows Android to use data services
        }
//        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null)
//        locationManager.requestLocationUpdates(1000L, 1f, criteria, this, null)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0F, this)
        val isSuccess = locationManager.registerGnssStatusCallback(
            this,
            Handler(Looper.getMainLooper())
        )
        Log.d("GPS", "GnssStatus Callback registration successful: $isSuccess")
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        val locationManager = GlobalContext.get().get<LocationManager>()
        val lastKnownLocation: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            // Use the last known location
            locationChangeCallback(lastKnownLocation)
        }
    }

    fun unsubscribe() {
        this.locationChangeCallback = {}
        locationManager.removeUpdates(this)
    }

    override fun onProviderEnabled(provider: String) {
        //Nothing to do
    }

    override fun onProviderDisabled(provider: String) {
        //Nothing to do
    }

    override fun onSatelliteStatusChanged(status: GnssStatus) {
        super.onSatelliteStatusChanged(status)
        Log.d("GPS", "Total Satellites: ${status.satelliteCount}")
        status.let { gnsStatus ->
            val totalSatellites = gnsStatus.satelliteCount
            if (totalSatellites > 0) {
                var satellitesFixed = 0
                for (i in 0 until totalSatellites) {
                    if (gnsStatus.usedInFix(i)) {
                        satellitesFixed++
                    }
                }
                Log.d("GPS", "Satellites Fixed: $satellitesFixed")
                currentGPSStrength = (satellitesFixed * 100) / totalSatellites
                gpsSignalCallback(currentGPSStrength)
            }
        }
    }

    private fun isValidLocation(location: Location): Boolean {

        if (location.time < startTime) {
             return false
        }

        if (currentGPSStrength == 0) {
//            getLastKnownLocation()
            return false
        }

        if (location.accuracy <= 0 || location.accuracy > 20) {

            return false
        }
        return true
    }
}