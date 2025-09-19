package dev.elainedb.android_claude.utils

import android.location.Address
import android.location.Geocoder
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Log
import kotlin.coroutines.resume

object LocationUtils {
    private const val TAG = "LocationUtils"

    suspend fun getLocationFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting reverse geocoding for coordinates ($latitude, $longitude)")

        try {
            val geocoder = Geocoder(context)

            // Check if Geocoder is available
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder is not available on this device")
                return@withContext Pair(null, null)
            }

            Log.d(TAG, "Geocoder is available, Android SDK: ${Build.VERSION.SDK_INT}")

            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new API for Android 13+
                Log.d(TAG, "Using new Geocoder API (Android 13+)")
                try {
                    suspendCancellableCoroutine<List<Address>?> { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            Log.d(TAG, "New Geocoder API returned ${addresses?.size ?: 0} addresses")
                            continuation.resume(addresses)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error with new Geocoder API for ($latitude, $longitude)", e)
                    null
                }
            } else {
                // Use the deprecated API for older Android versions
                Log.d(TAG, "Using legacy Geocoder API (pre-Android 13)")
                try {
                    @Suppress("DEPRECATION")
                    val result = geocoder.getFromLocation(latitude, longitude, 1)
                    Log.d(TAG, "Legacy Geocoder API returned ${result?.size ?: 0} addresses")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "Error with legacy Geocoder API for ($latitude, $longitude)", e)
                    null
                }
            }

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                Log.d(TAG, "Address details:")
                Log.d(TAG, "  - locality: ${address.locality}")
                Log.d(TAG, "  - subAdminArea: ${address.subAdminArea}")
                Log.d(TAG, "  - adminArea: ${address.adminArea}")
                Log.d(TAG, "  - subLocality: ${address.subLocality}")
                Log.d(TAG, "  - countryName: ${address.countryName}")
                Log.d(TAG, "  - countryCode: ${address.countryCode}")
                Log.d(TAG, "  - fullAddress: ${address.getAddressLine(0)}")

                // Try different fields for city name in order of preference
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.subLocality
                    ?: address.thoroughfare

                val country = address.countryName ?: address.countryCode

                Log.d(TAG, "Final resolved location for ($latitude, $longitude): city='$city', country='$country'")
                return@withContext Pair(city, country)
            } else {
                Log.w(TAG, "No address found for coordinates ($latitude, $longitude)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error resolving location for ($latitude, $longitude)", e)
        }

        Log.d(TAG, "Returning null for coordinates ($latitude, $longitude)")
        return@withContext Pair(null, null)
    }

    // Test function with known coordinates
    suspend fun testReverseGeocoding(context: Context): Pair<String?, String?> {
        Log.i(TAG, "Testing reverse geocoding with known coordinates (New York City)")
        // Test with New York City coordinates: 40.7128, -74.0060
        return getLocationFromCoordinates(context, 40.7128, -74.0060)
    }
}