package com.example.taxi.network

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class MyPhoneStateListener(private val context: Context, private val onSpeedChanged: (Boolean) -> Unit) : PhoneStateListener() {

    private var wasInCall = false

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)

        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                // Qo'ng'iroq davomida
                wasInCall = true
                onSpeedChanged(false) // Qo'ng'iroq davomida internet tezligi kamayadi deb qabul qilamiz
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                // Qo'ng'iroq tugadi
                if (wasInCall) {
                    wasInCall = false
                    onSpeedChanged(true) // Internet tezligini qayta tiklash uchun tekshirish
                }
            }
        }
    }
}