package com.atchome.atc.model

/**
 * Airport marker for the radar overlay, projected into east/north NM from home.
 */
data class Airport(
    val ident: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val eastNm: Float,
    val northNm: Float,
    val type: String = "medium"
)
