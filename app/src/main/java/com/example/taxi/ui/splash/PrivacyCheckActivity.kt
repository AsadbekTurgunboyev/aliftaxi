package com.example.taxi.ui.splash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.taxi.R
import com.example.taxi.domain.model.settings.SettingsData
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.HomeActivity
import com.example.taxi.ui.login.LoginActivity
import com.example.taxi.ui.permission.PermissionCheckActivity
import com.example.taxi.utils.LocationPermissionUtils
import com.example.taxi.utils.ViewUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class PrivacyCheckActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModel()
    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (LocationPermissionUtils.isLocationEnabled(this)
                && LocationPermissionUtils.isBasicPermissionGranted(this)
                && Settings.canDrawOverlays(this)
            ) {
                nextActivity()
            }
        }
    private val userPreferenceManager: UserPreferenceManager by inject()

   

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewUtils.setTheme(userPreferenceManager.getTheme())
        checkOverlayPermission()
    }

    override fun attachBaseContext(newBase: Context) {
        val newContext = ViewUtils.setLanguage(newBase, userPreferenceManager.getLanguage())
        super.attachBaseContext(newContext)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (LocationPermissionUtils.isBasicPermissionGranted(this)
            && LocationPermissionUtils.isLocationEnabled(this)
            && Settings.canDrawOverlays(this)
        ) {
          nextActivity()
        } else {
            permissionActivityResultLauncher.launch(
                PermissionCheckActivity.getOpenIntent(
                    context = this,
                    isPromptMode = false
                )
            )
        }

    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (LocationPermissionUtils.isBasicPermissionGranted(this)
            && LocationPermissionUtils.isLocationEnabled(this)
            && Settings.canDrawOverlays(this)
        ) {
            nextActivity()
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.neеded_permission))
            .setMessage(getString(R.string.draw_permission))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
            .show()
    }
    private fun nextActivity(){
        if (userPreferenceManager.getRegisterComplete()){
            continueHome()
        }else{
            continueLogin()
        }
    }
    private fun continueLogin() {
        LoginActivity.open(this)
        finish()
    }

    private fun continueHome() {
        HomeActivity.open(this)
        finish()
    }
}