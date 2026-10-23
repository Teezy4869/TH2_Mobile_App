package com.example.myapplication.data

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"

    private fun dateFormat() = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }

    fun today(): String = dateFormat().format(Date())

    fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    /** Date string [days] away from today (negative = past). */
    fun offset(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return dateFormat().format(cal.time)
    }

    fun parse(date: String): Date? = try {
        if (date.length == DATE_PATTERN.length) dateFormat().parse(date) else null
    } catch (_: ParseException) {
        null
    }

    fun isValid(date: String): Boolean = parse(date) != null

    /** A fruit is expired once its expiry date is before today. */
    fun isExpired(expiryDate: String): Boolean = expiryDate < today()

    /** Days from today until [expiryDate]; negative when already expired. */
    fun daysUntil(expiryDate: String): Long? {
        val expiry = parse(expiryDate) ?: return null
        val today = parse(today()) ?: return null
        return TimeUnit.MILLISECONDS.toDays(expiry.time - today.time)
    }
}
