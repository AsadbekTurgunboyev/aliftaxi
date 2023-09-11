package com.example.taxi.components.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.taxi.R
import com.example.taxi.domain.model.socket.SocketMessage
import com.example.taxi.domain.model.socket.SocketOnlyForYouData
import com.example.taxi.domain.model.socket.toOrderData
import com.example.taxi.ui.home.HomeActivity
import com.example.taxi.ui.home.order.ServiceOrderAdapter
import com.example.taxi.utils.*
import com.example.taxi.utils.ConversionUtil.convertToKm
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*

class KillStateDialogService : Service() {
    private var windowManager: WindowManager? = null
    private var dialogView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private val notificationId = 1
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var soundPlayer: SoundPlayer
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val notificationChannelId = "KillStateDialogChannel"

    override fun onBind(intent: Intent?) = null


    override fun onCreate() {
        super.onCreate()
         val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "myApp::MyWakelockTag"
        )
        ViewUtils.setLanguageForService(baseContext)


        checkOverlayPermission()
        setupDialogView()
        createNotificationChannel()
        soundPlayer = SoundPlayer(SoundPlayer.SoundType.LowSound, this)

    }

    override fun onDestroy() {
        super.onDestroy()
        dialogView?.let { windowManager?.removeView(it) }
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupDialogView() {
        setTheme(R.style.AppTheme_Dialog)

        dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_only_for_you, null) as FrameLayout
        dialogView?.apply {
            background = ColorDrawable(Color.TRANSPARENT)
            layoutParams = createLayoutParams()
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(dialogView, dialogView?.layoutParams)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            dimAmount = 0.0f
            windowAnimations = android.R.style.Animation_Dialog
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the message from the intent extras
        val message = intent?.getStringExtra("message")

        if (message != null) {
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            val type = Types.newParameterizedType(
                SocketMessage::class.java,
                SocketOnlyForYouData::class.java
            )
            val adapter: JsonAdapter<SocketMessage<SocketOnlyForYouData>> = moshi.adapter(type)
            try {
                val socketMessage = adapter.fromJson(message)
                if (socketMessage != null) {
                    val data = socketMessage.data as? SocketOnlyForYouData
                    data?.let { initVarDialog(it) }
                    dialogView?.visibility = View.VISIBLE
                }
                // Show the dialog view
            } catch (e: Exception) {
                Log.e("DialogService", "Error parsing JSON", e)
            }
            handler.postDelayed({
                dialogView?.visibility = View.GONE
                stopSelf()
                wakeLock?.release()
                soundPlayer.stopSound()

            }, 15000)

            startForeground(notificationId, createNotification())
        } else {
            dialogView?.visibility = View.GONE
            soundPlayer.stopSound()
            stopSelf()
        }

        // Ensure the service is not terminated by the system
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notification_channel_name)
            val channelDescription = "desc"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(notificationChannelId, channelName, importance).apply {
                    description = channelDescription
                }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initVarDialog(data: SocketOnlyForYouData) {
        setupViews(data)
        setupClickListeners(data)
        animateProgressBar()
        soundPlayer.playRequestSound()
    }


    @SuppressLint("MissingPermission")
    private fun setupViews(data: SocketOnlyForYouData) {
        val priceTextView = dialogView?.findViewById<TextView>(R.id.priceTextView_dialog)
        val addressTextView = dialogView?.findViewById<TextView>(R.id.addressTextView_dialog)
        val secondAddressTextView =
            dialogView?.findViewById<TextView>(R.id.secondDestinationAddress)
        val distanceTextView = dialogView?.findViewById<TextView>(R.id.distanceTextView_dialog)

        val service_recy = dialogView?.findViewById<RecyclerView>(R.id.service_recyclerView_dialog)
        val comment = dialogView?.findViewById<TextView>(R.id.comment_textView)

        service_recy?.adapter = ServiceOrderAdapter(data.toOrderData().services)
        data.type
        priceTextView?.text = PhoneNumberUtil.formatMoneyNumberPlate(data.startCost.toString())
        addressTextView?.convertToCyrillic(data.address.from)
        secondAddressTextView?.convertToCyrillic(data.address.to)
        comment?.text = data.comment?.ifEmpty {
            "-"
        }

        distanceTextView?.text = data.distance?.toDouble()
            ?.let { convertToKm(it) }
            ?.let { "%.2f".format(it) }
            ?.plus(" km")
}


private fun setupClickListeners(data: SocketOnlyForYouData) {
    val cancelOrderButton = dialogView?.findViewById<TextView>(R.id.cancelOrderButton)
    val acceptButton = dialogView?.findViewById<FrameLayout>(R.id.accept_button)

    cancelOrderButton?.setOnClickListener {
        dialogView?.visibility = View.GONE
        soundPlayer.stopSound()
        stopSelf()
    }

    acceptButton?.setOnClickListener {
        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("navigate_to_order", true)
        intent.putExtra("order_id", data.id)
        intent.putExtra("lat1", data.latitude1)
        intent.putExtra("lat2", data.latitude2)
        intent.putExtra("long1", data.longitude1)
        intent.putExtra("long2", data.longitude2)
        Log.d(
            "lokatsiya",
            "setupClickListeners: long2 = ${data.longitude2} , lat2 = ${data.latitude2}"
        )
        dialogView?.visibility = View.GONE
        soundPlayer.stopSound()
        startActivity(intent)
        stopSelf()
    }
}


private fun animateProgressBar() {
    val progressBar = dialogView?.findViewById<ProgressBar>(R.id.progress_bar)
    val objectAnimator = ObjectAnimator.ofInt(
        progressBar, "progress",
        progressBar!!.progress, 0
    ).setDuration(15000)

    objectAnimator.addUpdateListener { valueAnimator ->
        val progress = valueAnimator.animatedValue as Int
        progressBar.progress = progress
    }
    objectAnimator.start()
}

private fun createNotification(): Notification {
    val contentTitle = ""
    val contentText = ""
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    val contentIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, HomeActivity::class.java),
        flags
    )
    val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        .setSmallIcon(R.drawable.ic_admin)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setContentIntent(contentIntent)
        .setAutoCancel(true)

    return notificationBuilder.build()
}
}