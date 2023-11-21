package com.example.taxi.socket

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class SocketRepository constructor(
    val context: Context,
    private var viewModelScope: CoroutineScope? = null,
    private var socketMessageProcessor: SocketMessageProcessor? = null
) {

    private var webSocket: WebSocketClient? = null
    private var shouldReconnect = true

    private var _isConnected: Boolean = false
    val socketLive = MutableLiveData<Boolean>().apply {
        value = false
    }
    var isConnected: Boolean
        get() = _isConnected
        set(value) {
            if (_isConnected != value) {
                _isConnected = value

                sendSocketStatusBroadcast()

//                updateViewColor()
            }
        }
    var reconnectJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())


    private fun sendSocketStatusBroadcast() {
        val intent = Intent("SOCKET_STATUS")
        intent.putExtra("IS_CONNECTED", isConnected)
        context.sendBroadcast(intent)
    }


    fun initSocket(token: String) {
        if (reconnectJob?.isActive == true || isConnected) {
            return
        }

        reconnectJob = viewModelScope?.launch {
            connectSocket(token)
        }
    }

    private fun connectSocket(token: String) {
        shouldReconnect = true
//        webSocket = object : WebSocketClient(URI("wss://aliftaxi.uz/connect/?token=$token")) {
//        webSocket = object : WebSocketClient(URI("wss://my.xamkortaxi.uz/connect/?token=$token")) {
        webSocket = object : WebSocketClient(URI("wss://lidertaxi.uz/connect/?token=$token")) {
//        webSocket = object : WebSocketClient(URI("wss://wintaxi.uz/connect/?token=$token")) {
//        webSocket = object : WebSocketClient(URI("wss://my.likestar.uz/connect/?token=$token")) {
//        webSocket = object : WebSocketClient(URI("wss://my.mrtaxi.uz/connect/?token=$token")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
//                isConnectedSocket.value = true
                socketLive.postValue(true)
                isConnected = true
                Log.d("WebSocket", "Connection onOpen: ")

            }

            override fun onMessage(message: String?) {
                Log.d("WebSocket", "onMessage: $message")
                message?.let {
                    socketMessageProcessor?.handleMessage(it)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "Connection closed: $reason ($code)")
                isConnected = false
                socketLive.postValue(false)

//                isConnectedSocket.value = false
                if (shouldReconnect) {
                    reconnectSocket(token)
                }
//                reconnectSocket(token)
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "Error occurred", ex)
                isConnected = false
                socketLive.postValue(false)
//                isConnectedSocket.postValue(false)
                if (shouldReconnect) {
                    reconnectSocket(token)
                }

            }
        }

        webSocket?.connect()
    }

    private fun reconnectSocket(token: String) {
        reconnectJob?.cancel()
        if (reconnectJob?.isActive != true || !isConnected) {
            reconnectJob = viewModelScope?.launch {
                delay(SocketConfig.RECONNECT_DELAY_MS)
                connectSocket(token)
            }
        }
    }


    private fun updateViewColor() {
        viewModelScope?.launch() {

//            socketViewModel?.setConnected(isConnected)
        }
    }

    fun disconnectSocket() {
        shouldReconnect = false
        handler.removeCallbacksAndMessages(null)
        reconnectJob?.cancel()
        webSocket?.close()

    }
}