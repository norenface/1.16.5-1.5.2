package com.iidx.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity(), InputManager.InputDeviceListener {

    @Volatile private var audioEngine: AudioEngine? = null
    @Volatile private var scratchEngine: ScratchEngine? = null

    private lateinit var inputManager: InputManager
    private lateinit var turntableView: TurntableView
    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView
    private val keyViews = arrayOfNulls<View>(7)

    // beatmania IIDX Entry Model Bluetooth HID key mappings (gamepad buttons)
    private val gamepadMap = mapOf(
        KeyEvent.KEYCODE_BUTTON_1 to 0,
        KeyEvent.KEYCODE_BUTTON_2 to 1,
        KeyEvent.KEYCODE_BUTTON_3 to 2,
        KeyEvent.KEYCODE_BUTTON_4 to 3,
        KeyEvent.KEYCODE_BUTTON_5 to 4,
        KeyEvent.KEYCODE_BUTTON_6 to 5,
        KeyEvent.KEYCODE_BUTTON_7 to 6
    )

    // Keyboard 1-7 fallback (useful for testing on emulator)
    private val keyboardMap = mapOf(
        KeyEvent.KEYCODE_1 to 0,
        KeyEvent.KEYCODE_2 to 1,
        KeyEvent.KEYCODE_3 to 2,
        KeyEvent.KEYCODE_4 to 3,
        KeyEvent.KEYCODE_5 to 4,
        KeyEvent.KEYCODE_6 to 5,
        KeyEvent.KEYCODE_7 to 6
    )

    private val TURNTABLE_AXES = intArrayOf(
        MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
        MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
        MotionEvent.AXIS_RX, MotionEvent.AXIS_RY,
        MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
    )
    private val lastAxisValues = mutableMapOf<Int, Float>()
    private val whiteKeyIndices = setOf(0, 2, 4, 6)

    companion object {
        private const val REQ_BT_PERMS = 1001
        // Android 12+ permission strings (not in API 23 constants, use strings directly)
        private const val PERM_BT_CONNECT = "android.permission.BLUETOOTH_CONNECT"
        private const val PERM_BT_SCAN    = "android.permission.BLUETOOTH_SCAN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusDot     = findViewById(R.id.statusDot)    as View
        tvStatus      = findViewById(R.id.tvStatus)     as TextView
        turntableView = findViewById(R.id.turntableView) as TurntableView

        keyViews[0] = findViewById(R.id.key1) as View
        keyViews[1] = findViewById(R.id.key2) as View
        keyViews[2] = findViewById(R.id.key3) as View
        keyViews[3] = findViewById(R.id.key4) as View
        keyViews[4] = findViewById(R.id.key5) as View
        keyViews[5] = findViewById(R.id.key6) as View
        keyViews[6] = findViewById(R.id.key7) as View

        setupTouchFallback()

        (findViewById(R.id.btnBluetooth) as Button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        // Initialize audio engines on a background thread to prevent blocking onCreate
        Thread {
            try {
                audioEngine = AudioEngine()
                scratchEngine = ScratchEngine()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }.apply { isDaemon = true; start() }

        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this, null)

        // Request Bluetooth permissions on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= 31) {
            val missing = arrayOf(PERM_BT_CONNECT, PERM_BT_SCAN).filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), REQ_BT_PERMS)
            } else {
                checkConnectedDevices()
            }
        } else {
            checkConnectedDevices()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_BT_PERMS) {
            checkConnectedDevices()
        }
    }

    private fun setupTouchFallback() {
        for (i in 0..6) {
            val view = keyViews[i] ?: continue
            val idx = i
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        audioEngine?.playButton(idx)
                        setKeyVisual(idx, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        setKeyVisual(idx, false)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun checkConnectedDevices() {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (isController(device)) { showConnected(device.name); return }
        }
        showDisconnected()
    }

    private fun isController(device: InputDevice): Boolean {
        val name = device.name.lowercase()
        return name.contains("iidx") || name.contains("beatmania") ||
               name.contains("konami") ||
               (device.sources and InputDevice.SOURCE_GAMEPAD) != 0 ||
               (device.sources and InputDevice.SOURCE_JOYSTICK) != 0
    }

    private fun showConnected(name: String) = runOnUiThread {
        statusDot.setBackgroundColor(Color.parseColor("#00CC44"))
        tvStatus.text = "${getString(R.string.status_connected)}$name"
    }

    private fun showDisconnected() = runOnUiThread {
        statusDot.setBackgroundColor(Color.parseColor("#CC2200"))
        tvStatus.setText(R.string.status_disconnected)
    }

    // ---- Bluetooth HID controller input ----

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return super.onKeyDown(keyCode, event)
        val idx = gamepadMap[keyCode] ?: keyboardMap[keyCode]
        if (idx != null) {
            audioEngine?.playButton(idx)
            setKeyVisual(idx, true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val idx = gamepadMap[keyCode] ?: keyboardMap[keyCode]
        if (idx != null) { setKeyVisual(idx, false); return true }
        return super.onKeyUp(keyCode, event)
    }

    // ---- Turntable axis input ----

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) != 0 ||
                         (event.source and InputDevice.SOURCE_CLASS_JOYSTICK) != 0
        if (!isJoystick) return super.onGenericMotionEvent(event)

        var maxDelta = 0f
        var turntableDelta = 0f
        for (axis in TURNTABLE_AXES) {
            val value = event.getAxisValue(axis)
            val prev  = lastAxisValues.getOrElse(axis) { 0f }
            val delta = value - prev
            lastAxisValues[axis] = value
            if (kotlin.math.abs(delta) > kotlin.math.abs(maxDelta)) {
                maxDelta = delta; turntableDelta = delta
            }
        }
        if (kotlin.math.abs(turntableDelta) > 0.005f) {
            scratchEngine?.setSpeed(turntableDelta)
            turntableView.rotateDelta(turntableDelta * 360f)
        }
        return true
    }

    // ---- UI ----

    private fun setKeyVisual(index: Int, pressed: Boolean) = runOnUiThread {
        val view = keyViews[index] ?: return@runOnUiThread
        view.setBackgroundColor(when {
            pressed                  -> Color.parseColor("#FFCC00")
            index in whiteKeyIndices -> Color.parseColor("#E0E0E0")
            else                     -> Color.parseColor("#1A1A1A")
        })
    }

    // ---- InputDeviceListener ----

    override fun onInputDeviceAdded(deviceId: Int) {
        InputDevice.getDevice(deviceId)?.let { if (isController(it)) showConnected(it.name) }
    }
    override fun onInputDeviceRemoved(deviceId: Int) = showDisconnected()
    override fun onInputDeviceChanged(deviceId: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        inputManager.unregisterInputDeviceListener(this)
        audioEngine?.release()
        scratchEngine?.release()
    }
}
