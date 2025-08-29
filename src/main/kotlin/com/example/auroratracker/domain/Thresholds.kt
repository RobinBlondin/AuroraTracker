package com.example.auroratracker.domain

/**
 * Data class representing the thresholds for aurora tracking.
 *
 * @property minKp Minimum Kp index value for aurora visibility.
 * @property maxDistance Maximum distance from the aurora point in meters.
 * @property minProbability Minimum probability value in a NOAA aurora point data
 */
data class Thresholds(val minKp: Int, val maxDistance: Double, val minProbability: Int)