package com.example.speechapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), WebSocketCallback {

    companion object {
        const val MAX_LOG_LINE_NUM = 200
        var currLogLineNum = 0
        var strLog: String? = null
    }

    private lateinit var speechRecognition: SpeechRecognition

    private lateinit var btn: Button
    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var btnClear: Button
    private lateinit var startPlayer: MediaPlayer
    private lateinit var stopPlayer: MediaPlayer

    private lateinit var log: TextView
    private lateinit var logView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUI()

        speechRecognition.startRecord()
        speechRecognition.stopRecord()
    }

    private fun setUI() {
        speechRecognition = SpeechRecognition(this, this)
        // 设置 MediaPlayer
        startPlayer = MediaPlayer.create(this, R.raw.ding)
        stopPlayer = MediaPlayer.create(this, R.raw.dong)


        // 设置 button 监听器
        btn = findViewById(R.id.button)
        btn.setOnClickListener { finish() }

        log = findViewById(R.id.log)
        logView = findViewById(R.id.logView)

        speechRecognition.initialLog(log, logView)
        speechRecognition.updateLog("----> 启动语音识别", "green")

        btnRecord = findViewById(R.id.start)
        btnRecord.setOnClickListener {
            // 触发开始录音事件
            startPlayer.start()
            Thread.sleep(1500) // 这个延时是等待提示音播完
            speechRecognition.startRecord()
        }

        btnStop = findViewById(R.id.stop)
        btnStop.setOnClickListener {
            // 触发停止录音事件
            speechRecognition.stopRecord()
            stopPlayer.start()
        }

        btnClear = findViewById(R.id.btn_clear)
        btnClear.setOnClickListener {
            clearLog()
        }
    }


    /**
     * 清除日志区的消息
     */
    private fun clearLog() {
        log.post {
            strLog = ""
            currLogLineNum = 0
            log.text = ""
        }
        logView.post {
            logView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDataReceived(data: String) {
        if (data.contains("返回")) {
            finish()
        } else if(data.contains("清除")) {
            clearLog()
        }
    }


}

