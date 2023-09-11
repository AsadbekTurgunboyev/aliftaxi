package com.example.taxi.ui.home.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxi.domain.exception.traceErrorException
import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.balance.BalanceData
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.domain.model.settings.DataNames
import com.example.taxi.domain.model.settings.SettingsData
import com.example.taxi.domain.model.settings.getItemValueByName
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.domain.usecase.main.GetMainResponseUseCase
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getMainResponseUseCase: GetMainResponseUseCase,
    private val userPreferenceManager: UserPreferenceManager
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val _balanceResponse = MutableLiveData<Resource<MainResponse<BalanceData>>>()

    private val _settingsResponse = MutableLiveData<Resource<MainResponse<List<SettingsData>>>>()

    val balanceResponse: LiveData<Resource<MainResponse<BalanceData>>>
        get() = _balanceResponse

    private val  _driverDataResponse = MutableLiveData<Resource<MainResponse<SelfieAllData<IsCompletedModel, StatusModel>>>>()
    val driverDataResponse get() = _driverDataResponse

    val settingsResponse: LiveData<Resource<MainResponse<List<SettingsData>>>>
        get() = _settingsResponse

    fun getSettings() {
        _settingsResponse.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(
            getMainResponseUseCase.getSettings()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    // Perform any setup tasks before the subscription starts
                }
                .doOnTerminate {
                    // Perform any cleanup tasks after the subscription ends
                }
                .subscribe(
                    { response ->
                        viewModelScope.launch {
                            _settingsResponse.postValue(Resource(ResourceState.SUCCESS, response))
                            setPhoneNumber( response?.getItemValueByName(DataNames.PHONE_NUMBER))
                            setAllSettings(response)
                        }


                    },
                    { error ->
                        _settingsResponse.postValue(
                            Resource(
                                ResourceState.ERROR,
                                message = traceErrorException(error).getErrorMessage()
                            )
                        )
                    }
                )
        )
    }

    private fun setAllSettings(response: MainResponse<List<SettingsData>>?) {
        val centerLat = response?.getItemValueByName(DataNames.CENTER_LATITUDE)
        val centerLong = response?.getItemValueByName(DataNames.CENTER_LONGITUDE)
        val centerRadius = response?.getItemValueByName(DataNames.CENTER_RADIUS)
        userPreferenceManager.setSettings(centerLat,centerLong,centerRadius)
    }

    private fun setPhoneNumber(itemValueByName: String?) {
        itemValueByName?.let {
            userPreferenceManager.saveCallPhoneNumber(it)
        }
    }

    fun getDriverData(){
        _driverDataResponse.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(
            getMainResponseUseCase.getDriverData()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    // Perform any setup tasks before the subscription starts
                }
                .doOnTerminate {
                    // Perform any cleanup tasks after the subscription ends
                }
                .subscribe(
                    { response ->
                        _driverDataResponse.postValue(Resource(ResourceState.SUCCESS, response))
                    },
                    { error ->
                        _driverDataResponse.postValue(
                            Resource(
                                ResourceState.ERROR,
                                message = traceErrorException(error).code.toString()
                            )
                        )
                    }
                )
        )
    }

    fun getBalance() {
        _balanceResponse.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(
            getMainResponseUseCase.getBalance()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    // Perform any setup tasks before the subscription starts
                }
                .doOnTerminate {
                    // Perform any cleanup tasks after the subscription ends
                }
                .subscribe(
                    { response ->
                        _balanceResponse.postValue(Resource(ResourceState.SUCCESS, response))
                    },
                    { error ->
                        _balanceResponse.postValue(
                            Resource(
                                ResourceState.ERROR,
                                message = traceErrorException(error).getErrorMessage()
                            )
                        )
                    }
                )
        )
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}