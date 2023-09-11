package com.example.taxi.data.repository

import com.example.taxi.data.source.ApiService
import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.about.ResponseAbout
import com.example.taxi.domain.model.balance.BalanceData
import com.example.taxi.domain.model.history.*
import com.example.taxi.domain.model.location.LocationRequest
import com.example.taxi.domain.model.order.*
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.domain.model.settings.SettingsData
import com.example.taxi.domain.model.tarif.ModeRequest
import com.example.taxi.domain.model.tarif.ModeResponse
import com.example.taxi.domain.model.transfer.DriverNameByIdResponse
import com.example.taxi.domain.model.transfer.HistoryMeta
import com.example.taxi.domain.model.transfer.ResponseTransferHistory
import com.example.taxi.domain.model.transfer.TransferRequest
import com.example.taxi.domain.repository.MainRepository
import io.reactivex.Observable

class MainRepositoryImpl(private val apiService: ApiService) : MainRepository {

    override fun getModes(): Observable<ModeResponse> {
        return apiService.getModes()
    }

    override fun getService(): Observable<ModeResponse> {
        return apiService.getService()
    }

    override fun getDriverAllData(): Observable<MainResponse<SelfieAllData<IsCompletedModel,StatusModel>>> {
        return apiService.getDriverAllData()
    }

    override fun setModes(request: ModeRequest): Observable<ModeResponse> {
        return apiService.setModes(request = request)
    }

    override fun setService(request: ModeRequest): Observable<ModeResponse> {
        return apiService.setService(request = request)
    }

    override fun getBalance(): Observable<MainResponse<BalanceData>> {
        return apiService.getBalance()
    }

    override fun getSettings(): Observable<MainResponse<List<SettingsData>>> {
        return apiService.getSettings()
    }

    override fun getOrders(): Observable<MainResponse<List<OrderData<Address>>>> {
        return apiService.getOrders()
    }

    override fun orderAccept(id: Int): Observable<MainResponse<OrderAccept<UserModel>>> {
        return apiService.orderAccept(id = id)
    }

    override fun arrivedOrder(): Observable<MainResponse<Any>> {
        return apiService.arrivedOrder()
    }

    override fun startOrder(): Observable<MainResponse<Any>> {
        return apiService.startOrder()
    }

    override fun competeOrder(request: OrderCompleteRequest): Observable<MainResponse<Any>> {
        return apiService.completeOrder(request = request)
    }

    override fun sendLocation(request: LocationRequest): Observable<MainResponse<Any>> {
        return apiService.sendLocation(request = request)
    }

    override fun getDriverById(driver_id: Int): Observable<MainResponse<DriverNameByIdResponse>> {
        return apiService.getDriverById(driver_id = driver_id)
    }

    override fun getHistory(page: Int,from: String?, to: String?, type: Int?, status: Int?): Observable<HistoryDataResponse<Meta>> {
        return apiService.getHistory(page = page,from = from, to = to, type = type, status = status)
    }

    override fun transferMoney(request: TransferRequest): Observable<MainResponse<Any>> {
        return apiService.transferMoney(request = request)
    }

    override fun getTransferHistory(page: Int, from: String?, to: String?, type: Int?): Observable<ResponseTransferHistory<HistoryMeta>> {
        return apiService.getTransferHistory(page = page, from = from, to = to, type = type)
    }

    override fun getAbout(): Observable<MainResponse<ResponseAbout>> {
        return apiService.getAbout()
    }

    override fun getFAQ(): Observable<MainResponse<ResponseAbout>> {
        return apiService.getFAQ()
    }

    override fun getCurrentOrder(): Observable<MainResponse<Any>> {
        return apiService.getOrderCurrent()
    }


}