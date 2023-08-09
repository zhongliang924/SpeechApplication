package com.example.speechapplication;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.VIBRATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * ListeningActivity 实现了 Pocket Sphinx 的 RecognitionListener，并仅在识别唤醒词之后才在 MainActivity 上进行后续的操作
 * 当唤醒词从资源文件中读取时，要更改它，还需要在 './assets/sync/models/lm/words.dic' 中添加一个新的唤醒词
 * 不要忘了在修改后为 dictionary 生成一个新的 MD5 hash（<a href="http://passwordsgenerator.net/md5-hash-generator/">...</a>）
 */
public class ListeningActivity extends AppCompatActivity {

    private static final String LOG_TAG = ListeningActivity.class.getName();
    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private WakeWordRecognizer wakeWordRecognizer;
    private int sensibility = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);    // 将当前 Activity 对应的布局文件设置为 activity_listening.xml

        // 找到布局控件（灵敏度和灵敏度条）实例化，SeekBar 的初始进度为 sensibility 变量的值
        final TextView threshold = findViewById(R.id.threshold);
        threshold.setText(String.valueOf(sensibility));
        final SeekBar seekbar = findViewById(R.id.seekbar);
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
                wakeWordRecognizer.setSensibility(sensibility);
                onPause();
                onResume();
            }
        });

        wakeWordRecognizer = new WakeWordRecognizer(this);
        // 用于请求应用程序需要的一些权限，以便能够正常使用语音唤醒和振动反馈
        ActivityCompat.requestPermissions(ListeningActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, VIBRATE}, PERMISSIONS_REQUEST_CODE);
    }

    /**
     * 用于处理用户对应用程序请求的权限的授权结果
     * 如果用户拒绝了音频记录权限，则显示一条 Toast 消息提醒用户，并结束当前 Activity
     */
    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);   // 确保继承自父类将收到有关权限请求结果的通知
        // 检查权限请求码
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // 处理请求结果
            if (0 < grantResults.length && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // 用户做出了授权决定且拒绝授权
                Toast.makeText(this, "Audio recording permissions denied.", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wakeWordRecognizer != null){
            wakeWordRecognizer.onResume(); // 重新启动识别器
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wakeWordRecognizer != null){
            wakeWordRecognizer.onPause(); // 停止识别器
        }
    }

}



