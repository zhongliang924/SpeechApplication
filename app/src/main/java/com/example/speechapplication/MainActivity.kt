package com.example.speechapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat

class MainActivity : AppCompatActivity(), WebSocketCallback {
    companion object {
        const val MAX_LOG_LINE_NUM = 200
        var currLogLineNum = 0
        var strLog: String? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MainActivity
    }

    private lateinit var speechRecognition: SpeechRecognition

    private lateinit var btn: Button
    private lateinit var btnClear: Button
    private lateinit var log: TextView
    private lateinit var logView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUI()
        instance = this
        speechRecognition.startRecord()
        speechRecognition.stopRecord()

    }

    private fun setUI() {
        speechRecognition = SpeechRecognition(this, this)
        // 设置 button 监听器
        btn = findViewById(R.id.button)
        btn.setOnClickListener { finish() }

        btnClear = findViewById(R.id.btn_clear)
        btnClear.setOnClickListener {
            clearLog()
        }

        log = findViewById(R.id.log)
        logView = findViewById(R.id.logView)

        updateLog("----> 启动语音识别", "green")
    }

    /**
     * 在应用程序运行时记录和显示各种调试和状态信息
     * text 参数表示需要显示的文本内容， color 参数表示该文本颜色
     */
    fun updateLog(text: String, color: String) {
        log.post{
            if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                val st = strLog!!.indexOf("<br>")
                strLog = strLog!!.substring(st + 4)
            } else {
                currLogLineNum++
            }
            val str = "<font color=\"$color\">$text</font><br>"
            strLog = strLog?.let { if (it.isEmpty()) str else it + str } ?: str
            log.text = HtmlCompat.fromHtml(strLog!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        logView.post {
            logView.fullScroll(ScrollView.FOCUS_DOWN)
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
        } else if(data.contains("清空")) {
            clearLog()
        }
        Thread.sleep(3000)
        val intent = Intent(this, ListeningActivity::class.java)
        this.startActivity(intent)
    }

}

