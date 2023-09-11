package com.example.taxi.ui.home.order

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taxi.R
import com.example.taxi.SingleLocationProvider
import com.example.taxi.databinding.FragmentOrderBinding
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.map.MapLocation
import com.example.taxi.domain.model.order.Address
import com.example.taxi.domain.model.order.OrderAccept
import com.example.taxi.domain.model.order.OrderData
import com.example.taxi.domain.model.order.UserModel
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.socket.SocketService
import com.example.taxi.ui.home.driver.DriverViewModel
import com.example.taxi.utils.*
import com.example.taxi.utils.ConstantsUtils.locationDestination
import com.example.taxi.utils.ConstantsUtils.locationDestination2
import com.example.taxi.utils.ConstantsUtils.locationStart
import com.example.taxi.utils.LocationHandler.isGpsEnabled
import com.example.taxi.utils.LocationHandler.isLocationApproved
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class OrderFragment : Fragment(), BottomSheetInterface {

    lateinit var viewBinding: FragmentOrderBinding
    private val orderViewModel: OrderViewModel by sharedViewModel()
    private lateinit var orderAdapter: OrderAdapter
    private val driverViewModel: DriverViewModel by sharedViewModel()
    lateinit var singleLocationProvider: SingleLocationProvider
    private val preferenceManager: UserPreferenceManager by inject()
    private var locationCallback: LocationCallback? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val handler = CoroutineExceptionHandler { _, exception ->
        isRouteFetched = false
    }
    var dialog: Dialog? = null

    var isNewOrder: Boolean = false

    private var destLang: Double? = 0.0
    private var destLong: Double? = 0.0

    private var isRouteFetched = false

    private var myCurrentAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var orderId: Int = -1
        var lat1: String? = null
        var long1: String? = null
        var lat2: String? = null
        var long2: String? = null

        arguments?.let {
            orderId = it.getInt("order_id", -1)
            lat1 = it.getString("lat1")
            long1 = it.getString("long1")
            lat2 = it.getString("lat2")
            long2 = it.getString("long2")
        }
        Log.d("lokatsiya", "orderId: $orderId, lat1: $lat1, long1: $long1, long2: $long2, lat2: $lat2")

        if (orderId > 0 && lat1 != null && long1 != null) {
            acceptOrder(
                id = orderId,
                latitude1 = lat1!!,
                longitude1 = long1!!,
                latitude2 = lat2,
                longitude2 = long2
            )
        }


    }

    private fun acceptedOrder(resource: Resource<MainResponse<OrderAccept<UserModel>>>?) {
        resource?.let {
            when (resource.state) {
                ResourceState.LOADING -> {

                }
                ResourceState.ERROR -> {
                    dialog?.dismiss()
                    activity?.let { it1 ->
                        it.message?.let { it2 ->
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
                    orderViewModel.clearAcceptOrderData()
                }
                ResourceState.SUCCESS -> {
                    val orderSettings = it.data?.data
                    orderSettings?.let { it1 -> preferenceManager.savePriceSettings(it1) }

                    Log.d("qabul", "acceptedOrder: long ${it.data?.data?.longitude2} lat: ${it.data?.data?.latitude2}")
                    if (it.data?.data?.latitude2 != null && it.data.data.longitude2 != null) {
                        locationDestination2 = MapLocation(it.data.data.latitude2!!.toDouble(), it.data.data.longitude2!!.toDouble())

                    }
                    dialog?.dismiss()
                    preferenceManager.setDriverStatus(UserPreferenceManager.DriverStatus.ACCEPTED)
                    driverViewModel.acceptedOrder()
                    val navController = findNavController()
                    orderViewModel.clearAcceptOrderData()
                    changeDriveStatus()
                    navController.navigate(R.id.driverFragment)
                }
            }
        }
    }

    private fun changeDriveStatus() {
        // When drive status changes
        val intent = Intent("com.example.SEND_TO_SOCKET")
        intent.putExtra("data", "Your data here")
        requireContext().sendBroadcast(intent)
    }
    @SuppressLint("MissingPermission")
    private fun updateUi(resource: Resource<MainResponse<List<OrderData<Address>>>>?) {
        resource?.let {
            when (resource.state) {
                ResourceState.LOADING -> {
                    shimmerLoading()
                }
                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        Log.d("orderuchun", "updateUi: ${it.data}")
                        if (it.data.isEmpty()) {
                            noOrderShow()
                        } else {
                            fusedLocationClient =
                                LocationServices.getFusedLocationProviderClient(requireActivity())
                            fusedLocationClient!!.lastLocation.addOnSuccessListener { location ->
                                receiveItemFromSocket(it.data, location)

                            }

                            orderShow()

                        }
                    }
                }
                ResourceState.ERROR -> {

                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = FragmentOrderBinding.inflate(layoutInflater, container, false)
        if (dialog == null) {
            dialog = DialogUtils.loadingDialog(requireContext())
        }
        return viewBinding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        orderViewModel.isNewOrder.observe(viewLifecycleOwner) {
            isNewOrder = it
        }
        orderViewModel.orderResponse.observe(viewLifecycleOwner) {
            Log.d("itemuchun", "onCreate: sezish $it")
            updateUi(it)
        }

        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        singleLocationProvider = SingleLocationProvider(locationManager)


        orderViewModel.acceptOrder.observe(viewLifecycleOwner) {
            acceptedOrder(it)
        }


        with(viewBinding) {

            fbnBackHome.setOnClickListener {
                val navController = findNavController()
                navController.navigateUp()
            }

        }
    }

    private fun noOrderShow() {
        with(viewBinding) {
            noOrder.visibility = View.VISIBLE
            recyclerViewOrder.visibility = View.GONE
            shimmerOrder.stopShimmer()
            shimmerOrder.visibility = View.GONE
        }
    }

    private fun orderShow() {
        with(viewBinding) {
            noOrder.visibility = View.GONE
            recyclerViewOrder.visibility = View.VISIBLE
            shimmerOrder.stopShimmer()
            shimmerOrder.visibility = View.GONE
        }
    }

    private fun shimmerLoading() {
        with(viewBinding) {
            noOrder.visibility = View.GONE
            recyclerViewOrder.visibility = View.GONE
            shimmerOrder.startShimmer()
            shimmerOrder.visibility = View.VISIBLE
        }
    }

    private fun receiveItemFromSocket(orderData: List<OrderData<Address>>, location: Location) {
        Log.d("orderuchun", "receiveItemFromSocket: $orderData")
        val soundPlayer = SoundPlayer(SoundPlayer.SoundType.LowSound, requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            // Execute addItemToRecyclerView in parallel with playSound
            val addItemJob = async(Dispatchers.IO) {
                activity?.runOnUiThread {
                    orderAdapter = OrderAdapter(
                        orderData,
//                                    singleLocationProvider.getLocation(requireContext()),
                        location,
                        this@OrderFragment
                    )
                    viewBinding.recyclerViewOrder.adapter = orderAdapter

//                    orderAdapter.addItemToRecyclerView(orderItem = orderData)
                }
            }
            val playSoundJob = async(Dispatchers.IO) {
                if (isNewOrder) {
                    soundPlayer.playSound()

                }
            }

            // Await both jobs to complete
            addItemJob.await()
            playSoundJob.await()

            // Update the UI after the coroutines have completed
            // Perform any additional UI-related operations here
        }

    }

    override fun showBottom(orderData: OrderData<Address>, distance: String) {

        val myBottomSheet = MyBottomSheet(
            requireContext(),
            R.style.BottomSheetDialogTheme,
            orderData = orderData,
            distance = distance,
            bottomSheetInterface = this
        )
        val bottomView =
            LayoutInflater.from(context).inflate(R.layout.bottom_sheet_order_accepted, null)
        myBottomSheet.setContentView(bottomView)
        myBottomSheet.show()

    }

//    override fun acceptOrder(id: Int, latitude1: String, longitude1: String) {
//
//
//            if (requireActivity().isLocationApproved()) {
//                if (requireActivity().isGpsEnabled()) {
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        getCurrentLocation()
//                    }, 300)
//                } else {
//                    try {
//                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                    } catch (e: Exception) {
//
//                    }
//                }
//
//            } else {
//                requestPermissions(
//                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                    LocationHandler.LOCATION_PERMISSION
//                )
//            }
//            onclickMethod(id, latitude1.toDouble(), longitude1.toDouble())
//
//
//
//    }
//
//
//    private fun onclickMethod(id: Int, latitude: Double, longitude: Double) {
//        locationDestination = MapLocation(latitude, longitude)
//        orderViewModel.acceptOrder(id)
//    }
//
//
//    @SuppressLint("MissingPermission")
//    private fun getCurrentLocation() {
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
//        fusedLocationClient.lastLocation
//            .addOnSuccessListener { location: Location? ->
//                if (location != null) {
//                    locationStart.placeLatitude = location.latitude
//                    locationStart.placeLongitude = location.longitude
//
//                }
//            }
//
//    }


    override fun acceptOrder(
        id: Int,
        latitude1: String,
        longitude1: String,
        latitude2: String?,
        longitude2: String?
    ) {
        if (requireActivity().isLocationApproved()) {
            if (requireActivity().isGpsEnabled()) {
                if (dialog == null) {
                    dialog = DialogUtils.loadingDialog(requireContext())
                }
                dialog?.show()

                if (isHuaweiDevice()) {
                    Log.e("tekshirish", "acceptOrder: bu huawei")
                    startHuaweiLocationUpdates(
                        id,
                        latitude = latitude1.toDouble(),
                        longitude = longitude1.toDouble(),
                        latitude2 = latitude2?.toDouble(),
                        longitude2 = longitude2?.toDouble()
                    )
                } else {
                    startStandardLocationUpdates(
                        id,
                        latitude = latitude1.toDouble(),
                        longitude = longitude1.toDouble(),
                        latitude2 = latitude2?.toDouble(),
                        longitude2 = longitude2?.toDouble()
                    )
                }
            } else {
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    // Handle exception, if required
                }
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LocationHandler.LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startStandardLocationUpdates(
        id: Int,
        latitude: Double,
        longitude: Double,
        latitude2: Double?,
        longitude2: Double?
    ) {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { location ->
                    locationStart.placeLatitude = location.latitude
                    locationStart.placeLongitude = location.longitude
                    stopStandardLocationUpdates()
                    onclickMethod(id, latitude, longitude, latitude2, longitude2)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback, Looper.getMainLooper()
        )
    }

    private fun stopStandardLocationUpdates() {
        fusedLocationClient?.let {
            locationCallback?.let { it1 -> it.removeLocationUpdates(it1) }
            locationCallback = null
            fusedLocationClient = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun startHuaweiLocationUpdates(
        id: Int,
        latitude: Double,
        longitude: Double,
        latitude2: Double?,
        longitude2: Double?
    ) {
        val locationRequest = LocationRequest().apply {
            interval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { location ->
                    locationStart.placeLatitude = location.latitude
                    locationStart.placeLongitude = location.longitude
                    stopHuaweiLocationUpdates()
                    onclickMethod(id, latitude, longitude, latitude2, longitude2)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback, Looper.getMainLooper()
        )
    }

    private fun stopHuaweiLocationUpdates() {
        fusedLocationClient?.let {
            locationCallback?.let { it1 -> it.removeLocationUpdates(it1) }
            locationCallback = null
            fusedLocationClient = null
        }
    }

    private fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return manufacturer.equals("HUAWEI", ignoreCase = true)
    }

    private fun onclickMethod(
        id: Int,
        latitude: Double,
        longitude: Double,
        latitude2: Double?,
        longitude2: Double?
    ) {

        locationDestination = MapLocation(latitude, longitude)
        if (locationDestination2 != null) {
            locationDestination2 =
                latitude2?.let { longitude2?.let { it1 -> MapLocation(it, it1) } }
        }

        orderViewModel.acceptOrder(id)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopStandardLocationUpdates()
        stopHuaweiLocationUpdates()
    }


}