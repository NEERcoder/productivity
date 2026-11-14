package com.reelcounter

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var total: TextView
    private lateinit var breakdown: TextView
    private lateinit var history: TextView
    private lateinit var limitEt: EditText
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 1000) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(24))
        }
        fun text(size: Float, bold: Boolean = false, color: Int = 0xFF222222.toInt()) =
            TextView(this).apply {
                textSize = size; setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
                col.addView(this)
            }
        fun button(label: String, onClick: () -> Unit) =
            Button(this).apply { text = label; setOnClickListener { onClick() }; col.addView(this) }

        text(24f, true).text = "Reel Counter"
        status = text(14f)
        text(14f).text = "\nToday"
        total = text(56f, true, 0xFFE1306C.toInt())
        breakdown = text(16f)

        text(16f, true).text = "\nDaily limit (0 = no limit)"
        limitEt = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Store.limit(this@MainActivity).toString())
            col.addView(this)
        }
        button("Save limit") {
            val v = limitEt.text.toString().toIntOrNull() ?: 0
            Store.setLimit(this, v)
            Toast.makeText(this, if (v > 0) "Limit set to $v" else "Limit disabled", Toast.LENGTH_SHORT).show()
        }
        Switch(this).apply {
            text = "Block the entire app after limit\n(off = block only Reels/Shorts)"
            isChecked = Store.blockEntireApp(this@MainActivity)
            setOnCheckedChangeListener { _, c -> Store.setBlockEntire(this@MainActivity, c) }
            col.addView(this)
        }

        text(16f, true).text = "\nSetup"
        button("1. Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("Setting greyed out? Allow restricted settings") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        text(12f, false, 0xFF666666.toInt()).text =
            "Android 13+: App info > \u22EE menu > Allow restricted settings, then enable \"Reel Counter\" in Accessibility."

        text(16f, true).text = "\nLast 7 days"
        history = text(14f)
        button("Reset today's count") {
            Store.resetToday(this); refresh()
        }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() { super.onResume(); tick.run() }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun serviceEnabled(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/") && s.contains("ReelCounterService")
    }

    private fun refresh() {
        val on = serviceEnabled()
        status.text = if (on) "\u25CF Service running" else "\u25CB Service OFF - enable it in Accessibility settings"
        status.setTextColor(if (on) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt())
        val limit = Store.limit(this)
        val t = Store.total(this)
        total.text = if (limit > 0) "$t / $limit" else "$t"
        breakdown.text = "Instagram Reels: ${Store.get(this, "ig")}\nYouTube Shorts: ${Store.get(this, "yt")}"
        val f = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        val sb = StringBuilder()
        for (i in 0..6) {
            val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i)
            val k = Store.dayKey(i)
            sb.append("${f.format(cal.time)}: ${Store.total(this, k)}  (IG ${Store.get(this, "ig", k)} \u00B7 YT ${Store.get(this, "yt", k)})\n")
        }
        history.text = sb.toString().trim()
    }
}
