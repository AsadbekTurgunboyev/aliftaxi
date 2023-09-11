package com.example.taxi.ui.home.order

import android.util.Log
import androidx.lifecycle.*
import com.example.taxi.domain.exception.traceErrorException
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.location.LocationRequest
import com.example.taxi.domain.model.order.Address
import com.example.taxi.domain.model.order.OrderAccept
import com.example.taxi.domain.model.order.OrderData
import com.example.taxi.domain.model.order.UserModel
import com.example.taxi.domain.usecase.main.GetMainResponseUseCase
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class OrderViewModel(private val getMainResponseUseCase: GetMainResponseUseCase) : ViewModel() {

    val compositeDisposable = CompositeDisposable()
//    private var _orderResponse = MutableLiveData<Resource<MainResponse<List<OrderData<Address>>>>>()

    private var _orderItems = MutableLiveData<OrderData<Address>>()

    public var isNewOrder = MutableLiveData<Boolean>().apply {
        value = false
    }
    public fun clearNewOrder(){
        isNewOrder.value = false
    }

    private var _acceptedOrder = MutableLiveData<Resource<MainResponse<OrderAccept<UserModel>>>>()
    val acceptOrder: LiveData<Resource<MainResponse<OrderAccept<UserModel>>>> get() = _acceptedOrder

    private var _ordersCount = MutableLiveData<Int>().apply {
        value = 0
    }

    val ordersCount: LiveData<Int> = _ordersCount

    val orderResponse: LiveData<Resource<MainResponse<List<OrderData<Address>>>>>
        get() = _orderResponse
    private var _serverOrderResponse = MutableLiveData<Resource<MainResponse<List<OrderData<Address>>>>>()

    fun addItem(orderItem: OrderData<Address>) {
        Log.d("itemuchun", "addItem: $orderItem")
        _orderItems.postValue(orderItem)
//        _orderItems.value = orderItem
    }
    fun removeItem(orderId: Int) {
        Log.d("itemuchun", "removeItem: $orderId")
        val existingResponse = _serverOrderResponse.value
        if (existingResponse != null && existingResponse.state == ResourceState.SUCCESS) {
            val currentData = existingResponse.data?.data?.toMutableList()
            if (currentData != null) {
                val orderToRemove = currentData.firstOrNull { it.id == orderId }
                if (orderToRemove != null) {
                    currentData.remove(orderToRemove)
                    val newData = existingResponse.data
                    newData.data = currentData
                    _orderResponse.postValue(Resource(ResourceState.SUCCESS, newData))
                }
            }
        }
    }

    private var _orderResponse = MediatorLiveData<Resource<MainResponse<List<OrderData<Address>>>>>().apply {
        addSource(_orderItems) { newItem ->
            val existingResponse = _serverOrderResponse.value
            if (existingResponse != null && existingResponse.state == ResourceState.SUCCESS) {
                val currentData = existingResponse.data?.data?.toMutableList()
                if (currentData != null) {
                    isNewOrder.value = true
                    currentData.add(0, newItem) // add newItem at position 0
                    val newData = existingResponse.data
                    newData.data = currentData
                    this.value = Resource(ResourceState.SUCCESS, newData)
                }
            }
        }
        addSource(_serverOrderResponse) { newResponse ->
            Log.d("orderuchun", "addsource: ${newResponse.data}")

            this.value = newResponse
            isNewOrder.value = false
        }
    }


    fun sendLocation(request: LocationRequest) {
        compositeDisposable.add(
            getMainResponseUseCase.sendLocation(request = request)

                .subscribeOn(Schedulers.io())
                .doOnSubscribe {}
                .doOnTerminate {}
                .subscribe({

                }, { error ->

                    val errorMessage = if (error is HttpException) {
                        try {
                            val errorBody = error.response()?.errorBody()?.string()
                            val mainResponse = Gson().fromJson(errorBody, MainResponse::class.java)
                            mainResponse.message
                        } catch (e: Exception) {
                            "An error occurred"
                        }
                    } else {
                        "An error occurred"
                    }

                })
        )
    }

    fun acceptOrder(id: Int) {
        _acceptedOrder.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(
            getMainResponseUseCase.acceptOrder(id = id)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {}
                .doOnTerminate {}
                .subscribe({ response ->
                    Log.d("orderuchun", "accept: ${response.data}")

                    _acceptedOrder.postValue(Resource(ResourceState.SUCCESS, response))

                    }, { error ->

                    Log.d("xatolikni", "acceptOrder: $error) ${error.message}")
                    val errorMessage = if (error is HttpException) {
                        try {
                            val errorBody = error.response()?.errorBody()?.string()
                            val mainResponse = Gson().fromJson(errorBody, MainResponse::class.java)
                            mainResponse.message
                        } catch (e: Exception) {
                            "An error occurred"
                        }
                    } else {
                        "An error occurred"
                    }

                    _acceptedOrder.postValue(
                        Resource(
                            ResourceState.ERROR,
                            message = errorMessage
                        )
                    )

                })
        )
    }


    fun getOrders() {
        _serverOrderResponse.postValue(Resource(ResourceState.LOADING))
        compositeDisposable.add(
            getMainResponseUseCase.getOrders()
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
                            Log.d("orderviewmodel", "getOrders: $response")
                            _ordersCount.postValue(response.data.size)
                            _serverOrderResponse.postValue(Resource(ResourceState.SUCCESS, response))
                        }

                    },
                    { error ->
                        _serverOrderResponse.postValue(
                            Resource(
                                ResourceState.ERROR,
                                message = traceErrorException(error).getErrorMessage()
                            )
                        )
                    }
                )
        )
    }

    fun clearAcceptOrderData(){
        _acceptedOrder = MutableLiveData()
    }

    fun clearViewModelData() {
        _acceptedOrder = MutableLiveData()
        _orderResponse = MediatorLiveData()
        _ordersCount = MutableLiveData()
        _ordersCount.value = 0
        _orderItems = MutableLiveData()
    }

    public override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}