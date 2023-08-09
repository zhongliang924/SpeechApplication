package com.example.speechapplication

interface WebSocketCallback {
    fun onDataReceived(data: String)
}