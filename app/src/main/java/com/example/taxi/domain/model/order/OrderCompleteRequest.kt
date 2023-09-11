package com.example.taxi.domain.model.order

data class OrderCompleteRequest(
    val cost: Int,
    val distance: Int,
    val wait_time: Int,
    val wait_cost: Int
)
