package com.example.taxi.ui.home.driver.driveReport

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.taxi.R
import com.example.taxi.databinding.DialogFinishOrderBinding
import com.example.taxi.domain.location.LocationPoint
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.order.OrderCompleteRequest
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.driver.DriverViewModel
import com.example.taxi.utils.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kotlin.math.*

class DriveFinishDialog(val raceId: Long, val viewModel: DriveReportViewModel) :
    DialogFragment() {

    private val driverViewModel: DriverViewModel by sharedViewModel()
    private val preferenceManager: UserPreferenceManager by inject()

    var farCenterDistance = 0.0

    lateinit var viewBinding: DialogFinishOrderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = DialogFinishOrderBinding.inflate(layoutInflater, container, false)
        return viewBinding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keycode, _ -> keycode == KeyEvent.KEYCODE_BACK }
//
//        dialog.window?.apply {
//            val attributes = attributes
//            attributes.width = WindowManager.LayoutParams.MATCH_PARENT
//            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
//            attributes.gravity = Gravity.BOTTOM
//            setAttributes(attributes)
//        }

        return dialog
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getDrivePath().observe(viewLifecycleOwner) {
            setData(it)
        }
        viewModel.getDriveAnalyticsLiveData().observe(viewLifecycleOwner) {
            renderAnalyticsReportData(it)
        }

//        driverViewModel.completeOrder.observe(viewLifecycleOwner) { resource ->
//            completeUi(resource)
//
//        }

    }

    private fun totalPathDistance(points: List<LocationPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += haversineDistance(points[i], points[i + 1])
        }

        return totalDistance
    }

    private fun setData(locationPoints: List<LocationPoint>?) {

        val centralPoint = preferenceManager.getCentralLocationPoint()
        val minDistanceInMeters = preferenceManager.getCenterRadius()?.toInt() ?: 0

        if (centralPoint != null) {
            val farPoints =
                locationPoints?.let { pointsWithinDistance(it, centralPoint, minDistanceInMeters) }
            farCenterDistance = farPoints?.let { totalPathDistance(it) }!!
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme // Optional: Customize the theme of the bottom sheet dialog
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun renderAnalyticsReportData(driveAnalyticsData: DriveAnalyticsData) {

        val costPerKm: Int = preferenceManager.getCostPerKm() // Suppose it returns Int
        val totalDistance: Double =
            driveAnalyticsData.getTotalDistanceAsDouble() // Suppose it returns Double
        val minDistance = driveAnalyticsData.getMinDistanceAsKm(preferenceManager.getMinDistance())

        Log.d(TAG, "renderAnalyticsReportData: totalDistance $totalDistance")
        Log.d(TAG, "renderAnalyticsReportData: har bir km uchun narx $costPerKm")
        Log.d(TAG, "renderAnalyticsReportData: minimal masofa $minDistance")
        val isDistance = totalDistance - (farCenterDistance / 1000.0)
        var totalInCenterDistance = 0.0
        if (isDistance > minDistance) {
            totalInCenterDistance = isDistance - (minDistance)
        }
        Log.d(TAG, "renderAnalyticsReportData: out masofa $farCenterDistance")
        val moneyForInDistance = (totalInCenterDistance) * costPerKm
        val moneyForFarDistance =
            (farCenterDistance / 1000.toDouble()) * preferenceManager.getCostOutCenter()
        Log.d(
            TAG,
            "renderAnalyticsReportData: tashqaridagi har bir km uchun narx ${preferenceManager.getCostOutCenter()}"
        )
        Log.d(TAG, "renderAnalyticsReportData: in Cost $moneyForInDistance")
        Log.d(TAG, "renderAnalyticsReportData: out Cost $moneyForFarDistance")

        val moneyTotalDistance = totalDistance * costPerKm
//        val moneyWithKm: Int = TaxiCalculator.roundToNearestMultiple((moneyForInDistance + moneyForFarDistance).roundToInt())// This will also work
        val moneyWithKm: Int =
            TaxiCalculator.roundToNearestMultiple(moneyTotalDistance.roundToInt())// This will also work

        Log.d(
            TAG,
            "renderAnalyticsReportData: mijozgacha kutish vaqti ${ConversionUtil.getAllWaitTime()}"
        )
        Log.d(
            TAG,
            "renderAnalyticsReportData: ishda kutish vaqti ${driveAnalyticsData.getPauseTimeWithSecond()}"
        )
        val waitTime = driveAnalyticsData.getPauseTimeWithSecond() + ConversionUtil.getAllWaitTime()
        val moneyWithTime =
            TaxiCalculator.roundToNearestMultiple(((waitTime / 60.toDouble()) * preferenceManager.getCostWaitTimePerMinute()).toInt())


        preferenceManager.clearPassengerPhone()

        val allPrice = moneyWithTime + moneyWithKm + preferenceManager.getStartCost()



        viewBinding.waitTimeTxt.text = ConversionUtil.convertSecondsToMinutes(waitTime.toInt())
        viewBinding.km.text = driveAnalyticsData.getTotalDistanceAsString()
        viewBinding.startcost.text =
            PhoneNumberUtil.formatMoneyNumberPlate(preferenceManager.getStartCost().toString())
        viewBinding.priceTripNoWait.text = PhoneNumberUtil.formatMoneyNumberPlate(
            moneyWithKm.toString()
        )

        val order =
            OrderCompleteRequest(
                cost = allPrice,
                distance = driveAnalyticsData.getDistance(),
                wait_time = waitTime.toInt(),
                wait_cost = moneyWithTime
            )




        viewBinding.priceWait.text =
            PhoneNumberUtil.formatMoneyNumberPlate(moneyWithTime.toString())

        viewBinding.priceAll.text = PhoneNumberUtil.formatMoneyNumberPlate(allPrice.toString())

        viewBinding.finishButtonDialog.setOnClickListener {
            driverViewModel.completeOrder(
                order
            )
        }

        driverViewModel.completeOrder.observe(viewLifecycleOwner) {
            when (it.state) {
                ResourceState.ERROR -> {
                    preferenceManager.saveLastRace(order,1)

                    viewModel.deleteDrive(driveId = raceId)
                    preferenceManager.timeClear()
                    viewBinding.finishButtonDialog.stopAnimation()
                    navigateToDashboardFragment()

                }

                ResourceState.SUCCESS -> {
                    preferenceManager.saveLastRace(order,-1)

                    viewBinding.finishButtonDialog.stopAnimation()

                    driverViewModel.completedOrder()
                    viewModel.deleteDrive(driveId = raceId)
                    preferenceManager.timeClear()
                    navigateToDashboardFragment()
                }

                ResourceState.LOADING -> {
                    viewBinding.finishButtonDialog.startAnimation()

                }
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.finishButtonDialog.dispose()
    }
    private fun navigateToDashboardFragment() {
        activity?.let { currentActivity ->
            val intent = currentActivity.intent
            currentActivity.finish()
            currentActivity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            startActivity(intent)
            currentActivity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(Gravity.BOTTOM)
        }
    }

    private fun pointsWithinDistance(
        points: List<LocationPoint>,
        center: LocationPoint,
        minDistance: Int
    ): List<LocationPoint> {
        return points.filter { haversineDistance(it, center) >= minDistance }
    }

    private fun haversineDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val R = 6371e3  // radius of Earth in meters
        val lat1 = point1.latitude * PI / 180  // convert degrees to radians
        val lat2 = point2.latitude * PI / 180
        val deltaLat = (point2.latitude - point1.latitude) * PI / 180
        val deltaLon = (point2.longitude - point1.longitude) * PI / 180

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    companion object {
        val TAG = "finishDialog"
    }
}