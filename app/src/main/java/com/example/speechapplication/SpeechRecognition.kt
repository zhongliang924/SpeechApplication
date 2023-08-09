package com.example.speechapplication

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import kotlin.math.sqrt


class SpeechRecognition(private val activity: Activity, private val callback: WebSocketCallback) {

    companion object {
        private const val TAG = "MainActivity"
        private const val WS_URL = "ws://192.168.43.46:8080/ws"     // WebSocket 服务器地址
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO     // 录音通道设置
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT    // 录音格式
        private const val SAMPLE_RATE = 16000 // 采样率
        private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2
    }

    private var audioRecord: AudioRecord? = null
    private var webSocket: WebSocket? = null

    private var isRecording = true

    private lateinit var log: TextView
    private lateinit var logView: ScrollView

    /**
     * 开始录音
     */
    fun startRecord() {
        updateLog("----> 开始录音", "green")
        Log.d(TAG, "StartRecording:")
        // 检查是否有录音权限，如果没有直接返回一个空值
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize)
        connectWebSocket()
        readThread.start()
    }

    /**
     * 停止录音
     */
    fun stopRecord() {
        stopRecordThread.start()
    }

    /**
     * 连接 WebSocket 服务器
     */
    private fun connectWebSocket() {
        val request = Request.Builder().url(WS_URL).build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket Info", "onOpen")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket Info", "OnMessage: $text")
                // 接收服务端传来的结果
                val obj = JSONObject(text)  // 将 JSON 字符串转换为对象
                when (obj.getString("type")) {
                    // 处理不同类型的消息
                    "result" -> {
                        // 接收来自服务端的推理结果
                        val data = obj.getString("data")
                        updateLog(data, "red")
                        callback.onDataReceived(data)
                    }
                    "status" -> {
                        // 接收来自服务端的状态信息
                        val status = obj.getString("status")
                        if (status == "Triton 服务器未启动"){
                            updateLog("----> Triton 服务器未启动", "green")
                            disconnectWebSocket()
                        } else {
                            updateLog("----> $status", "green")
                        }
                    }
                    "stop" -> {
                        // 接收断开连接的消息
                        disconnectWebSocket()
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket Info", "onClosing")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket Info", "onClosed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                updateLog("----> 未连接到 Triton 服务器，请检查设置", "red")
                Log.e("WebSocket Error", t.message ?: "")
            }
        })
    }

    /**
     * 断开 WebSocket 服务器
     */
    private fun disconnectWebSocket() {
        webSocket?.close(1000, "用户主动关闭")
    }

    fun initialLog(mLog: TextView, mLogView: ScrollView) {
        log = mLog
        logView = mLogView
    }

    /**
     * 开启录音线程，并将录音数据通过 WebSocket 发送给服务端
     */
    private val readThread = Thread {
        val mindB = 1000
        val delayTime = 1.0
        val buffer = ByteArray(minBufferSize)

        var flag = false
        var tempnum = 0
        var tempnum2 = 0
        var status = false

        audioRecord?.startRecording()
        while (isRecording) {
            val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            val volume = calculateVolume(buffer, readBytes)
            if (volume > mindB && !flag) {
                flag = true
                updateLog("到达录音开始点，开始录音", "blue")
                tempnum2 = tempnum
            }
            if (flag) {
                if (volume < mindB && !status) {
                    status = true
                    tempnum2 = tempnum
                    Log.d(TAG, "声音小了，且录音已开始，当前是记录开始点")
                }
                if (volume > mindB) {
                    status = false
                    tempnum2 = tempnum
                }
                if (tempnum > tempnum2+delayTime*15 && status) {
                    Log.d(TAG, "间隔`$delayTime`秒后开始检测是否还是小声")
                    updateLog("间隔 $delayTime 秒后检测还是小声", "blue")
                    if (volume < mindB) {
                        isRecording = false
                        Log.d(TAG, "还是小声，结束录音")
                        updateLog("结束录音", "blue")
                    } else {
                        status = false
                        Log.d(TAG, "大声，结束录音")
                    }
                }
            }
            tempnum += 1

            if (readBytes > 0 && webSocket != null && isRecording) {
                // 发送录音数据到 WebSocket 服务器
                webSocket?.let {
                    if (!it.send(buffer.toByteString(0, readBytes))){
                        Log.w("WebSocket Info", "send failed")
                    }
                }
            }
        }
    }


    private val stopRecordThread = Thread {
        while (true) {
            if (!isRecording) {
                updateLog("----> 停止录音线程", "green")
                // 等待读取线程完成执行，确保所有数据都已被写入并读取完毕
                try {
                    readThread.join(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                // 告诉服务端完成录音
                webSocket?.send("Recording finished") // 录音完成，发送消息给 webSocket
                // 停止录音并释放相关资源
                audioRecord?.stop()
                audioRecord?.release()
                break
            }
        }
    }


    /**
     * 在应用程序运行时记录和显示各种调试和状态信息
     * text 参数表示需要显示的文本内容， color 参数表示该文本颜色
     */
    fun updateLog(text: String, color: String) {
        log.post{
            if (MainActivity.currLogLineNum >= MainActivity.MAX_LOG_LINE_NUM) {
                val st = MainActivity.strLog!!.indexOf("<br>")
                MainActivity.strLog = MainActivity.strLog!!.substring(st + 4)
            } else {
                MainActivity.currLogLineNum++
            }
            val str = "<font color=\"$color\">$text</font><br>"
            MainActivity.strLog = MainActivity.strLog?.let { if (it.isEmpty()) str else it + str } ?: str
            log.text = HtmlCompat.fromHtml(MainActivity.strLog!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        logView.post {
            logView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * 计算音量大小
     */
    private fun calculateVolume(buffer: ByteArray, bufferSize: Int): Double {
        var sum = 0.0
        for (i in 0 until bufferSize step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            sum += sample * sample.toDouble()
        }

        return sqrt(sum / (bufferSize / 2))
    }
}