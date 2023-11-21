package com.example.taxi.ui.splash

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.taxi.R
import com.example.taxi.domain.model.FORBIDDEN_ERROR_MESSAGE
import com.example.taxi.domain.model.checkAccess.AccessModel
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.ui.home.HomeActivity
import com.example.taxi.ui.login.LoginActivity
import com.example.taxi.ui.permission.PermissionCheckActivity
import com.example.taxi.ui.splash.SplashViewModel
import com.example.taxi.ui.update.UpdateActivity
import com.example.taxi.utils.LocationPermissionUtils
import com.example.taxi.utils.ResourceState
import com.example.taxi.utils.ViewUtils
import com.jaredrummler.android.device.DeviceName
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class PrivacyCheckActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModel()
    private var app_detail = ""
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
        checkAccess()
        checkOverlayPermission()

    }

    private fun checkAccess() {
        val manufacture = Build.MANUFACTURER
        var versionCode: Long = 0
        var versionName: String = "1.0"


        try {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
            versionName = pInfo.versionName

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        val m = DeviceName.getDeviceInfo(this).manufacturer
        val infoName = DeviceName.getDeviceInfo(this).name
        val deviceName = "$m $infoName"       // "Galaxy S8+"

        app_detail = if (m == manufacture) deviceName else manufacture

        val checkAccess = AccessModel(
            app_version = versionCode.toInt(),
            app_version_name = versionName,
            phone_model = app_detail
        )
        viewModel.checkAccess(checkAccess)

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
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
            .show()
    }

    private fun nextActivity() {
        if (userPreferenceManager.getRegisterComplete()) {
            if (userPreferenceManager.getDriverStatus() != UserPreferenceManager.DriverStatus.COMPLETED) {
                continueHome()
            } else {
                viewModel.checkAccess.removeObservers(this)
                viewModel.checkAccess.observe(this) {
                    when (it.state) {
                        ResourceState.ERROR -> {
                            if ((userPreferenceManager.getDriverStatus() == UserPreferenceManager.DriverStatus.COMPLETED) && (it.message == FORBIDDEN_ERROR_MESSAGE)) {
                                continueUpdating()
                            } else {
                                continueHome()
                            }
                        }

                        ResourceState.SUCCESS -> {
                            continueHome()
                        }

                        ResourceState.LOADING -> {
                        }
                    }

                }
            }

        } else {
            continueLogin()
        }
    }

    private fun continueUpdating() {
        UpdateActivity.open(this)
        finish()
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