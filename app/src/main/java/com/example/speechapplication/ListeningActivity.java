package com.example.speechapplication;

import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private WakeWordRecognizer wakeWordRecognizer;
    /**
     * 主要功能是设置语音唤醒的灵敏度
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);    // 将当前 Activity 对应的布局文件设置为 activity_listening.xml
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



