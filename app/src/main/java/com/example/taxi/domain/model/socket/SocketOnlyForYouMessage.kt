package com.example.taxi.domain.model.socket

import com.example.taxi.domain.model.order.Address
import com.example.taxi.domain.model.order.OrderData
import com.example.taxi.domain.model.order.Settings
import com.example.taxi.domain.model.order.Type
import com.squareup.moshi.*


@JsonClass(generateAdapter = true)
data class SocketMessage<T>(
    val status: Int,
    val key: String,
    val data: T
)


@JsonClass(generateAdapter = true)
data class SocketOrderCancelledData(
    val order_id: Int,
    val driver_id: Int? = -1
)

@JsonClass(generateAdapter = true)
data class SocketOnlyForYouData(
    val id: Int,
    val type: SocketType,
    @Json(name = "services") val orderServices: List<Service>,
    val address: SocketAddress,
    @Json(name = "start_cost") val startCost: Int,
    @Json(name = "latitude1") val latitude1: String,
    @Json(name = "longitude1") val longitude1: String,
    @Json(name = "latitude2") val latitude2: String?,
    @Json(name = "longitude2") val longitude2: String?,
    val comment: String?,
    val mode: SocketMode,
    val settings: List<Settings>?,
    val distance: Int? = 0
){
    fun getCostPerKm(): Int {
        val setting = settings?.find { it.name == "cost_per_km" }
        return setting?.value?.toIntOrNull() ?: 0
    }
}

@JsonClass(generateAdapter = true)
data class   SocketType(
    val number: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class Service(
    val name: String,
    val icon: String,
    val cost: Int
)

@JsonClass(generateAdapter = true)
data class SocketAddress(
    val from: String,
    val to: String
)

@JsonClass(generateAdapter = true)
data class SocketMode(
    val number: Int,
    val name: String
)


fun SocketOnlyForYouData.toOrderData(): OrderData<Address> {
    return OrderData(
        id = this.id,
        type = this.type?.let {
            Type(
                number = it.number,
                name = it.name
            )
        },
        services = this.orderServices.map {
            com.example.taxi.domain.model.order.Service(
                name = it.name,
                icon = it.icon,
                cost = it.cost
            )
        },
        address = Address(
            from = this.address.from,
            to = this.address.to
        ),
        start_cost = this.startCost,
        latitude1 = this.latitude1,
        longitude1 = this.longitude1,
        latitude2 = this.latitude2,
        longitude2 = this.longitude2,
        comment = this.comment
    )
}



