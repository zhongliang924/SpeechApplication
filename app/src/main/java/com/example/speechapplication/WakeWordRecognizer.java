package com.example.speechapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class WakeWordRecognizer implements RecognitionListener, WebSocketCallback {
    private static final String WAKEWORD_SEARCH = "WAKEWORD_SEARCH";
    private static final String LOG_TAG = ListeningActivity.class.getName();
    private final Context context;
    private final Vibrator vibrator;
    private final AppCompatActivity activity;
    private SpeechRecognizer recognizer;

    private int sensibility = 80;

    public WakeWordRecognizer(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * 初始化并启动语音唤醒器
     * 通过添加监听器处理语音输入事件
     * 灵敏度决定了唤醒词被激活的难易程度
     * addKeyphraseSearch() 方法添加了一个关键词搜索，用于检测用户是否说出了唤醒词
     */
    private void setup() {
            try {
                final Assets assets = new Assets(context);
                final File assetDir = assets.syncAssets();

                Log.i(LOG_TAG, "Changing Recognition Threshold to " + sensibility);
                // 设置音频模型文件
                // 设置字典文件
                // 设置唤醒词灵敏度值
                recognizer = SpeechRecognizerSetup.defaultSetup()
                        .setAcousticModel(new File(assetDir, "models/zh-cn-ptm"))   // 设置音频模型文件
                        .setDictionary(new File(assetDir, "models/lm/words.dic"))   // 设置字典文件
                        .setKeywordThreshold(Float.parseFloat("1.e-" + 4 * sensibility))    // 设置唤醒词灵敏度值
                        .getRecognizer();   // 获取 SpeechRecognizer 对象的引用
                recognizer.addKeyphraseSearch(WAKEWORD_SEARCH, context.getString(R.string.wake_word)); // 添加关键词搜索
                recognizer.addListener(this);
                recognizer.startListening(WAKEWORD_SEARCH);

                Log.d(LOG_TAG, "... listening");

            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
    }

    public void onResume() {
        setup(); // 重新启动监听
        Log.d(LOG_TAG, "... listening");
    }

    public void onPause() {
        if (recognizer != null) {
            recognizer.removeListener(this);
            recognizer.stop();
            recognizer.shutdown();
        }
    }

    public void setSensibility(int newSensibility) {
        sensibility = newSensibility;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG, "Beginning Of Speech");
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle("~ ~ ~");
        }
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(LOG_TAG, "End Of Speech");
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle("");
        }
    }

    /**
     * 接收部分语音输入结果后，检查结果是否包含正确的唤醒词
     * 如果是，则触发设备震动、清除 ActionBar 的短消息，并启动 MainActivity
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            final String text = hypothesis.getHypstr();
            Log.d(LOG_TAG, "on partial: " + text);
            // 如果检测到唤醒词
            if (text.equals(context.getString(R.string.wake_word))) {
                vibrator.vibrate(100);   // 使用 Vibrator API 给设备震动 100ms
                if (activity.getSupportActionBar() != null) {
                    // 如果当前 Activity 存在 ActionBar，将副标题设置为空字符串
                    activity.getSupportActionBar().setSubtitle("");
                }
                if (activity.getClass() == ListeningActivity.class) {
                    // 使用 Activity 上下文启动 MainActivity
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                } else if (activity.getClass() == MainActivity.class) {
                    onPause();
                    SpeechRecognition recognition = new SpeechRecognition(activity, this);
                    recognition.startRecord();
                    recognition.stopRecord();

                    onResume();
                }
            }
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            Log.d(LOG_TAG, "on Result: " + hypothesis.getHypstr() + " : " + hypothesis.getBestScore());
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setSubtitle("");
            }
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(LOG_TAG, "on Error: " + e);
    }

    @Override
    public void onTimeout() {
        Log.d(LOG_TAG, "on Timeout");
    }

    @Override
    public void onDataReceived(@NonNull String data) {
        if (data.contains("返回")) {
            activity.finish();
        } else if(data.contains("清空")) {
            MainActivity.instance.clearLog();
        }
    }
}
