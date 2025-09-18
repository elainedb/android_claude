package dev.elainedb.android_claude.utils

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.*

object ConfigHelper {
    private const val TAG = "ConfigHelper"

    fun getAuthorizedEmails(context: Context): List<String> {
        val configFiles = listOf(
            "config.properties",        // Local development
            "config.properties.ci",     // CI environment
            "config.properties.template" // Fallback template
        )

        for (configFile in configFiles) {
            try {
                val inputStream = context.assets.open(configFile)
                val properties = Properties()
                properties.load(inputStream)
                inputStream.close()

                val emailsString = properties.getProperty("authorized_emails", "")
                if (emailsString.isNotBlank()) {
                    val emails = emailsString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    Log.d(TAG, "Loaded ${emails.size} authorized emails from $configFile")
                    return emails
                }
            } catch (e: IOException) {
                Log.d(TAG, "Config file $configFile not found, trying next...")
            }
        }

        // Fallback if no config files are found
        Log.w(TAG, "No config files found, using fallback emails")
        return listOf(
            "fallback1@example.com",
            "fallback2@example.com"
        )
    }

    fun getYouTubeApiKey(context: Context): String {
        val configFiles = listOf(
            "config.properties",        // Local development
            "config.properties.ci",     // CI environment
            "config.properties.template" // Fallback template
        )

        for (configFile in configFiles) {
            try {
                val inputStream = context.assets.open(configFile)
                val properties = Properties()
                properties.load(inputStream)
                inputStream.close()

                val apiKey = properties.getProperty("youtubeApiKey", "")
                    .trim()
                    .replace("`", "") // Remove any backtick characters
                    .replace("\"", "") // Remove any quote characters
                if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY_HERE") {
                    Log.d(TAG, "Loaded YouTube API key from $configFile (length: ${apiKey.length})")
                    return apiKey
                }
            } catch (e: IOException) {
                Log.d(TAG, "Config file $configFile not found, trying next...")
            }
        }

        // Fallback if no API key is found
        Log.w(TAG, "No YouTube API key found in config files")
        return "FALLBACK_API_KEY"
    }
}