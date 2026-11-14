package com.reelcounter

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast

class ReelCounterService : AccessibilityService() {

    companion object {
        const val IG = "com.instagram.android"
        const val YT = "com.google.android.youtube"

        // View-id fragments that identify the Reels / Shorts player screens.
        // If the apps change their layouts, tweak these lists.
        private val IG_IDS = listOf("clips_viewer", "clips_swipe_refresh")
        private val YT_IDS = listOf("reel_recycler", "reel_player_page_container", "reel_watch")
    }

    private var overlay: TextView? = null
    private var currentPkg: String? = null
    private var inReels = false
    private var lastCount = 0L
    private var lastEval = 0L
    private var lastBlock = 0L

    private fun isTarget(pkg: String) = pkg == IG || pkg == YT
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindow(pkg)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (isTarget(pkg)) evaluate(pkg, true)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (isTarget(pkg)) evaluate(pkg, false)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun onWindow(pkg: String) {
        if (isTarget(pkg)) {
            if (currentPkg != pkg) inReels = false
            currentPkg = pkg
            showOverlay()
            updateOverlay()
            if (Store.blockEntireApp(this) && Store.limitReached(this)) block()
        } else if (pkg != "com.android.systemui" && !pkg.contains("inputmethod") && !pkg.contains("keyboard")) {
            currentPkg = null
            inReels = false
            hideOverlay()
        }
    }

    private fun evaluate(pkg: String, scroll: Boolean) {
        val now = System.currentTimeMillis()
        if (!scroll && now - lastEval < 400) return
        lastEval = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val reels = scan(root, pkg, 0)
        val entered = reels && !inReels
        inReels = reels
        if (!reels) return

        if ((entered && now - lastCount > 1500) || (scroll && now - lastCount > 600)) {
            countReel(pkg, now)
        }
    }

    private fun scan(n: AccessibilityNodeInfo?, pkg: String, depth: Int): Boolean {
        if (n == null || depth > 40) return false
        val id = n.viewIdResourceName
        if (id != null) {
            val keys = if (pkg == IG) IG_IDS else YT_IDS
            if (keys.any { id.contains(it) }) return true
            if (pkg == IG && n.isSelected && id.endsWith("clips_tab")) return true
        }
        if (pkg == YT && n.isSelected &&
            n.contentDescription?.toString()?.startsWith("Shorts", ignoreCase = true) == true
        ) return true
        for (i in 0 until n.childCount) if (scan(n.getChild(i), pkg, depth + 1)) return true
        return false
    }

    private fun countReel(pkg: String, now: Long) {
        val limit = Store.limit(this)
        if (limit > 0 && Store.total(this) >= limit) { block(); return }
        lastCount = now
        Store.increment(this, if (pkg == IG) "ig" else "yt")
        updateOverlay()
        if (limit > 0 && Store.total(this) >= limit) {
            Toast.makeText(this, "Daily limit of $limit reached. Next reel will be blocked.", Toast.LENGTH_LONG).show()
        }
    }

    private fun block() {
        val now = System.currentTimeMillis()
        if (now - lastBlock < 1500) return
        lastBlock = now
        inReels = false
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Daily reel/shorts limit reached. See you tomorrow!", Toast.LENGTH_LONG).show()
    }

    // ---------- Overlay pill near the notch ----------

    private fun showOverlay() {
        if (overlay != null) return
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = GradientDrawable().apply {
                setColor(0xCC000000.toInt())
                cornerRadius = dp(12).toFloat()
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = dp(48)   // shifted right so it doesn't sit on a centre camera hole
            y = dp(4)
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, lp)
            overlay = tv
        } catch (_: Exception) { }
    }

    private fun updateOverlay() {
        val tv = overlay ?: return
        val limit = Store.limit(this)
        val total = Store.total(this)
        tv.text = if (limit > 0) "\u25B6 $total / $limit" else "\u25B6 $total"
        tv.background = (tv.background as GradientDrawable).apply {
            setColor(if (limit > 0 && total >= limit) 0xDDD32F2F.toInt() else 0xCC000000.toInt())
        }
    }

    private fun hideOverlay() {
        overlay?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) { }
        }
        overlay = null
    }
}
