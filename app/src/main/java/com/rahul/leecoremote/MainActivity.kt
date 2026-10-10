package com.rahul.leecoremote

import android.content.Context
import android.content.Intent
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var statusDot: TextView
    private lateinit var connectAction: TextView
    private var adb: Kadb? = null
    private val prefs by lazy { getSharedPreferences("remote", Context.MODE_PRIVATE) }

    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { sendText(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        status = findViewById(R.id.status)
        statusDot = findViewById(R.id.statusDot)
        connectAction = findViewById(R.id.connectAction)

        findViewById<Button>(R.id.tabRemote).setOnClickListener { remotePage() }
        findViewById<Button>(R.id.tabTouch).setOnClickListener { touchPage() }
        findViewById<Button>(R.id.tabApps).setOnClickListener { appsPage() }
        findViewById<Button>(R.id.tabMacro).setOnClickListener { macroPage() }
        findViewById<Button>(R.id.tabSettings).setOnClickListener { connectPage() }
        findViewById<Button>(R.id.tabUpdate).setOnClickListener { updatePage() }
        listOf(R.id.tabRemote, R.id.tabTouch, R.id.tabApps, R.id.tabMacro, R.id.tabSettings, R.id.tabUpdate).forEach { id ->
            findViewById<Button>(id).apply {
                isAllCaps = false
                setTextColor(Color.rgb(21, 42, 58))
                background = getDrawable(R.drawable.bg_glass_button)
                stateListAnimator = null
                elevation = dp(2).toFloat()
                textSize = 12f
            }
        }
        connectAction.setOnClickListener { connectPage() }
        remotePage()
    }

    private fun clear() = content.removeAllViews()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.rgb(54,77,93))
        textSize = 11f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        letterSpacing = .08f
        setPadding(dp(4), dp(18), dp(4), dp(9))
    }

    private fun neuButton(text: String, large: Boolean = false, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.rgb(21,42,58))
        textSize = if (large) 16f else 12f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = getDrawable(R.drawable.bg_glass_button)
        stateListAnimator = null
        elevation = dp(2).toFloat()
        setPadding(dp(8), dp(6), dp(8), dp(6))
        setOnClickListener { action() }
    }

    private fun addRow(vararg views: View, height: Int = 62) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        views.forEach { v -> row.addView(v, LinearLayout.LayoutParams(0, dp(height), 1f).apply { setMargins(dp(5), dp(5), dp(5), dp(5)) }) }
        content.addView(row)
    }

    private fun hero(text: String, action: () -> Unit) {
        val b = neuButton(text, true, action)
        b.layoutParams = LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, dp(6), 0, dp(6)) }
        content.addView(b)
    }

    private fun key(code: String) = shell("input keyevent $code")

    private fun shell(cmd: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val a = adb ?: throw Exception("TV is not connected")
                a.shell(cmd)
                withContext(Dispatchers.Main) { setConnectedStatus("Connected  •  Command sent") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { setErrorStatus(e.message ?: "Command failed") }
            }
        }
    }

    private fun setConnectedStatus(text: String) {
        status.text = text
        statusDot.setTextColor(Color.rgb(78,155,114))
        connectAction.text = "CONNECTED"
    }

    private fun setErrorStatus(text: String) {
        status.text = text
        statusDot.setTextColor(Color.rgb(196,93,103))
        connectAction.text = "CONNECT"
    }

    private fun remotePage() {
        clear()
        content.addView(label("PRIMARY CONTROLS"))
        addRow(neuButton("⏻\nPower") { key("KEYCODE_POWER") }, neuButton("◉\nMute") { key("KEYCODE_MUTE") }, neuButton("⌂\nHome") { key("KEYCODE_HOME") })

        content.addView(label("NAVIGATION"))
        addRow(neuButton("↑") { key("KEYCODE_DPAD_UP") }, neuButton("OK", true) {}.also { it.setOnClickListener { key("KEYCODE_DPAD_CENTER") } }, neuButton("Info") { key("KEYCODE_INFO") })
        addRow(neuButton("←") { key("KEYCODE_DPAD_LEFT") }, neuButton("Back") { key("KEYCODE_BACK") }, neuButton("→") { key("KEYCODE_DPAD_RIGHT") })
        addRow(neuButton("Menu") { key("KEYCODE_MENU") }, neuButton("↓") { key("KEYCODE_DPAD_DOWN") }, neuButton("Guide") { key("KEYCODE_GUIDE") })

        content.addView(label("VOLUME & CHANNEL"))
        addRow(neuButton("−\nVolume") { key("KEYCODE_VOLUME_DOWN") }, neuButton("+\nVolume") { key("KEYCODE_VOLUME_UP") }, neuButton("−\nChannel") { key("KEYCODE_CHANNEL_DOWN") }, neuButton("+\nChannel") { key("KEYCODE_CHANNEL_UP") })

        content.addView(label("MEDIA"))
        addRow(neuButton("Previous") { key("KEYCODE_MEDIA_PREVIOUS") }, neuButton("▶  Play / Pause", true) {}.also { it.setOnClickListener { key("KEYCODE_MEDIA_PLAY_PAUSE") } }, neuButton("Next") { key("KEYCODE_MEDIA_NEXT") })        addRow(neuButton("Stop") { key("KEYCODE_MEDIA_STOP") }, neuButton("Sleep") { key("KEYCODE_SLEEP") }, neuButton("Search") { key("KEYCODE_SEARCH") })

        content.addView(label("NUMBER PAD"))
        listOf("1","2","3","4","5","6","7","8","9","0").chunked(3).forEach { nums ->
            addRow(*nums.map { n -> neuButton(n) { key("KEYCODE_$n") } }.toTypedArray())
        }
        hero("🎙  Voice Search") { startVoice() }
    }

    private fun touchPage() {
        clear()
        content.addView(label("TOUCH CONTROL"))
        val info = TextView(this).apply {
            text = "Swipe to navigate  •  Tap to select  •  Long press for action"
            setTextColor(Color.rgb(54,77,93)); textSize = 13f; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(info)
        val pad = FrameLayout(this).apply {
            background = getDrawable(R.drawable.bg_glass_inset)
            minimumHeight = dp(380)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        var sx = 0f; var sy = 0f; var downAt = 0L
        pad.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx=e.x; sy=e.y; downAt=System.currentTimeMillis(); true }
                MotionEvent.ACTION_UP -> {
                    val dx=e.x-sx; val dy=e.y-sy
                    if (kotlin.math.abs(dx)<35 && kotlin.math.abs(dy)<35) key("KEYCODE_DPAD_CENTER")
                    else shell("input swipe ${sx.toInt()} ${sy.toInt()} ${e.x.toInt()} ${e.y.toInt()} 180")
                    true
                }
                else -> true
            }
        }
        content.addView(pad, LinearLayout.LayoutParams(-1, dp(380)).apply { setMargins(0,dp(6),0,dp(10)) })
        addRow(neuButton("Back") { key("KEYCODE_BACK") }, neuButton("Home") { key("KEYCODE_HOME") }, neuButton("OK", true) {}.also { it.setOnClickListener { key("KEYCODE_DPAD_CENTER") } })
        content.addView(label("TEXT INPUT"))
        hero("⌨  Send Text / Keyboard") { keyboardDialog() }
        hero("🎙  Voice to TV") { startVoice() }
    }

    private fun appsPage() {
        clear(); content.addView(label("QUICK LAUNCH"))
        val apps = linkedMapOf(
            "SmartTube" to "com.teamsmart.videomanager.tv",
            "Netflix" to "com.netflix.ninja",
            "Prime Video" to "com.amazon.amazonvideo.livingroom",
            "JioHotstar" to "in.startv.hotstar",
            "Settings" to "com.android.settings"
        )
        apps.entries.chunked(2).forEach { pair -> addRow(*pair.map { (name,pkg) -> neuButton(name) { shell("monkey -p $pkg 1") } }.toTypedArray(), height=76) }
        content.addView(label("CUSTOM"))
        hero("＋  Launch package manually") { packageDialog() }
        hero("↻  Refresh package list") { shell("pm list packages") }
    }

    private fun macroPage() {
        clear(); content.addView(label("SMART SCENES"))
        hero("Home  →  SmartTube") { sequence("KEYCODE_HOME", "com.teamsmart.videomanager.tv") }
        hero("Home  →  JioHotstar") { sequence("KEYCODE_HOME", "in.startv.hotstar") }
        hero("Home  →  Netflix") { sequence("KEYCODE_HOME", "com.netflix.ninja") }
        hero("Power Toggle") { key("KEYCODE_POWER") }
        hero("Mute  →  Home") { key("KEYCODE_MUTE"); lifecycleScope.launch { delay(180); key("KEYCODE_HOME") } }
        content.addView(label("These scenes are editable in MainActivity.kt for your TV and apps."))
    }

    private fun sequence(keyCode: String, pkg: String) {
        key(keyCode); lifecycleScope.launch { delay(350); shell("monkey -p $pkg 1") }
    }

    private fun updatePage() {
        clear()
        content.addView(label("TV SOFTWARE UPDATE"))

        val versionCard = TextView(this).apply {
            text = "Current firmware: checking…"
            setTextColor(Color.rgb(21,42,58))
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = getDrawable(R.drawable.bg_glass_card)
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }
        content.addView(versionCard, LinearLayout.LayoutParams(-1, dp(74)).apply { setMargins(0,dp(4),0,dp(8)) })

        hero("↻  Check for TV Update") {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val a = adb ?: throw Exception("Connect to the TV first")
                    val props = listOf(
                        "ro.build.display.id",
                        "ro.build.version.release",
                        "ro.build.version.incremental",
                        "ro.product.model",
                        "ro.product.device"
                    ).associateWith { key -> a.shell("getprop $key").output.trim() }
                    withContext(Dispatchers.Main) {
                        versionCard.text = "${props["ro.product.model"].orEmpty().ifEmpty { "LeEco TV" }}\nBuild: ${props["ro.build.display.id"].orEmpty().ifEmpty { "Unknown" }}\nAndroid: ${props["ro.build.version.release"].orEmpty().ifEmpty { "Unknown" }}"
                        setConnectedStatus("Firmware information read successfully")
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Update check")
                            .setMessage("The TV reports its current firmware. Automatic firmware download/install is only enabled when the TV exposes a supported OTA/update interface. This app will not force-flash an unknown package.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { setErrorStatus(e.message ?: "Could not read firmware") }
                }
            }
        }

        content.addView(label("UPDATE OPTIONS"))
        hero("⚙  Open TV System Update") {
            shell("am start -a android.settings.SYSTEM_UPDATE_SETTINGS")
        }
        hero("ℹ  Read Build / Device Info") {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val a = adb ?: throw Exception("Connect to the TV first")
                    val info = a.shell("getprop").output
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("TV diagnostics")
                            .setMessage(info.take(12000).ifEmpty { "No diagnostic information returned." })
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { setErrorStatus(e.message ?: "Diagnostics failed") }
                }
            }
        }
        hero("↻  Restart TV") { key("KEYCODE_POWER") }

        content.addView(label("SAFETY"))
        val note = TextView(this).apply {
            text = "Firmware flashing is intentionally not automatic. Only install firmware intended for the exact TV model and hardware revision. If the TV provides its own System Update screen, use that official updater."
            setTextColor(Color.rgb(54,77,93))
            textSize = 13f
            setPadding(dp(4), 0, dp(4), dp(18))
        }
        content.addView(note)
    }

    private fun connectPage() {
        clear(); content.addView(label("WIRELESS ADB"))
        val ip = edit("TV IP address", prefs.getString("ip", ""), InputType.TYPE_CLASS_PHONE)
        val port = edit("ADB port", prefs.getString("port", "5555"), InputType.TYPE_CLASS_NUMBER)
        val pairPort = edit("Pairing port (Android 11+)", prefs.getString("pairPort", ""), InputType.TYPE_CLASS_NUMBER)
        val code = edit("6-digit pairing code", "", InputType.TYPE_CLASS_NUMBER)
        listOf(ip,port,pairPort,code).forEach { content.addView(it, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0,dp(5),0,dp(5)) }) }
        hero("PAIR WIRELESS ADB") {
            lifecycleScope.launch(Dispatchers.IO) {
                try { Kadb.pair(ip.text.toString(), pairPort.text.toString().toInt(), code.text.toString()); withContext(Dispatchers.Main){ setConnectedStatus("Pairing succeeded  •  Now connect") } }
                catch(e:Exception){ withContext(Dispatchers.Main){ setErrorStatus("Pair failed: ${e.message}") } }
            }
        }
        hero("CONNECT TO TV") { connect(ip.text.toString(), port.text.toString().toIntOrNull() ?: 5555) }
        hero("DISCONNECT") { adb?.close(); adb=null; status.text="Not connected"; statusDot.setTextColor(Color.rgb(54,77,93)); connectAction.text="CONNECT" }
        content.addView(label("REQUIREMENT"))
        val note = TextView(this).apply { text="Phone and TV must be on the same Wi‑Fi. The TV must expose ADB over network / wireless debugging. This remote does not bypass Android security or enable ADB automatically."; setTextColor(Color.rgb(54,77,93)); textSize=13f; setPadding(dp(4),0,dp(4),dp(16)) }
        content.addView(note)
    }

    private fun edit(hint: String, value: String?, type: Int) = EditText(this).apply {
        this.hint=hint; setText(value ?: ""); inputType=type; setTextColor(Color.rgb(21,42,58)); setHintTextColor(Color.rgb(75,96,111)); background=getDrawable(R.drawable.bg_glass_inset); setPadding(dp(16),0,dp(16),0)
    }

    private fun connect(host:String, port:Int) {
        prefs.edit { putString("ip",host); putString("port",port.toString()) }
        lifecycleScope.launch(Dispatchers.IO) {
            try { adb?.close(); adb=Kadb.create(host,port); val r=adb!!.shell("getprop ro.product.model"); withContext(Dispatchers.Main){ setConnectedStatus("Connected  •  ${r.output.trim().ifEmpty{"LeEco TV"}}") } }
            catch(e:Exception){ adb=null; withContext(Dispatchers.Main){ setErrorStatus("Connection failed: ${e.message}") } }
        }
    }

    private fun startVoice() {
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        speech.launch(i)
    }

    private fun keyboardDialog() {
        val input=EditText(this).apply { hint="Type text for the TV"; setTextColor(Color.rgb(21,42,58)) }
        AlertDialog.Builder(this).setTitle("Send text").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Send") { _,_-> sendText(input.text.toString()) }.show()
    }

    private fun packageDialog() {
        val input=EditText(this).apply { hint="com.example.tvapp"; setTextColor(Color.rgb(21,42,58)) }
        AlertDialog.Builder(this).setTitle("Launch app package").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Launch") { _,_-> shell("monkey -p ${input.text} 1") }.show()
    }

    private fun sendText(s:String) {
        if(s.isBlank()){ key("KEYCODE_SEARCH"); return }
        val escaped=s.replace(" ","%s").replace("'","\\'")
        shell("input text '$escaped'")
    }
}
