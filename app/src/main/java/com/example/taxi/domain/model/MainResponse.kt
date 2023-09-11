package com.example.taxi.domain.model

data class MainResponse<T>(
    val success: Boolean,
    val status: Int,
    val name: String,
    val message: String,
    val step: Int,
    var data: T
)
