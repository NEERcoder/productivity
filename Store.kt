package com.reelcounter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Store {
    private fun p(c: Context) = c.getSharedPreferences("reel_counter", Context.MODE_PRIVATE)
    private val fmt get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dayKey(daysAgo: Int = 0): String {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return fmt.format(cal.time)
    }

    fun get(c: Context, app: String, day: String = dayKey()) = p(c).getInt("count_${day}_$app", 0)
    fun total(c: Context, day: String = dayKey()) = get(c, "ig", day) + get(c, "yt", day)

    fun increment(c: Context, app: String) {
        val k = "count_${dayKey()}_$app"
        p(c).edit().putInt(k, p(c).getInt(k, 0) + 1).apply()
    }

    fun resetToday(c: Context) {
        p(c).edit().putInt("count_${dayKey()}_ig", 0).putInt("count_${dayKey()}_yt", 0).apply()
    }

    /** 0 = no limit */
    fun limit(c: Context) = p(c).getInt("limit", 100)
    fun setLimit(c: Context, v: Int) = p(c).edit().putInt("limit", v).apply()

    /** true = whole app is blocked after limit; false = only the Reels/Shorts section */
    fun blockEntireApp(c: Context) = p(c).getBoolean("block_entire", true)
    fun setBlockEntire(c: Context, v: Boolean) = p(c).edit().putBoolean("block_entire", v).apply()

    fun limitReached(c: Context): Boolean { val l = limit(c); return l > 0 && total(c) >= l }
}
