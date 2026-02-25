package com.example.car_rc_client

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Esp32Client(
    private val ip: String,
    private val port: Int
) {
    private val io = Executors.newSingleThreadExecutor()
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val connected = AtomicBoolean(false)
    private var reconnectAttempts = 0
    private val MAX_RECONNECT_ATTEMPTS = 10
    private val RECONNECT_DELAY_MS = 2000L

    // Aggiungi Handler per la UI
    private val uiHandler = Handler(Looper.getMainLooper())

    fun connect(onConnected: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null) {
        io.execute {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 3000) // Timeout aumentato
                socket = s
                writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                connected.set(true)
                reconnectAttempts = 0
                Log.d("ESP32", "Connesso a $ip:$port")
                onConnected?.invoke()
            } catch (e: Exception) {
                connected.set(false)
                Log.e("ESP32", "Errore connessione: ${e.message}", e)

                // Tentativo di riconnessione
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++
                    Log.d("ESP32", "Tentativo di riconnessione $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")

                    uiHandler.postDelayed({
                        connect(onConnected, onError)
                    }, RECONNECT_DELAY_MS)
                } else {
                    onError?.invoke(e)
                }
            }
        }
    }

    fun checkConnectionAndReconnect() {
        io.execute {
            if (!connected.get() || socket?.isConnected != true) {
                Log.d("ESP32", "Connessione persa, tentativo di riconnessione...")
                connect(
                    onConnected = {
                        Log.d("ESP32", "Riconnessione riuscita")
                    },
                    onError = { e ->
                        Log.e("ESP32", "Riconnessione fallita: ${e.message}")
                    }
                )
            }
        }
    }

    fun sendCommand(throttle: Int, steer: Int) {
        // clamp per sicurezza
        val t = throttle.coerceIn(-255, 255)
        val r = steer.coerceIn(-255, 255)

        io.execute {
            try {
                if (!connected.get() || writer == null) return@execute
                writer!!.write("T:$t;R:$r\n")
                writer!!.flush()
            } catch (e: Exception) {
                connected.set(false)
                Log.e("ESP32", "Errore invio: ${e.message}", e)
            }
        }
    }

    fun disconnect() {
        io.execute {
            try {
                connected.set(false)
                writer?.close()
                socket?.close()
                writer = null
                socket = null
            } catch (_: Exception) { }
        }
    }
}