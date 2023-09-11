package com.example.taxi.ui.home.driver

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taxi.custom.SingleLiveEvent
import com.example.taxi.domain.exception.traceErrorException
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.order.OrderCompleteRequest
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.domain.usecase.main.GetMainResponseUseCase
import com.example.taxi.ui.home.DriveAction
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.koin.java.KoinJavaComponent.inject

class DriverViewModel(private val mainResponseUseCase: GetMainResponseUseCase) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val orderStartCompleteLiveData = MutableLiveData<Int>().apply {
        value = DriveAction.ACCEPT
    }



    private var _startOrder = MutableLiveData<Resource<MainResponse<Any>>>()
    val startOrder: LiveData<Resource<MainResponse<Any>>> = _startOrder

    private var _arriveOrder = MutableLiveData<Resource<MainResponse<Any>>>()
    val arriveOrder: LiveData<Resource<MainResponse<Any>>> get() = _arriveOrder

    private var _completeOrder = MutableLiveData<Resource<MainResponse<Any>>>()
    val completeOrder: LiveData<Resource<MainResponse<Any>>> get() = _completeOrder


    fun arriveOrder(){

        _arriveOrder.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(mainResponseUseCase.arrivedOrder().subscribeOn(Schedulers.io())
            .doOnSubscribe {
                // Perform any setup tasks before the subscription starts
            }.doOnTerminate {}.subscribe({ response ->
                _arriveOrder.postValue(Resource(ResourceState.SUCCESS, response))
            }, { error ->
                _arriveOrder.postValue(
                    Resource(
                        ResourceState.ERROR,
                        message = traceErrorException(error).getErrorMessage()
                    )
                )

            })
        )
    }

    fun startOrder() {
        _startOrder.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(mainResponseUseCase.startOrder().subscribeOn(Schedulers.io())
            .doOnSubscribe {
                // Perform any setup tasks before the subscription starts
            }.doOnTerminate {}.subscribe({ response ->
                _startOrder.postValue(Resource(ResourceState.SUCCESS, response))
            }, { error ->
                _startOrder.postValue(
                    Resource(
                        ResourceState.ERROR,
                        message = traceErrorException(error).getErrorMessage()
                    )
                )

            })
        )
    }

    fun completeOrder(orderCompleteRequest: OrderCompleteRequest) {
        _completeOrder.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(mainResponseUseCase.completeOrder(orderCompleteRequest)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                // Perform any setup tasks before the subscription starts
            }.doOnTerminate {}.subscribe({ response ->
                _completeOrder.postValue(Resource(ResourceState.SUCCESS, response))

            }, { error ->
                _completeOrder.postValue(
                    Resource(
                        ResourceState.ERROR,
                        message = traceErrorException(error).getErrorMessage()
                    )
                )

            })
        )
    }

    fun acceptedOrder(){
        orderStartCompleteLiveData.postValue( DriveAction.ACCEPT)
    }

    fun arrivedOrder(){
        orderStartCompleteLiveData.postValue( DriveAction.ARRIVED)
    }

    fun startedOrder(){
        orderStartCompleteLiveData.postValue( DriveAction.STARTED)
    }

    fun completedOrder(){
        orderStartCompleteLiveData.postValue( DriveAction.COMPLETED)
    }


    fun clearAllData(){
        _arriveOrder = MutableLiveData()
        _startOrder = MutableLiveData()
        _completeOrder = MutableLiveData()
        orderStartCompleteLiveData.value = DriveAction.ACCEPT
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}