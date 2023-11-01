package com.example.taxi.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.taxi.R
import com.example.taxi.components.service.ActivityMessenger
import com.example.taxi.components.service.DriveBackGroundService
import com.example.taxi.components.service.KillStateDialogService
import com.example.taxi.databinding.ActivityHomeBinding
import com.example.taxi.domain.model.DashboardData
import com.example.taxi.domain.model.socket.SocketMessage
import com.example.taxi.domain.model.socket.SocketOnlyForYouData
import com.example.taxi.domain.model.socket.toOrderData
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.network.NO_CONNECT
import com.example.taxi.socket.SocketConfig
import com.example.taxi.ui.home.DriveAction.PAUSE
import com.example.taxi.ui.home.DriveAction.START
import com.example.taxi.ui.home.DriveAction.STOP
import com.example.taxi.ui.home.driver.DriverViewModel
import com.example.taxi.ui.home.driver.driveReport.DriveFinishDialog
import com.example.taxi.ui.home.driver.driveReport.DriveReportViewModel
import com.example.taxi.ui.home.order.OrderViewModel
import com.example.taxi.utils.DialogUtils
import com.example.taxi.utils.ViewUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val MY_REQUEST_CODE = 999

class HomeActivity : AppCompatActivity(), ServiceConnection {
    private val OVERLAY_PERMISSION_REQUEST_CODE = 5
    private val userPreferenceManager: UserPreferenceManager by inject()

    private val orderViewModel: OrderViewModel by viewModel()
    private val viewModel: HomeViewModel by viewModel()
    private lateinit var navController: NavController

    private val driveReportViewModel: DriveReportViewModel by viewModel()
    private val driverViewModel: DriverViewModel by viewModel()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    //    private lateinit var locationEngine: LocationEngine
    lateinit var viewBinding: ActivityHomeBinding


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("OrderData_new")
            val messageUpdate = intent.getStringExtra("orderData_update")
            val id = intent.getIntExtra("OrderData_canceled", -1)
            val driverId = intent.getIntExtra("driver_id", -1)

            val orderStatus = intent.getStringExtra(SocketConfig.ORDER_CANCELLED)


            if (((driverId > 0 && userPreferenceManager.getDriverID() == driverId) && id > 0) || orderStatus == NO_CONNECT) {
                userPreferenceManager.setDriverStatus(UserPreferenceManager.DriverStatus.COMPLETED)
                viewModel.stopDrive()
                userPreferenceManager.clearPassengerPhone()
                userPreferenceManager.timeClear()
                userPreferenceManager.setIsOrderCancel(true)
                navigateToDashboardActivity()
            }

            if (id > 0 && (orderStatus == SocketConfig.ORDER_CANCELLED || orderStatus == SocketConfig.ORDER_ACCEPTED)) {
                orderViewModel.removeItem(id)
            }

            val type = Types.newParameterizedType(
                SocketMessage::class.java, SocketOnlyForYouData::class.java
            )
            val adapter: JsonAdapter<SocketMessage<SocketOnlyForYouData>> = moshi.adapter(type)

            val orderData = message?.let { adapter.fromJson(it) }
            val orderDataUpdate = messageUpdate?.let { adapter.fromJson(it) }
            orderData?.let {
                orderViewModel.addItem(orderItem = it.data.toOrderData())
            }

