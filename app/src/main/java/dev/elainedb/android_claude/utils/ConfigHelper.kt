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
}