package com.example.taxi.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxi.domain.model.DashboardData
import com.example.taxi.domain.preference.UserPreferenceManager
import kotlin.math.roundToInt

object TaxiCalculator {


    @RequiresApi(Build.VERSION_CODES.N)
    fun getCurrentDriveCost(
        dashboardData: DashboardData,
        preferenceManager: UserPreferenceManager,
        secondsElapsed: Long
    ) :String{
        val waitTime = ((preferenceManager.getFinishedTimeAcceptOrder() - preferenceManager.getTransitionTime()) / 1000).toInt() + secondsElapsed
        Log.d("tekshirishuchun", "getCurrentDriveCost: wait time $waitTime")
        Log.d("tekshirishuchun", "getCurrentDriveCost: oraliq ${(preferenceManager.getFinishedTimeAcceptOrder() - preferenceManager.getTransitionTime()) / 1000}")
        val currentDriveCost: Int = if (dashboardData.getDistanceText() >= dashboardData.convertToKm(preferenceManager.getMinDistance())) {
            val a = dashboardData.getDistanceText() - dashboardData.convertToKm(preferenceManager.getMinDistance())
            val addPrice = a * preferenceManager.getCostPerKm()
//            plus((waitTime / 60.0) * preferenceManager.getCostWaitTimePerMinute()
            preferenceManager.getStartCost().plus(addPrice).roundToInt()
        } else {
            preferenceManager.getStartCost()
        }
        return PhoneNumberUtil.formatMoneyNumberPlate(roundToNearestMultiple(currentDriveCost).toString())
    }

     fun roundToNearestMultiple(number: Int): Int {
       val multiple = 500
        val remainder = number % multiple
        return if (remainder < multiple / 2) {
            number - remainder
        } else {
            number + (multiple - remainder)
        }
    }

}

data class TaxiData(
    var costPerKm: Int? = 0,
    var costWaitTimePerMinute: Int? = 0,
    var costOutCenter: Int? = 0,
    var startCost: Int? = 0,
    var minimalDistance: Int? = 0,
    var minWaitTime: Int? = 0,
    var currentDriveDistance: Int? = 0
)