            orderDataUpdate?.let {
                if (userPreferenceManager.getDriverStatus() != UserPreferenceManager.DriverStatus.COMPLETED && orderDataUpdate.data.id == userPreferenceManager.getOrderId()) {
                    userPreferenceManager.saveStartCostUpdate(it.data.startCost)
                } else {
                    orderViewModel.updateItem(updateItem = it.data.toOrderData())
                }
            }
            // Do something with the orderData
        }
    }
    private val activityMessenger = ActivityMessenger(
        ::onStatusUpdate, ::onDriveFinished
    )

    override fun onCreate(savedInstanceState: Bundle?) {
//        injectFeature()
        super.onCreate(savedInstanceState)
        val filter = IntentFilter("com.example.taxi.ORDER_DATA_ACTION")
        registerReceiver(receiver, filter)
        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    MY_REQUEST_CODE
                )
            }
        }


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.homeFragNavHost) as NavHostFragment
        navController = navHostFragment.navController

        if (intent.getBooleanExtra("navigate_to_order", false)) {
            val lat1 = intent.getStringExtra("lat1")
            val lat2 = intent.getStringExtra("lat2")
            val long1 = intent.getStringExtra("long1")
            val long2 = intent.getStringExtra("long2")
            val orderId = intent.getIntExtra("order_id", -1)
            Log.d("lokatsiya", "olindi: long2 = ${long2} , lat2 = ${lat2}")

            navigateToOrderFragment(orderId, lat1, long1, lat2, long2)
        }


        driveReportViewModel.checkAndUpdateDrive()


        startService(Intent(this, DriveBackGroundService::class.java))

        viewModel.startStopLiveData.observe(this) {
            when (it) {
                START -> {
                    activityMessenger.startDrive()
                    viewBinding.root.keepScreenOn = true
                }

                PAUSE -> {
                    activityMessenger.pauseDrive()
                    viewBinding.root.keepScreenOn = false
                }

                STOP -> {
                    activityMessenger.stopDrive()
                    viewBinding.root.keepScreenOn = false
                }
            }
        }


//        startLocationUpdates()


    }

//    private fun completeOrderUi(data: Resource<MainResponse<Any>>?) {
//        when(data?.state){
//            ResourceState.LOADING ->{
//                Log.d("internet1", "completeOrderUi: loading")
//            }
//            ResourceState.SUCCESS ->{
//                userPreferenceManager.saveLastRaceId(-1)
//                driverViewModel.completedOrder()
//                Log.d("internet1", "completeOrderUi: suc")
//
//            }
//            ResourceState.ERROR ->{
//                Log.d("internet1", "completeOrderUi: eror")
//                if (data.data?.status != 400){
//                    checkAndCompleteOrderLostNetwork()
//                }else{
//                    userPreferenceManager.saveLastRaceId(-1)
//
//                }
//            }
//
//            else -> {}
//        }
//    }

//    private fun checkAndCompleteOrderLostNetwork() {
//        if (userPreferenceManager.getLastRaceId() > 0) {
//            driverViewModel.completeOrderLostNetwork(
//                userPreferenceManager.getLastRace()
//            )
//        }
//    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    companion object {
        fun open(activity: Activity) {
            val intent = Intent(activity, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
            activity.finish()
        }
    }


    override fun onStart() {
        super.onStart()

        if (userPreferenceManager.getIsOrderCancel()) {
            val d = DialogUtils.orderCancelDialog(this)
            d.show()
            if (d.isShowing) {
                userPreferenceManager.setIsOrderCancel(false)
            }

        }
        bindService(
            Intent(this, DriveBackGroundService::class.java), this, Context.BIND_AUTO_CREATE
        )
        val currentDestinationId = navController.currentDestination?.id
        if (userPreferenceManager.getStatusIsTaximeter()) {
            if (userPreferenceManager.getDriverStatus() != UserPreferenceManager.DriverStatus.COMPLETED) {
                when (userPreferenceManager.getDriverStatus()) {
                    UserPreferenceManager.DriverStatus.STARTED -> {
                        Log.d("taxometer", "onStart: start")
                        driverViewModel.startedOrder()
                        Log.d("xatolik", "onStart:start ishladi ")
                    }
                    UserPreferenceManager.DriverStatus.ARRIVED ->{
                        driverViewModel.arrivedOrder()
                        Log.d("taxometer", "onStart: arrive")

                    }
                    UserPreferenceManager.DriverStatus.ACCEPTED ->{
                        driverViewModel.acceptedOrder()
                        Log.d("taxometer", "onStart: accept")

                    }

                    else -> {}
                }
                navController.navigate(R.id.taximeterFragment)
            }
        } else {
            if (currentDestinationId != R.id.driverFragment) {
                if (userPreferenceManager.getDriverStatus() != UserPreferenceManager.DriverStatus.COMPLETED) {
                    when (userPreferenceManager.getDriverStatus()) {
                        UserPreferenceManager.DriverStatus.STARTED -> {
                            driverViewModel.startedOrder()
                            Log.d("xatolik", "onStart:start ishladi ")
                        }

                        UserPreferenceManager.DriverStatus.ARRIVED -> driverViewModel.arrivedOrder()
                        UserPreferenceManager.DriverStatus.ACCEPTED -> driverViewModel.acceptedOrder()
                        else -> {}
                    }
                    navController.navigate(R.id.driverFragment)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        unbindService(this)
    }

    private fun onStatusUpdate(dashboardData: DashboardData) {
        viewModel.updateDashboardData(dashboardData)
    }

    private fun onDriveFinished(raceId: Long) {
        driveReportViewModel.setInitData(driveId = raceId)
        val dialog = DriveFinishDialog(raceId = raceId, driveReportViewModel)
        dialog.show(supportFragmentManager, "DriveFinishDialog")
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        viewModel.syncServices()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        activityMessenger.onDisconnect()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        activityMessenger.onConnect(Messenger(service))
        activityMessenger.handShake()
    }

    override fun onBackPressed() {
        if (!checkAndEnterPipMode()) {
            super.onBackPressed()
        }
    }


    private fun checkAndEnterPipMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            )
        ) {
            if (viewModel.dashboardLiveData.value?.isRunning() == true || userPreferenceManager.getDriverStatus().name != UserPreferenceManager.DriverStatus.COMPLETED.name) {
                return enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1)).build()
                )
            }
        }

        return false
    }

    override fun onUserLeaveHint() {
        checkAndEnterPipMode()
    }

    @SuppressLint("NewApi", "MissingSuperCall")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            viewBinding.priceTxtMini.visibility = View.VISIBLE
            viewBinding.homeFragNavHost.visibility = View.GONE
