package com.example.taxi.ui.taxometer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taxi.R
import com.example.taxi.databinding.FragmentDriverBinding
import com.example.taxi.domain.drive.currentDrive.CurrentDriveStatus
import com.example.taxi.domain.model.DashboardData
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.map.MapLocation
import com.example.taxi.domain.model.order.OrderAccept
import com.example.taxi.domain.model.order.UserModel
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.DriveAction
import com.example.taxi.ui.home.HomeViewModel
import com.example.taxi.ui.home.driver.DriverViewModel
import com.example.taxi.ui.home.driver.TAG1
import com.example.taxi.utils.ButtonUtils
import com.example.taxi.utils.ConstantsUtils
import com.example.taxi.utils.DialogUtils
import com.example.taxi.utils.Event
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.tapadoo.alerter.Alerter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TaximeterFragment : Fragment() {

    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentBottomStatus = DriveAction.ARRIVED

    private val driverViewModel: DriverViewModel by sharedViewModel()

    private val preferenceManager: UserPreferenceManager by inject()
    private val homeViewModel: HomeViewModel by sharedViewModel()
    private var lastStatus = -1

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableLocationComponent()
            }
        }

    lateinit var viewBinding: FragmentDriverBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = FragmentDriverBinding.inflate(inflater, container, false)
        setUpSomeUiConfig()
        return viewBinding.root
    }

    private fun setUpSomeUiConfig() {
        viewBinding.bottomDialog.bttomcola.visibility = View.GONE
        viewBinding.bottomDialogTaxometer.bttomcola.visibility = View.VISIBLE
        viewBinding.buttonNavigator.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpUi()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        viewBinding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            // Style loaded
            // Check permission
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) -> {
                    enableLocationComponent()
                }

                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                val lastLocation: Location = locationResult.lastLocation!!

                viewBinding.mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(lastLocation.longitude, lastLocation.latitude))
                        .zoom(15.0)
                        .build()
                )
            }
        }

        viewBinding.bottomDialogTaxometer.swipeButton.onSwipedOnListener = {
            Log.d("zakaz", "onViewCreated: $currentBottomStatus")
            when (currentBottomStatus) {

                DriveAction.STARTED -> {
                    Log.d("zakaz", "onViewCreated: started")

                    viewBinding.bottomDialogTaxometer.swipeButton.checkedText =
                        getString(R.string.downloading)
                    setFinishDialog()

                }

                DriveAction.ACCEPT -> {
                    preferenceManager.saveStatusIsTaximeter(true)
                    Log.d("zakaz", "onViewCreated: arrived")

                    viewBinding.bottomDialogTaxometer.swipeButton.checkedText =
                        getString(R.string.downloading)
                    viewBinding.bottomDialogTaxometer.swipeButton.uncheckedText = getString(R.string.finish)
                    driverViewModel.acceptWithTaximeter()
                }

                DriveAction.COMPLETED -> {


                }
            }
        }
    }

    private fun setFinishDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_ask_finish)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        preferenceManager.saveStatusIsTaximeter(false)

        val finishButton: MaterialButton = dialog.findViewById(R.id.button_finish_order)
        val cancelButton: MaterialButton = dialog.findViewById(R.id.button_cancel_order)

        finishButton.setOnClickListener {

//            homeViewModel.startDrive()

            preferenceManager.setDriverStatus(UserPreferenceManager.DriverStatus.COMPLETED)

            homeViewModel.stopDrive()
            viewBinding.parentContainer.keepScreenOn = false
            dialog.dismiss()

            driverViewModel.completedOrder()

        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
            viewBinding.bottomDialogTaxometer.swipeButton.isChecked = false

        }

        dialog.setOnDismissListener {
            dialog.dismiss()
            viewBinding.bottomDialogTaxometer.swipeButton.isChecked = false
        }

        dialog.show()
    }

    private fun setUpUi() {
        viewBinding.callDispetcher.setOnClickListener {
            ButtonUtils.callToDispatcher(requireActivity())
        }

        homeViewModel.dashboardLiveData.observe(viewLifecycleOwner) {
            setDashboardStatus(it)
        }

        driverViewModel.orderStartCompleteLiveData.observe(viewLifecycleOwner) {
            setBottomSheetSetting(it)
        }

        driverViewModel.acceptWithTaximeter.observe(viewLifecycleOwner){
            acceptOrderUi(it)
        }
    }

    private fun acceptOrderUi(resourceEvent: Event<Resource<MainResponse<OrderAccept<UserModel>>>>?) {
        resourceEvent?.getContentIfNotHandled()?.let { resource ->

            when (resource.state) {
                ResourceState.LOADING -> {}

                ResourceState.ERROR -> {
                    activity?.let { it1 ->
                        resource.message?.let { it2 ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                DialogUtils.createChangeDialog(
                                    activity = requireActivity(),
                                    title = "Xatolik!",
                                    message = it2,
                                    color = R.color.tred
                                )
                            }
                        }
                    }
                    viewBinding.bottomDialogTaxometer.swipeButton.isChecked = false
//                    orderViewModel.clearAcceptOrderData()
                }

                ResourceState.SUCCESS -> {
                    viewBinding.bottomDialogTaxometer.swipeButton.isChecked = false

                    val orderSettings = resource.data?.data
                    orderSettings?.let { it1 -> preferenceManager.savePriceSettings(it1) }
                    driverViewModel.startedOrder()
                    homeViewModel.startDrive()
                    preferenceManager.setDriverStatus(UserPreferenceManager.DriverStatus.ACCEPTED)
                    preferenceManager.saveLastRaceId(-1)

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        viewBinding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_navigation_puck_icon
                )
            )
            enabled = true
            Log.d("Mapbox", "Location puck and provider set")
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                val lastLocation = locationResult.lastLocation
                Log.d("Mapbox", "Received location update")
                viewBinding.mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(lastLocation?.longitude?.let {
                            com.mapbox.geojson.Point.fromLngLat(
                                it, lastLocation.latitude
                            )
                        })
                        .zoom(15.0)
                        .build()
                )
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }


    @SuppressLint("StringFormatInvalid")
    private fun setDashboardStatus(dashboardData: DashboardData?) {

        if (lastStatus != dashboardData?.getStatus()) {
            lastStatus = dashboardData?.getStatus()!!
            when (dashboardData.getStatus()) {

                CurrentDriveStatus.STARTING -> {
                    viewBinding.bottomDialog.icPauseStart.setImageDrawable(
                        ActivityCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_pause
                        )
                    )

                    viewBinding.bottomDialogTaxometer.timeWorkTextView.visibility = View.VISIBLE
                    viewBinding.bottomDialogTaxometer.descTextView.visibility = View.GONE
                    viewBinding.bottomDialogTaxometer.currentDetailTime.text =
                        getString(R.string.buyurtmada)

                    Alerter.create(activity = requireActivity())
                        .setTitle(title = getString(R.string.recconnect))
                        .enableProgress(true)
                        .setDismissable(false)
                        .setText("${getString(R.string.recconnect)}:  ${dashboardData.getGPSSignalStrength()}")
                        .setBackgroundColorRes(colorResId = R.color.blue)
                        .show()

                    viewBinding.parentContainer.keepScreenOn = true
                }

                CurrentDriveStatus.STARTED -> {


                    Alerter.hide()
                    viewBinding.bottomDialog.icPauseStart.setImageDrawable(
                        ActivityCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_pause
                        )
                    )

                    viewBinding.bottomDialogTaxometer.currentDetailTime.text =
                        getString(R.string.buyurtmada)

                    viewBinding.bottomDialogTaxometer.timeWorkTextView.visibility = View.VISIBLE
                    viewBinding.parentContainer.keepScreenOn = true
                }

                CurrentDriveStatus.PAUSED -> {
                    Alerter.hide()
                    viewBinding.bottomDialog.icPauseStart.setImageDrawable(
                        ActivityCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_play
                        )
                    )

                    viewBinding.bottomDialog.currentDetailTime.text =
                        getString(R.string.pulli_kutish)


//                    viewBinding.timeCard.visibility = View.VISIBLE
                    viewBinding.bottomDialog.progressTimeTextView.visibility = View.VISIBLE
                    viewBinding.bottomDialog.timeWorkTextView.visibility = View.GONE
                    viewBinding.parentContainer.keepScreenOn = false
                }

                CurrentDriveStatus.STOPPED -> {
                    Alerter.hide()
                    viewBinding.bottomDialog.icPauseStart.setImageDrawable(
                        ActivityCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_pause
                        )
                    )
//                    viewBinding.timeCard.visibility = View.GONE

                    viewBinding.parentContainer.keepScreenOn = false
                }
            }
        }

        var secondsElapsed = 0





        viewBinding.bottomDialogTaxometer.timeWorkTextView.text = dashboardData.timeText()
    }

    private fun setBottomSheetSetting(state: Int?) {
        Log.d("zakaz", "setBottomSheetSetting: ozgardi $state ")
        if (currentBottomStatus != state) {
            currentBottomStatus = state!!
        }
        when(state){
            DriveAction.STARTED ->{

            }
        }
    }

}