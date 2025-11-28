package my.application.ui.utils

import android.util.Log
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Utility functions for formatting dates and timestamps
 */
object DateUtils {
    private const val TAG = "DateUtils"

    /**
     * Format an ISO timestamp string to a user-friendly display format
     * Examples:
     * - Today: "14:30"
     * - Yesterday: "I går"
     * - Within a week: "3 dager siden"
     * - Older: "15.11.2024"
     *
     * Note: Backend sends timestamps already converted to Europe/Oslo timezone
     * but without timezone offset info, so we parse them as LocalDateTime
     */
    fun formatTimestamp(isoTimestamp: String?): String {
        if (isoTimestamp == null) return "Ukjent"

        Log.d(TAG, "==========================================")
        Log.d(TAG, "RAW TIMESTAMP RECEIVED: '$isoTimestamp'")
        Log.d(TAG, "Current time now: ${java.time.LocalDateTime.now()}")

        return try {
            // Backend sends timestamps like "2024-11-28T14:30:00" (no timezone info)
            // These are already in Europe/Oslo timezone, so parse as LocalDateTime
            val cleanedTimestamp = isoTimestamp.substringBefore("+").substringBefore("Z")
            Log.d(TAG, "Cleaned timestamp: '$cleanedTimestamp'")

            val dateTime = if (isoTimestamp.contains("T")) {
                // ISO format with T separator
                java.time.LocalDateTime.parse(cleanedTimestamp)
            } else {
                // Format with space separator
                java.time.LocalDateTime.parse(isoTimestamp.replace(" ", "T"))
            }

            Log.d(TAG, "Parsed as LocalDateTime: $dateTime")

            val now = java.time.LocalDateTime.now()
            Log.d(TAG, "Current LocalDateTime: $now")

            val daysBetween = ChronoUnit.DAYS.between(
                dateTime.toLocalDate(),
                now.toLocalDate()
            )

            Log.d(TAG, "Days between: $daysBetween")

            val result = when {
                daysBetween == 0L -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }

            Log.d(TAG, "FORMATTED RESULT: '$result'")
            Log.d(TAG, "==========================================")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp: $isoTimestamp", e)
            // Try parsing with ZonedDateTime as fallback
            try {
                Log.d(TAG, "Trying ZonedDateTime fallback...")
                val zonedDateTime = ZonedDateTime.parse(isoTimestamp)
                    .withZoneSameInstant(java.time.ZoneId.systemDefault())
                Log.d(TAG, "Parsed as ZonedDateTime: $zonedDateTime")
                val now = ZonedDateTime.now()
                Log.d(TAG, "Current ZonedDateTime: $now")

                val daysBetween = ChronoUnit.DAYS.between(
                    zonedDateTime.toLocalDate(),
                    now.toLocalDate()
                )

                Log.d(TAG, "Days between (fallback): $daysBetween")

                val result = when {
                    daysBetween == 0L -> zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    daysBetween == 1L -> "I går"
                    daysBetween < 7 -> "$daysBetween dager siden"
                    else -> zonedDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                }

                Log.d(TAG, "FALLBACK FORMATTED RESULT: '$result'")
                Log.d(TAG, "==========================================")
                result
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback formatting also failed", e2)
                Log.d(TAG, "==========================================")
                "Ukjent"
            }
        }
    }

    /**
     * Format a timestamp in milliseconds to a user-friendly display format
     */
    fun formatTimestampMillis(timestampMillis: Long): String {
        return try {
            val dateTime = java.time.Instant.ofEpochMilli(timestampMillis)
                .atZone(java.time.ZoneId.systemDefault())
            val now = ZonedDateTime.now()

            val daysBetween = ChronoUnit.DAYS.between(
                dateTime.toLocalDate(),
                now.toLocalDate()
            )

            when {
                daysBetween == 0L -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp millis", e)
            "Ukjent"
        }
    }
}

