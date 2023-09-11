package com.example.taxi.socket

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taxi.R
import com.example.taxi.domain.model.location.LocationRequest
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.HomeActivity
import com.mapbox.android.core.location.*
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement.distance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SocketService : android.app.Service() {

    // Initialize your dependencies here, e.g., ViewModel, CoroutineScope, SocketMessageProcessor


    private val socketMessageProcessor: SocketMessageProcessor by inject()
    private val preferenceManager: UserPreferenceManager by inject()
    private var socketRepository: SocketRepository? = null
    private lateinit var locationEngine: LocationEngine
    private var lastSentLocation: Point? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra("data")
            Log.d("jarayon", "onReceive: $data")
            sendDataToSocket(data)
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val wakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            packageName
        ).also {
            it.setReferenceCounted(false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter()
        filter.addAction("com.example.SEND_TO_SOCKET")
        registerReceiver(receiver, filter)
        // Create a notification for the foreground service
        val notification = Notification() // Replace with your own notification
        startForeground(NOTIFICATION_ID, createNotification())

        checkAndUpdateCPUWake()

        socketRepository = SocketRepository(
            context = this,
//            socketViewModel = socketViewModel,
            viewModelScope = CoroutineScope(Dispatchers.IO),
            socketMessageProcessor = socketMessageProcessor
        )
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(10000)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(20000)
            .setFastestInterval(10000)// Increased maxWaitTime
            .build()

        locationEngine.requestLocationUpdates(request, object :
            LocationEngineCallback<LocationEngineResult> {

            override fun onSuccess(result: LocationEngineResult?) {
                val location = result?.lastLocation ?: return

                val currentLocationPoint = Point.fromLngLat(location.longitude, location.latitude)

                // Check if the new location is sufficiently different from the last sent location
                if (lastSentLocation == null ||
                    distance(
                        lastSentLocation!!,
                        currentLocationPoint,
                        TurfConstants.UNIT_KILOMETERS
                    ) > 0.01
                ) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val accuracy = location.accuracy
                    val angle = location.bearing


                    // Replace this with your own ViewModel or use socketMessageProcessor to send the location
                    socketMessageProcessor.sendLocation(
                        LocationRequest(
                            latitude = latitude,
                            longitude = longitude,
                            accuracy = accuracy.toInt(),
                            angle = angle.toInt()
                        )
                    )

                    // Store the sent location
                    lastSentLocation = currentLocationPoint
                }
            }

            override fun onFailure(exception: Exception) {
                // Handle location update failure
                Log.e("LocationUpdateFailure", "Failed to get location updates", exception)
            }
        }, Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra("TOKEN")
        Log.d("jarayon", "onStartCommand: umumiy")


        val isReadyForWork = intent?.getBooleanExtra("IS_READY_FOR_WORK", false) ?: false

        if (token != null && isReadyForWork) {
            socketRepository?.initSocket(token)
        } else {
            socketRepository?.disconnectSocket()
            stopSelf()
        }

        return START_STICKY
    }
    private fun sendDataToSocket(data: String?) {
        // Code to send data to socket
        checkAndUpdateCPUWake()
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("jarayon", "onDestroy: sihlayapti ")
        socketRepository?.disconnectSocket()
        socketRepository = null
        unregisterReceiver(receiver)
        releaseWakelock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            flags
        )

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

        return notificationBuilder
            .setContentTitle("Socket Service")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pendingIntent)
            .build()
    }


    companion object {
        private const val CHANNEL_ID = "SocketServiceChannel"
        private const val CHANNEL_NAME = "Socket Service Channel"
        const val NOTIFICATION_ID = 101
    }


    private fun checkAndUpdateCPUWake() {
        Log.d("jarayon", "checkAndUpdateCPUWake: ${preferenceManager.getDriverStatus()}")
        if (preferenceManager.getDriverStatus() == UserPreferenceManager.DriverStatus.COMPLETED) {
            releaseWakelock()
        } else {
            acquireWakeLock()
        }
    }

    private fun releaseWakelock() {
        Log.d("jarayon", "releaseWakelock: ")
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun acquireWakeLock() {
        Log.d("jarayon", "acquireWakeLock: ")
        if (wakeLock.isHeld.not()) {
            Log.d("jarayon", "acquireWakeLock: isheld")
            wakeLock.acquire(TimeUnit.HOURS.toMillis(2))
        }
    }
}