//            viewBinding.bottomNavigation.visibility = View.GONE
            viewModel.dashboardLiveData.observe(this) {
                viewBinding.priceTxtMini.text = (it.getDistanceText()).toString()
            }
        } else {
            viewBinding.priceTxtMini.visibility = View.GONE
            viewBinding.homeFragNavHost.visibility = View.VISIBLE
//            viewBinding.bottomNavigation.visibility = View.VISIBLE
            viewModel.dashboardLiveData.removeObservers(this)
        }
    }

    private fun startKillStateDialogService() {
        val intent = Intent(this, KillStateDialogService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (hasOverlayPermission()) {
                startKillStateDialogService()
            } else {
                // Overlay permission not granted, handle accordingly (e.g., show error message)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startKillStateDialogService()
            } else {
                // Permission not granted, handle accordingly (e.g., show error message)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("navigate_to_order", false) == true) {
            val lat1 = intent.getStringExtra("lat1")
            val lat2 = intent.getStringExtra("lat2")
            val long1 = intent.getStringExtra("long1")
            val long2 = intent.getStringExtra("long2")
            val orderId = intent.getIntExtra("order_id", -1)
            Log.d("lokatsiya", "ichida: long2 = ${long2} , lat2 = ${lat2}")

            navigateToOrderFragment(orderId, lat1, long1, lat2, long2)
        }
    }

    private fun navigateToOrderFragment(
        id: Int, lat1: String?, long1: String?, lat2: String?, long2: String?
    ) {
        Log.d("lokatsiya", "funksiya: long2 = ${long2} , lat2 = ${lat2}")

        val bundle = Bundle().apply {
            putInt("order_id", id)
            putString("lat1", lat1)
            putString("lat2", lat2)
            putString("long1", long1)
            putString("long2", long2)
        }

        val navOptions = NavOptions.Builder().setPopUpTo(R.id.homeFragNavHost, true).build()
        navController.navigate(R.id.orderFragment, bundle, navOptions)
    }

    override fun attachBaseContext(newBase: Context) {
        val newContext = ViewUtils.setLanguage(newBase, userPreferenceManager.getLanguage())
        super.attachBaseContext(newContext)
    }

    private fun navigateToDashboardActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

    }


}
