package com.example.taxi.domain.repository

import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.about.ResponseAbout
import com.example.taxi.domain.model.balance.BalanceData
import com.example.taxi.domain.model.checkAccess.AccessModel
import com.example.taxi.domain.model.history.HistoryDataResponse
import com.example.taxi.domain.model.history.Meta
import com.example.taxi.domain.model.location.LocationRequest
import com.example.taxi.domain.model.order.Address
import com.example.taxi.domain.model.order.OrderAccept
import com.example.taxi.domain.model.order.OrderCompleteRequest
import com.example.taxi.domain.model.order.OrderData
import com.example.taxi.domain.model.order.UserModel
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.domain.model.settings.SettingsData
import com.example.taxi.domain.model.tarif.ModeRequest
import com.example.taxi.domain.model.tarif.ModeResponse
import com.example.taxi.domain.model.transfer.DriverNameByIdResponse
import com.example.taxi.domain.model.transfer.HistoryMeta
import com.example.taxi.domain.model.transfer.ResponseTransferHistory
import com.example.taxi.domain.model.transfer.TransferRequest
import io.reactivex.Observable

interface MainRepository {

    fun getModes(): Observable<ModeResponse>

    fun getService(): Observable<ModeResponse>

    fun getDriverAllData(): Observable<MainResponse<SelfieAllData<IsCompletedModel, StatusModel>>>

    fun setModes(request: ModeRequest): Observable<ModeResponse>

    fun setService(request: ModeRequest): Observable<ModeResponse>

    fun getBalance(): Observable<MainResponse<BalanceData>>

    fun getSettings(): Observable<MainResponse<List<SettingsData>>>

    fun getOrders(): Observable<MainResponse<List<OrderData<Address>>>>

    fun orderAccept(id: Int): Observable<MainResponse<OrderAccept<UserModel>>>

    fun orderWithTaximeter():Observable<MainResponse<OrderAccept<UserModel>>>

    fun arrivedOrder(): Observable<MainResponse<Any>>

    fun startOrder(): Observable<MainResponse<Any>>

    fun competeOrder(request: OrderCompleteRequest): Observable<MainResponse<Any>>

    fun completeOrderNoNetwork(request: OrderCompleteRequest): Observable<MainResponse<Any>>

    fun sendLocation(request: LocationRequest): Observable<MainResponse<Any>>

    fun checkAccess(request: AccessModel): Observable<MainResponse<Any>>

    fun getDriverById(driver_id: Int): Observable<MainResponse<DriverNameByIdResponse>>

    fun getHistory(
        page: Int,
        from: String?,
        to: String?,
        type: Int?,
        status: Int?
    ): Observable<HistoryDataResponse<Meta>>

    fun transferMoney(request: TransferRequest): Observable<MainResponse<Any>>

    fun getTransferHistory(
        page: Int,
        from: String?,
        to: String?,
        type: Int?
    ): Observable<ResponseTransferHistory<HistoryMeta>>

    fun getAbout(): Observable<MainResponse<ResponseAbout>>
    fun getFAQ(): Observable<MainResponse<ResponseAbout>>
    fun getCurrentOrder(): Observable<MainResponse<Any>>
}
