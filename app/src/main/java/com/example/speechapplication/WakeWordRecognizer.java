package com.example.speechapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class WakeWordRecognizer implements RecognitionListener {

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
        // onResume();   // 初始化启动语音识别唤醒器

        // 找到布局控件（灵敏度和灵敏度条）实例化，SeekBar 的初始进度为 sensibility 变量的值
        final TextView threshold = activity.findViewById(R.id.threshold);
        threshold.setText(String.valueOf(sensibility));
        final SeekBar seekbar = activity.findViewById(R.id.seekbar);
        seekbar.setProgress(sensibility);
        /*
         * 首先为 SeekBar 添加一个进度改变监听器，当拖动 seekBar 时，该监听器可以根据 SeekBar 的进度更新灵敏度的值
         * 同时为 SeekBar 设置了两个触摸事件监听器，但未具体实现
         * 当停止滑动 SeekBar 时，执行 onStopTrackingTouch() 方法，获得 seekBar 最终进度，重新启动语音唤醒监听器
         * 这段代码用于设置语音识别的灵敏度，并允许用户通过 SeekBar 调整，实现了 SeekBar 相关监听器，可以更新语音唤醒相应设置
         */
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold.setText(String.valueOf(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // intentionally empty
            }

            public void onStopTrackingTouch(final SeekBar seekBar) {
                sensibility = seekBar.getProgress();
                Log.i(LOG_TAG, "Changing Recognition Threshold to " + sensibility);
                threshold.setText(String.valueOf(sensibility));
                recognizer.removeListener(WakeWordRecognizer.this);
                recognizer.stop();
                recognizer.shutdown();
                setup();
            }
        });
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
            // 设置音频模型文件
            // 设置字典文件
            // 设置唤醒词灵敏度值
            recognizer = SpeechRecognizerSetup.defaultSetup()
                    .setAcousticModel(new File(assetDir, "models/zh-cn-ptm"))   // 设置音频模型文件
                    .setDictionary(new File(assetDir, "models/lm/words.dic"))   // 设置字典文件
                    .setKeywordThreshold(Float.parseFloat("1.e-" + 2 * sensibility))    // 设置唤醒词灵敏度值
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
                // 使用 Activity 上下文启动 MainActivity
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
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
}
