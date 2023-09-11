package com.example.taxi.ui.splash

import androidx.lifecycle.ViewModel
import com.example.taxi.domain.privacyPolicy.PrivacyPolicyService

class SplashViewModel(private val privacyPolicyService: PrivacyPolicyService) : ViewModel() {

    fun isPrivacyPolicyAccepted(): Boolean {
        return privacyPolicyService.isPolicyAccepted()
    }

    fun acceptPrivacyPolicy() {
        privacyPolicyService.onAcceptPolicy()
    }
}