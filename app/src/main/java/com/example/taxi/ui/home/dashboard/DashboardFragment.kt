package com.example.taxi.ui.home.dashboard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.soundmodule.SoundManager
import com.example.taxi.R
import com.example.taxi.databinding.FragmentDashboardBinding
import com.example.taxi.di.MAIN_URL
import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.balance.BalanceData
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.domain.model.settings.SettingsData
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.network.NetworkReceiver
import com.example.taxi.socket.SocketMessageProcessor
import com.example.taxi.socket.SocketRepository
import com.example.taxi.socket.SocketService
import com.example.taxi.ui.home.SocketViewModel
import com.example.taxi.ui.home.driver.DriverViewModel
import com.example.taxi.ui.home.order.OrderViewModel
import com.example.taxi.ui.login.LoginActivity
import com.example.taxi.ui.permission.PermissionCheckActivity
import com.example.taxi.utils.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


const val TAG = "bekor"
class DashboardFragment : Fragment() {

    private lateinit var viewBinding: FragmentDashboardBinding
    private var isViewCreated = false
    private lateinit var soundManager: SoundManager
    private lateinit var networkReceiver: NetworkReceiver

    val navController by lazy {
        findNavController()
    }
    private lateinit var socketRepository: SocketRepository
    private val socketMessageProcessor: SocketMessageProcessor by inject()

