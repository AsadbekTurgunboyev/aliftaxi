package com.example.taxi.domain.usecase.main

import com.example.taxi.domain.model.order.OrderCompleteRequest
import com.example.taxi.domain.model.tarif.ModeRequest
import com.example.taxi.domain.model.transfer.TransferRequest
import com.example.taxi.domain.repository.MainRepository

class GetMainResponseUseCase(private val mainRepository: MainRepository) {

    fun getModes() = mainRepository.getModes()

    fun getDriverData() = mainRepository.getDriverAllData()

    fun setModes(request: ModeRequest) = mainRepository.setModes(request = request)

    fun getService() = mainRepository.getService()

    fun setService(request: ModeRequest) = mainRepository.setService(request = request)

    fun getBalance() = mainRepository.getBalance()

    fun getSettings() = mainRepository.getSettings()

    fun getOrders() = mainRepository.getOrders()

    fun acceptOrder(id: Int) = mainRepository.orderAccept(id = id)

    fun arrivedOrder() = mainRepository.arrivedOrder()

    fun startOrder() = mainRepository.startOrder()

    fun completeOrder(request: OrderCompleteRequest) =
        mainRepository.competeOrder(request = request)

    fun sendLocation(request: com.example.taxi.domain.model.location.LocationRequest) =
        mainRepository.sendLocation(
            request = request
        )

    fun getHistory(page: Int,from: String? = null, to: String? = null, type: Int? = null, status: Int? = null) = mainRepository.getHistory(page = page,from = from, to = to,type = type, status = status)

    fun getDriverNameById(driver_id: Int) = mainRepository.getDriverById(driver_id = driver_id)

    fun transferMoney(request: TransferRequest) = mainRepository.transferMoney(request = request)

    fun getTransferHistory(page: Int, from: String? = null, to: String? = null, type: Int? = null) =
        mainRepository.getTransferHistory(page = page, from, to,type)

    fun getAbout() = mainRepository.getAbout()
    fun getFAQ() = mainRepository.getFAQ()

     fun getOrderCurrent() = mainRepository.getCurrentOrder()
}