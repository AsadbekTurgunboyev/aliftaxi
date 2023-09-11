package com.example.taxi.utils

import android.telephony.PhoneNumberUtils

class PhoneNumberUtil {
    companion object {
        fun formatPhoneNumber(phoneNumber: String?, countryCode: String?): String? {
            if (phoneNumber.isNullOrEmpty() || countryCode.isNullOrEmpty()) {
                return null
            }
            // Format the phone number
            return PhoneNumberUtils.formatNumber(phoneNumber, countryCode)

        }


        fun formatMoneyNumberPlate(input: String): String {
            val regex = "(\\d)(?=(\\d{3})+$)".toRegex()
            val a =  input.replace(regex, "$1 ")
            return "$a UZS"
        }
    }
}