package com.iidx.controller

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.input.InputManager
import android.os.Bundle
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private lateinit var audioEngine: AudioEngine
    private lateinit var scratchEngine: ScratchEngine
    private lateinit var inputManager: InputManager

    private lateinit var turntableView: TurntableView
    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView
    private val keyViews = arrayOfNulls<View>(7)

    // beatmania IIDX Entry Model Bluetooth HID key mappings
    // Primary: standard gamepad buttons
    private val gamepadMap = mapOf(
        KeyEvent.KEYCODE_BUTTON_1 to 0,
        KeyEvent.KEYCODE_BUTTON_2 to 1,
        KeyEvent.KEYCODE_BUTTON_3 to 2,
        KeyEvent.KEYCODE_BUTTON_4 to 3,
        KeyEvent.KEYCODE_BUTTON_5 to 4,
        KeyEvent.KEYCODE_BUTTON_6 to 5,
        KeyEvent.KEYCODE_BUTTON_7 to 6
    )

    // Fallback: keyboard 1-7 for testing
    private val keyboardMap = mapOf(
        KeyEvent.KEYCODE_1 to 0,
        KeyEvent.KEYCODE_2 to 1,
        KeyEvent.KEYCODE_3 to 2,
        KeyEvent.KEYCODE_4 to 3,
        KeyEvent.KEYCODE_5 to 4,
        KeyEvent.KEYCODE_6 to 5,
        KeyEvent.KEYCODE_7 to 6
    )

    // Track which axis is the turntable
    private val lastAxisValues = mutableMapOf<Int, Float>()
    private val TURNTABLE_AXES = intArrayOf(
        MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
        MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
        MotionEvent.AXIS_RX, MotionEvent.AXIS_RY,
        MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
    )

    private val whiteKeyIndices = setOf(0, 2, 4, 6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusDot = findViewById(R.id.statusDot) as View
        tvStatus = findViewById(R.id.tvStatus) as TextView
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

        audioEngine = AudioEngine()
        scratchEngine = ScratchEngine()

        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this, null)

        checkConnectedDevices()
    }

    private fun setupTouchFallback() {
        for (i in 0..6) {
            val view = keyViews[i] ?: continue
            val buttonIndex = i
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        audioEngine.playButton(buttonIndex)
                        setKeyVisual(buttonIndex, true)
                        true
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        setKeyVisual(buttonIndex, false)
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
            if (isController(device)) {
                showConnected(device.name)
                return
            }
        }
        showDisconnected()
    }

    private fun isController(device: InputDevice): Boolean {
        val name = device.name.lowercase()
        val isKnown = name.contains("iidx") || name.contains("beatmania") ||
                      name.contains("konami")
        val isGamepad = (device.sources and InputDevice.SOURCE_GAMEPAD) != 0 ||
                        (device.sources and InputDevice.SOURCE_JOYSTICK) != 0
        return isKnown || isGamepad
    }

    private fun showConnected(deviceName: String) {
        runOnUiThread {
            statusDot.setBackgroundColor(Color.parseColor("#00CC44"))
            tvStatus.text = "${getString(R.string.status_connected)}$deviceName"
        }
    }

    private fun showDisconnected() {
        runOnUiThread {
            statusDot.setBackgroundColor(Color.parseColor("#CC2200"))
            tvStatus.setText(R.string.status_disconnected)
        }
    }

    // --- Key/Button input from Bluetooth HID controller ---

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return super.onKeyDown(keyCode, event)
        val idx = gamepadMap[keyCode] ?: keyboardMap[keyCode]
        if (idx != null) {
            audioEngine.playButton(idx)
            setKeyVisual(idx, true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val idx = gamepadMap[keyCode] ?: keyboardMap[keyCode]
        if (idx != null) {
            setKeyVisual(idx, false)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // --- Turntable (analog axis) input ---

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) != 0 ||
                         (event.source and InputDevice.SOURCE_CLASS_JOYSTICK) != 0
        if (!isJoystick) return super.onGenericMotionEvent(event)

        var maxDelta = 0f
        var turntableDelta = 0f

        for (axis in TURNTABLE_AXES) {
            val value = event.getAxisValue(axis)
            val prev = lastAxisValues.getOrElse(axis) { 0f }
            val delta = value - prev
            lastAxisValues[axis] = value
            if (kotlin.math.abs(delta) > kotlin.math.abs(maxDelta)) {
                maxDelta = delta
                turntableDelta = delta
            }
        }

        if (kotlin.math.abs(turntableDelta) > 0.005f) {
            scratchEngine.setSpeed(turntableDelta)
            turntableView.rotateDelta(turntableDelta * 360f)
        }

        return true
    }

    // --- Visual feedback ---

    private fun setKeyVisual(index: Int, pressed: Boolean) {
        runOnUiThread {
            val view = keyViews[index] ?: return@runOnUiThread
            val isWhite = index in whiteKeyIndices
            view.setBackgroundColor(
                when {
                    pressed -> Color.parseColor("#FFCC00")
                    isWhite -> Color.parseColor("#E0E0E0")
                    else    -> Color.parseColor("#1A1A1A")
                }
            )
        }
    }

    // --- InputDeviceListener ---

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        if (isController(device)) showConnected(device.name)
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        showDisconnected()
    }

    override fun onInputDeviceChanged(deviceId: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        inputManager.unregisterInputDeviceListener(this)
        audioEngine.release()
        scratchEngine.release()
    }
}
