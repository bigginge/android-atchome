package com.atchome.atc.model

/**
 * A historical position point along an aircraft's trail.
 * Offsets are in nautical miles east and north from home location.
 */
data class TrailPoint(
    val eastNm: Float,
    val northNm: Float
)
