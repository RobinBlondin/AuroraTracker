package com.example.auroratracker

/**
 * Data class representing the thresholds for aurora tracking.
 *
 * @property minKp Minimum Kp index value for aurora visibility.
 * @property maxDistance Maximum distance from the aurora point in meters.
 */
data class Thresholds(val minKp: Int, val maxDistance: Double)

