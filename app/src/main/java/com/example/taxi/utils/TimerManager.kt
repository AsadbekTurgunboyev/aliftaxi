package com.example.taxi.utils

import android.content.Context
import com.example.taxi.R
import com.example.taxi.domain.preference.UserPreferenceManager
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext

class TimerManager (
    val context: Context,
    var updateCallback: (Int, String) -> Unit
) {

    private var isCountingDown = true
    private var startTime: Long = 0
    private var minWaitTime: Int = 0
    private var moneyTime: Int = 0

    private var pauseTime: Long = 0
    private var transitionTime: Long = 0
    private val userPreferenceManager by lazy { GlobalContext.get().get<UserPreferenceManager>() }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    var isPaused = false
        set(value) {
            field = value
            userPreferenceManager.saveIsPaused(value)
        }

    init {
        loadFromPreferences()
        startTimer()
    }

    private fun loadFromPreferences() {
        with(userPreferenceManager) {
            startTime = getStartedTimeAcceptOrder()
            transitionTime = getTransitionTime()
            pauseTime = getPauseTime()
            isCountingDown = getIsCountingDown()
            minWaitTime = getMinWaitTime()
            isPaused = getIsPaused()
        }
    }

    private fun startTimer() {
        job = scope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = ((currentTime - startTime) / 1000).toInt()

                if (isCountingDown) {
                    handleCountDown(currentTime, elapsedSeconds)
                } else {
                    handleMoneyTime(currentTime)
                }

                delay(1000)
            }
        }
    }

    private suspend fun handleCountDown(currentTime: Long, elapsedSeconds: Int) {
        val remainingTime = minWaitTime - elapsedSeconds
        withContext(Dispatchers.Main) {
            updateCallback(remainingTime, context.getString(R.string.bepul_kutish))
        }

        if (remainingTime <= 0) {
            isCountingDown = false
            userPreferenceManager.saveIsCountingDown(false)
            transitionTime = currentTime
            userPreferenceManager.saveTransitionTime(transitionTime)
        }
    }

    private suspend fun handleMoneyTime(currentTime: Long) {
        val moneyTime = ((currentTime - transitionTime) / 1000).toInt()
        withContext(Dispatchers.Main) {
            updateCallback(moneyTime, context.getString(R.string.wait_money))
        }
    }

    fun saveTransitionTime() {
        if (isCountingDown) {
            val currentTime = System.currentTimeMillis()
            transitionTime = currentTime
            moneyTime = 0
            userPreferenceManager.saveTransitionTime(currentTime)
            userPreferenceManager.saveIsCountingDown(false)
        }else{
            moneyTime = ((transitionTime - startTime) / 1000).toInt()
        }
        job?.cancel()
    }



    companion object Factory {

        const val TAG = "vaqtlar"
    }
    fun stop() {
        // Implement cleanup logic here, such as canceling the coroutine job
        job?.cancel()
    }

}