    private val socketStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isConnected = intent?.getBooleanExtra("IS_CONNECTED", false) ?: false
            updateSocket(isConnected)

        }
    }

    private val socketViewModel: SocketViewModel by viewModels()
    private val orderViewModel: OrderViewModel by sharedViewModel()
    private val driverViewModel: DriverViewModel by sharedViewModel()
    private val dashboardViewModel: DashboardViewModel by viewModel()
    private val userPreferenceManager: UserPreferenceManager by inject()

    var isNewOrder: Boolean = false

    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (LocationPermissionUtils.isLocationEnabled(requireContext())
                && LocationPermissionUtils.isBasicPermissionGranted(requireContext())
            ) {
                startOrder()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerSocketStatusReceiver()

    }

    private fun registerSocketStatusReceiver() {
        context?.registerReceiver(socketStatusReceiver, IntentFilter("SOCKET_STATUS"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        isViewCreated = true
        viewBinding = FragmentDashboardBinding.inflate(inflater, container, false)

//        initializeNetworkReceiver()


        setToggleButtonUi(userPreferenceManager.getToggleState())

        return viewBinding.root

    }

    private fun initializeNetworkReceiver() {
        networkReceiver = NetworkReceiver { isConnected ->
            if (isConnected) {
                viewLifecycleOwner.lifecycleScope.launch {
                    checkAndCompleteOrderLostNetwork()
                }
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context?.registerReceiver(networkReceiver, filter)
    }


    private fun checkAndCompleteOrderLostNetwork() {
        if (userPreferenceManager.getLastRaceId() == 1) {
            view?.let {
                try {
                    if (findNavController().currentDestination?.id == R.id.dashboardFragment) {
                        // DashboardFragment is the current fragment
                        Log.d(TAG, "checkAndCompleteOrderLostNetwork: bu dashboard")
                        driverViewModel.completeOrderLostNetwork(
                            userPreferenceManager.getLastRace()
                        )
                    } else {
                        // DashboardFragment is not the current fragment
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundManager = SoundManager(requireContext())
        getAllData()
        viewBinding.refresh.setOnRefreshListener {
            getAllData()
        }

        userPreferenceManager.timeClear()

        socketRepository = SocketRepository(
            context = requireContext(),
//            socketViewModel = socketViewModel,
            socketMessageProcessor = socketMessageProcessor,
            viewModelScope = socketViewModel.viewModelScope
        )


        setupView()
        setupObservers()




        if (!userPreferenceManager.getIsSeenIntro()) {
            SpotlightUtils.showIntroTarget(requireActivity(), viewBinding, userPreferenceManager)
        }


        val menu = MenuClass(requireContext(), preferenceManager = userPreferenceManager)
        addGroupsToNavigationView(
            viewBinding.navigationView,
            menu.getMenu(),
            menu.getItemActionView()
        )

        viewBinding.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                // 456 -> id change language layout
                456 -> {
                    navController.navigate(R.id.choosingLanguageFragment)
                    true
                }

                // 233 -> id taximeter layout
                233 -> {
                    navController.navigate(R.id.taximeterFragment)
                    true
                }
                // 987 -> id about us layout
                987 -> {
                    navController.navigate(R.id.aboutFragment)
                    true
                }
                // 988 -> id FAQ
                988 -> {
                    navController.navigate(R.id.FAQFragment)
                    true
                }
                985 ->{
                    navController.navigate(R.id.shareQRFragment)
                    true
                }
                // 990 -> id for log-out
                990 -> {
                    DialogUtils.showConfirmationDialog(requireContext()) {
                        // Clear preferences first
                        userPreferenceManager.clear()

                        // Then start the new activity
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        // Clear all the previous activities from the stack
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    true
                }

                else -> {
                    true
                }
            }
        }

        with(viewBinding) {


            drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerOpened(drawerView: View) {
                    if (!userPreferenceManager.getIsSeenStatus()) {
                        SpotlightUtils.showSpotlight(
                            requireActivity(),
                            viewBinding,
                            userPreferenceManager
                        )
                    }
                }
            })
        }

        // open call page
    }

    private fun getAllData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "getAllData: Started")
                getDashboardData()
                getOrderData()
                checkAndCompleteOrderLostNetwork()

            } catch (e: Exception) {
                Log.e(TAG, "getAllData: Error occurred", e)
                // Consider showing a message to the user to inform them about the error
            }
        }
    }

    private fun getDashboardData() {
        dashboardViewModel.getBalance()
        dashboardViewModel.getSettings()
        dashboardViewModel.getDriverData()
    }
    private fun getOrderData() {
        orderViewModel.getOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewCreated = false
        Log.d(TAG, "onDestroyView: ")
//        context?.unregisterReceiver(networkReceiver)
//        disposable.dispose()  // Dispose the observer to prevent memory leaks
    }

    private fun connectToSocket() {
        initSocket()
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
//        context?.unregisterReceiver(networkReceiver)
        lifecycleScope.cancel()
    }


    private fun initSocket() {
        val token = userPreferenceManager.getToken()
        token?.let { socketRepository.initSocket(token = it) }
    }

    private fun setTextViews(data: SelfieAllData<IsCompletedModel, StatusModel>?, view: View) {
        val texts = mapOf(
            R.id.driverNameTextView to data?.first_name,
            R.id.driverPhoneNumberTextView to data?.phone,
            R.id.driver_id_header to "ID ${data?.id}",
            R.id.status_text to data?.status?.string
        )

        for ((id, text) in texts) {
            text?.let { view.findViewById<TextView>(id)?.convertToCyrillic(it) }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun driverUpdate(resources: Resource<MainResponse<SelfieAllData<IsCompletedModel, StatusModel>>>?) {
        resources?.let { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    resource.data?.data?.let { data ->
                        viewBinding.navigationView.getHeaderView(0).apply {
                            setTextViews(data, this)
                            userPreferenceManager.saveDriverAllData(data)
                            findViewById<ImageView>(R.id.circleImageView)?.let { imageView ->
                                Glide.with(requireActivity())
                                    .load("${MAIN_URL}${data.photo}")
                                    .placeholder(R.drawable.error_placeholder)
                                    .error(R.drawable.error_placeholder) // provide an error placeholder
                                    .into(imageView)
                            }

                            val (statusIcon, color) = when (data.status.int) {
                                10 -> Pair(R.drawable.icons8_correct, "#804CB04F")
                                else -> Pair(R.drawable.icons8_error, "#80F54E59")
                            }

                            findViewById<ImageView>(R.id.status_icon)?.setImageResource(statusIcon)
                            findViewById<LinearLayout>(R.id.active_status_shape)?.backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor(color))

                            viewBinding.menuError.visibility =
                                if (data.status.int == 10) View.GONE else View.VISIBLE


                        }
                    }
                }

                ResourceState.LOADING -> {
                    // Show loading state
                }

                ResourceState.ERROR -> {
                    if (resource.message == "401") {
                        userPreferenceManager.clear()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.not_found_driver),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Then start the new activity
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        // Clear all the previous activities from the stack
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                    }
                    // Handle the error
                }
            }
        }
    }

    private fun startOrder() {
        navController.navigate(R.id.orderFragment)
    }

    private fun updateData(resource: Resource<MainResponse<List<SettingsData>>>?) {
        resource?.let {
            when (resource.state) {
                ResourceState.LOADING -> {
                    // Show loading state
                }

                ResourceState.SUCCESS -> {

                }

                ResourceState.ERROR -> {
//                    val errorMessage = resource.message
                    // Handle the error
                }
            }
        }

    }

    private fun completeOrderUi(data: Resource<MainResponse<Any>>?) {
        when (data?.state) {
            ResourceState.LOADING -> {
                viewBinding.buttonOrder.isEnabled = false

            }

            ResourceState.SUCCESS -> {
                userPreferenceManager.saveLastRaceId(-1)
                viewBinding.buttonOrder.isEnabled = true

            }

            ResourceState.ERROR -> {
                if (data.message == "400") {
                    userPreferenceManager.saveLastRaceId(-1)
                    viewBinding.buttonOrder.isEnabled = true
                } else {
                    userPreferenceManager.saveLastRaceId(1)
                    viewBinding.buttonOrder.isEnabled = false
                }
            }

            else -> {}
        }
    }


    private fun updateUi(resource: Resource<MainResponse<BalanceData>>?) {
        resource?.let {
            when (it.state) {
                ResourceState.LOADING -> {
                    startBalanceLoading()
                }

                ResourceState.ERROR -> {

                }

                ResourceState.SUCCESS -> {
                    viewBinding.refresh.isRefreshing = false

                    viewLifecycleOwner.lifecycleScope.launch {
                        viewBinding.totalBalanceTxt.text =
                            PhoneNumberUtil.formatMoneyNumberPlate(it.data?.data?.total.toString())
                        stopBalanceLoading()

                    }
                }
            }
        }
    }

    private fun startBalanceLoading() {
        with(viewBinding) {
            totalBalanceTxt.visibility = View.GONE
            totalBalanceLoading.visibility = View.VISIBLE
        }
    }

    private fun stopBalanceLoading() {
        with(viewBinding) {
            totalBalanceTxt.visibility = View.VISIBLE
            totalBalanceLoading.visibility = View.GONE
        }
    }


    private fun exitApp() {
        DialogUtils.showExitDialog(requireContext()).dismiss()
        socketViewModel.setExit(true)
        requireActivity().finishAffinity()

    }

    override fun onResume() {
        super.onResume()


//        socketRepository.socketLive.observe(this){
//            Log.d("tekshirish", "onResume: Live $it")
//        }
        val color = if (userPreferenceManager.getToggleState()) Color.GREEN else Color.RED
        viewBinding.socketIsConnected.backgroundTintList = ColorStateList.valueOf(color)

        // Don't forget to register your receiver
        val filter = IntentFilter("SOCKET_STATUS")
        requireActivity().registerReceiver(socketStatusReceiver, filter)

    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(socketStatusReceiver)
    }

    private fun updateSocket(connected: Boolean) {
        if (!isViewCreated) return
        viewLifecycleOwner.lifecycleScope.launch {
            val color = if (connected) Color.GREEN else Color.RED
            viewBinding.socketIsConnected.backgroundTintList =
                ColorStateList.valueOf(color)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun receiveItemFromSocket(size: Int) {
        val soundPlayer = SoundPlayer(SoundPlayer.SoundType.LowSound, requireContext())
        CoroutineScope(Dispatchers.Main).launch {
            // Execute addItemToRecyclerView in parallel with playSound
            val addItemJob = async(Dispatchers.IO) {
                activity?.runOnUiThread {
                    viewBinding.orderCountTextView.text =
                        getString(R.string.buyurtmalar) + " ($size)"

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
            orderViewModel.clearNewOrder()
        }

    }

    private fun setupObservers() {

//        socketViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
////            updateSocket(isConnected)
//        }

        driverViewModel.completeOrderLostNetwork.observe(viewLifecycleOwner) {
            completeOrderUi(it)
        }
        dashboardViewModel.balanceResponse.observe(viewLifecycleOwner) { resource ->
            updateUi(resource)
        }

        dashboardViewModel.settingsResponse.observe(viewLifecycleOwner) { resources ->
            updateData(resources)
        }

        dashboardViewModel.driverDataResponse.observe(viewLifecycleOwner) { resources ->
            driverUpdate(resources)
        }

        orderViewModel.orderResponse.observe(viewLifecycleOwner) {
            it.data?.data?.size?.let { size ->
                receiveItemFromSocket(size)
            }
        }

        orderViewModel.isNewOrder.observe(viewLifecycleOwner) {
            isNewOrder = it
        }

        // Add any additional observers you have...
    }

    private fun setupView() {
        val appVersion = requireActivity().packageManager.getPackageInfo(
            requireActivity().packageName,
            0
        ).versionName
        viewBinding.tvVersionName.text = "v$appVersion"
        if (userPreferenceManager.getLastRaceId() == -1){
            viewBinding.buttonOrder.isEnabled = userPreferenceManager.getToggleState()
        }


        val widthOfNav = (ScreenUtils.width) * 0.8
        viewBinding.navigationView.layoutParams.width = widthOfNav.toInt()
        viewBinding.navigationView.requestLayout()

        viewBinding.openMenuBtn.setOnClickListener {
            viewBinding.drawerLayout.open()
        }

        viewBinding.fillBalance.setOnClickListener {
            navController.navigate(R.id.transferDashboardFragment)
        }

        viewBinding.buttonTarif.setOnClickListener {
            navController.navigate(R.id.tarifFragment)
        }

        viewBinding.buttonHistory.setOnClickListener {
            navController.navigate(R.id.historyFragment)
        }

        viewBinding.buttonXizmatlar.setOnClickListener {
            navController.navigate(R.id.serviceFragment)
        }

        viewBinding.buttonExit.setOnClickListener {

            if (userPreferenceManager.getToggleState()) {
                DialogUtils.showIsSocketConnect(requireContext())
            } else {
                exitApp()
            }

//            DialogUtils.showExitDialog(
//                context = requireContext(),
//                onSuccessAction = { exitApp() },
//                onNegativeAction = {}
//            ).show()
        }

        viewBinding.callBtn.setOnClickListener {
            ButtonUtils.callToDispatcher(requireContext())
        }

        viewBinding.buttonOrder.setOnClickListener {
            if (LocationPermissionUtils.isBasicPermissionGranted(requireContext())
                && LocationPermissionUtils.isLocationEnabled(requireContext())
            ) {
                startOrder()
            } else {
                permissionActivityResultLauncher.launch(
                    PermissionCheckActivity.getOpenIntent(
                        context = requireContext(),
                        isPromptMode = false
                    )
                )
            }
        }


        viewBinding.isReadyForWork.setOnToggledListener { _, isOn ->

            userPreferenceManager.saveToggleState(isOn)
            val intent = Intent(requireActivity(), SocketService::class.java).apply {
                putExtra("TOKEN", userPreferenceManager.getToken())
                putExtra("IS_READY_FOR_WORK", isOn)
            }
            setToggleButtonUi(isOn)
            if (userPreferenceManager.getLastRaceId() == -1){

                viewBinding.buttonOrder.isEnabled = isOn
            }

            if (isOn) {
                soundManager.playSoundBasedOnFirstOnline()
                Intent(requireActivity(), SocketService::class.java).also { intent ->
                    intent.putExtra("TOKEN", userPreferenceManager.getToken())
                    intent.putExtra("IS_READY_FOR_WORK", true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireContext().startForegroundService(intent)
                    } else {
                        requireContext().startService(intent)
                    }
                }
            } else {
                soundManager.playSoundYouAreOffline()
                Intent(requireActivity(), SocketService::class.java).also { intent ->
                    intent.putExtra("IS_READY_FOR_WORK", false)
                    requireContext().startService(intent)
                }
            }
        }

    }

    private fun setToggleButtonUi(on: Boolean) {
        if (on) {
            viewBinding.isReadyForWork.isOn = true
            viewBinding.isReadyForWork.colorBorder = requireActivity().getColor(R.color.blue)
            viewBinding.isReadyForWork.colorOn = requireActivity().getColor(R.color.blue)

        } else {
            viewBinding.isReadyForWork.isOn = false
            viewBinding.isReadyForWork.colorBorder = requireActivity().getColor(R.color.grey)
            viewBinding.isReadyForWork.colorOn = requireActivity().getColor(R.color.grey)
        }
    }


}