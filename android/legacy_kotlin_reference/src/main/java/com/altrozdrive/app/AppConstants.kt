package com.altrozdrive.app

import androidx.compose.ui.graphics.Color

internal val Gold = Color(0xFFF1BD4B)
internal val Green = Color(0xFF64D8A3)
internal val LeafGreen = Color(0xFF35C759)
internal val Amber = Color(0xFFFFB454)
internal val Muted = Color(0xFF9FAABD)

internal const val PREFS = "altrozdrive_preferences"
internal const val KEY_APPEARANCE = "appearance"
internal const val KEY_DISTANCE = "distance"
internal const val KEY_ECONOMY = "economy"
internal const val KEY_TEMP = "temperature"
internal const val KEY_ACCENT = "accent"
internal const val KEY_VEHICLE_MODEL = "vehicle_model"
internal const val KEY_VEHICLE_TRIM = "vehicle_trim"
internal const val KEY_VEHICLE_FUEL = "vehicle_fuel"
internal const val KEY_VEHICLE_TRANSMISSION = "vehicle_transmission"
internal const val KEY_FIRST_LAUNCH_SETUP_COMPLETE = "first_launch_setup_complete"
internal const val KEY_NOTIFICATION_PERMISSION_PROMPTED = "notification_permission_prompted"

internal val tataIceVehicles = listOf(
    VehicleOption("Tiago", listOf("XE","XM","XT","XZ+"), listOf("Petrol","CNG"), listOf("5MT","AMT")),
    VehicleOption("Tigor", listOf("XE","XM","XZ","XZA+"), listOf("Petrol","CNG"), listOf("5MT","AMT")),
    VehicleOption("Altroz", listOf("XE","XM+","XM+ S","XT","XZ","XZ+ S","XZA+ S"), listOf("Petrol","Diesel","CNG"), listOf("5MT","6MT","DCA")),
    VehicleOption("Punch", listOf("Pure","Adventure","Accomplished","Creative"), listOf("Petrol","CNG"), listOf("5MT","AMT")),
    VehicleOption("Nexon", listOf("Smart","Smart+","Smart+ S","Pure+","Pure+ S","Creative","Creative+ S","Creative+ PS","Fearless+ PS"), listOf("Petrol","Diesel","CNG"), listOf("5MT","6MT","6AMT","7DCA")),
    VehicleOption("Curvv", listOf("Pure","Pure+","Creative","Creative+ S","Accomplished S","Accomplished+ A"), listOf("Petrol","Diesel"), listOf("6MT","7DCA","6DCA")),
    VehicleOption("Harrier", listOf("Pure","Adventure","Accomplished","Fearless","Fearless+"), listOf("Diesel"), listOf("6MT","6AT")),
    VehicleOption("Safari", listOf("Smart","Pure","Adventure","Accomplished","Accomplished+","Fearless","Fearless+"), listOf("Diesel"), listOf("6MT","6AT")),
    VehicleOption("Sierra", listOf("Pure","Adventure","Accomplished","Fearless"), listOf("Petrol","Diesel"), listOf("6MT","6AT","DCA"))
)
