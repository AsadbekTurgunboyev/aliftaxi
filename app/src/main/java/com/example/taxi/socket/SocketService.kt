package com.example.taxi.socket

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taxi.R
import com.example.taxi.domain.model.location.LocationRequest
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.HomeActivity
import com.example.taxi.ui.home.order.OrderViewModel
import com.mapbox.android.core.location.*
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement.distance
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class SocketService : Service() {

    // Initialize your dependencies here, e.g., ViewModel, CoroutineScope, SocketMessageProcessor

    private var isAckReceived = true
    private val orderViewModel: OrderViewModel by inject()
    private val compositeDisposable = CompositeDisposable()
    private var hasLocationChanged = false
    private var lastSentAccuracy = 0
    private var lastSentAngle = 0

    private val handler = Handler(Looper.getMainLooper())
    private val locationSenderRunnable: Runnable = object : Runnable {
        override fun run() {
            if (hasLocationChanged && lastSentLocation != null) {
                hasLocationChanged = false

                Log.d("LocationUpdateFailure", "run: ")
                socketMessageProcessor.sendLocation(
                    LocationRequest(
                        latitude = lastSentLocation!!.latitude(),
                        longitude = lastSentLocation!!.longitude(),
                        accuracy = lastSentAccuracy, // Replace with actual accuracy
                        angle = lastSentAngle // Replace with actual angle
                    )
                )
                compositeDisposable.add(
                    orderViewModel.waitForAck()
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            if (it) {
                                // Acknowledgment received, proceed to next location
                                handler.postDelayed(this, 2000)
                            } else {
                                // No acknowledgment received, retry or skip
                                handler.postDelayed(this, 1000)
                            }
                        }, {
                            // Timeout or other error occurred
                            handler.postDelayed(this, 1000)
                        })
                )
            }else{
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val socketMessageProcessor: SocketMessageProcessor by inject()
    private val preferenceManager: UserPreferenceManager by inject()
    private var socketRepository: SocketRepository? = null
    private lateinit var locationEngine: LocationEngine
    private var lastSentLocation: Point? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra("data")

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
        handler.postDelayed(locationSenderRunnable, 1000)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(5000)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(5000)
            .setFastestInterval(5000) // Increased maxWaitTime
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
                    ) > 0.1
                ) {
                    Log.d("locationupda", "onSuccess: ozgarish")
                    val latitude = location.latitude
                    val longitude = location.longitude
                    lastSentAccuracy = location.accuracy.toInt()
                    lastSentAngle = location.bearing.toInt()


                    // Replace this with your own ViewModel or use socketMessageProcessor to send the location
//                    socketMessageProcessor.sendLocation(
//                        LocationRequest(
//                            latitude = latitude,
//                            longitude = longitude,
//                            accuracy = accuracy.toInt(),
//                            angle = angle.toInt()
//                        )
//                    )

                    // Store the sent location
                    hasLocationChanged = true
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
        socketRepository?.disconnectSocket()
        socketRepository = null
        unregisterReceiver(receiver)
        releaseWakelock()
        handler.removeCallbacks(locationSenderRunnable)
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
//        if (preferenceManager.getDriverStatus() == UserPreferenceManager.DriverStatus.COMPLETED) {
//            releaseWakelock()
//        } else {
//            acquireWakeLock()
//        }
        acquireWakeLock()
    }

    private fun releaseWakelock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock.isHeld.not()) {
            wakeLock.acquire(TimeUnit.HOURS.toMillis(2))
        }
    }
}
