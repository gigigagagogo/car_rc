package com.example.car_rc_client

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var esp32: Esp32Client
    private val uiHandler = Handler(Looper.getMainLooper())

    // Stato comando
    private var throttle = 0   // -255..255
    private var steer = 0      // -255..255

    // Parametri “feel”
    private val STEP_THROTTLE = 18
    private val STEP_STEER = 22
    private val SEND_PERIOD_MS = 50L
    private val MAX = 255

    // Stato press&hold
    private var holdingForward = false
    private var holdingBackward = false
    private var holdingLeft = false
    private var holdingRight = false

    private var lastSentT = 999
    private var lastSentR = 999

    private val connectionChecker = object : Runnable {
        override fun run() {
            esp32.checkConnectionAndReconnect()
            uiHandler.postDelayed(this, 5000) // Controlla ogni 5 secondi
        }
    }


    private val sendLoop = object : Runnable {
        override fun run() {
            throttle = when {
                holdingForward && !holdingBackward -> (throttle + STEP_THROTTLE).coerceAtMost(MAX)
                holdingBackward && !holdingForward -> (throttle - STEP_THROTTLE).coerceAtLeast(-MAX)
                else -> 0
            }

            // Steer: sinistra/destra
            steer = when {
                holdingLeft && !holdingRight -> (steer - STEP_STEER).coerceAtLeast(-MAX)
                holdingRight && !holdingLeft -> (steer + STEP_STEER).coerceAtMost(MAX)
                else -> 0
            }

            if (throttle != lastSentT || steer != lastSentR) {
                esp32.sendCommand(throttle, steer)
                lastSentT = throttle
                lastSentR = steer
            }
            uiHandler.postDelayed(this, SEND_PERIOD_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        esp32 = Esp32Client("192.168.1.100", 3333)
        esp32.connect(
            onConnected = {
                runOnUiThread {
                    Toast.makeText(this, "Connesso alla macchina!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { e ->
                runOnUiThread {
                    Toast.makeText(this, "Connessione fallita. Riconnessione automatica...", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Avvia il controllo periodico della connessione
        uiHandler.postDelayed(connectionChecker, 5000)

        // AVVIA IL LOOP DI INVIO COMANDI
        uiHandler.post(sendLoop)  // <-- AGGIUNGI QUESTA RIGA

        val btnLeft = findViewById<Button>(R.id.btnLeft)
        val btnRight = findViewById<Button>(R.id.btnRight)
        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnBackward = findViewById<Button>(R.id.btnBackward)

        btnForward.setHoldListener(
            onDown = { holdingForward = true },
            onUp = {
                holdingForward = false;
                lastSentT = 999;
                lastSentR = 999;
            }
        )

        btnBackward.setHoldListener(
            onDown = { holdingBackward = true },
            onUp = {
                holdingBackward = false;
                lastSentT = 999;
                lastSentR = 999;
            }
        )

        btnLeft.setHoldListener(
            onDown = { holdingLeft = true },
            onUp = {
                holdingLeft = false;
                lastSentT = 999;
                lastSentR = 999;
            }
        )

        btnRight.setHoldListener(
            onDown = { holdingRight = true },
            onUp = {
                holdingRight = false;
                lastSentT = 999;
                lastSentR = 999;
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacks(sendLoop)
        uiHandler.removeCallbacks(connectionChecker)
        esp32.sendCommand(0, 0)
        esp32.disconnect()
    }
}

private fun Button.setHoldListener(onDown: () -> Unit, onUp: () -> Unit) {
    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { onDown(); true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { onUp(); true }
            else -> false
        }
    }
}
