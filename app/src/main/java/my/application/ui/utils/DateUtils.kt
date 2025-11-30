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

        return try {
            // Backend sender tidsstempler som "2024-11-28T14:30:00" (ingen tidssoneinformasjon)
            // Disse er allerede i Europe/Oslo tidssone, så parse som LocalDateTime
            val cleanedTimestamp = isoTimestamp.substringBefore("+").substringBefore("Z")

            val dateTime = if (isoTimestamp.contains("T")) {
                // ISO-format med T-separator
                java.time.LocalDateTime.parse(cleanedTimestamp)
            } else {
                // Format med mellomrom som separator
                java.time.LocalDateTime.parse(isoTimestamp.replace(" ", "T"))
            }

            val now = java.time.LocalDateTime.now()

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
            // Prøv å parse med ZonedDateTime som reserveløsning
            try {
                val zonedDateTime = ZonedDateTime.parse(isoTimestamp)
                    .withZoneSameInstant(java.time.ZoneId.systemDefault())
                val now = ZonedDateTime.now()

                val daysBetween = ChronoUnit.DAYS.between(
                    zonedDateTime.toLocalDate(),
                    now.toLocalDate()
                )

                when {
                    daysBetween == 0L -> zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    daysBetween == 1L -> "I går"
                    daysBetween < 7 -> "$daysBetween dager siden"
                    else -> zonedDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                }
            } catch (e2: Exception) {
                "Ukjent"
            }
        }
    }

    /**
     * Formater et tidsstempel i millisekunder til et brukervennlig visningsformat
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

