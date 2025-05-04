package com.yuanshiguang.muxueanzhuangqi;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_APK = 1;
    private EditText inputBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 读取主题设置并应用
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", 0);
        switch (mode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputBox = findViewById(R.id.input_box);
        Button btnChooseApk = findViewById(R.id.btn_choose_apk);
        Button btnInstall = findViewById(R.id.btn_install);
        btnInstall.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            int method = prefs.getInt("install_method", 0);
            String apkPath = inputBox.getText().toString();
            InstallMethodHandler handler;
            switch (method) {
                case 1:
                    handler = new ShizukuInstallHandler();
                    break;
                case 2:
                    handler = new RootInstallHandler();
                    break;
                default:
                    handler = new SystemInstallHandler();
            }
            handler.install(this, apkPath);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_APK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                inputBox.setText(uri.getPath());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// 定义接口
interface InstallMethodHandler {
    void install(AppCompatActivity activity, String apkPath);
    void requestPermission(AppCompatActivity activity);
}

// 系统安装方式
class SystemInstallHandler implements InstallMethodHandler {
    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(apkPath), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
    @Override
    public void requestPermission(AppCompatActivity activity) {
        // 系统安装无需特殊权限
    }
}

// Shizuku 安装方式（需集成 Shizuku SDK）
class ShizukuInstallHandler implements InstallMethodHandler {
    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        try {
            // 检查 Shizuku 权限
            if (!rikka.shizuku.Shizuku.pingBinder() ||
                rikka.shizuku.Shizuku.checkSelfPermission() != activity.getPackageManager().PERMISSION_GRANTED) {
                Toast.makeText(activity, "Shizuku 未连接或未授权", Toast.LENGTH_SHORT).show();
                return;
            }
            // 通过 Shizuku 执行 pm install
            String[] cmd = new String[]{"pm", "install", "-r", apkPath};
            Process process = rikka.shizuku.Shizuku.newProcess(cmd, null, null);
            int result = process.waitFor();
            if (result == 0) {
                Toast.makeText(activity, "Shizuku 安装成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Shizuku 安装失败，代码：" + result, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Shizuku 安装异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void requestPermission(AppCompatActivity activity) {
        Toast.makeText(activity, "Shizuku 权限请求接口占位", Toast.LENGTH_SHORT).show();
        // 这里应调用 Shizuku SDK 的权限请求方法
    }
}

// Root 安装方式
class RootInstallHandler implements InstallMethodHandler {
    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            os.writeBytes("pm install -r \"" + apkPath + "\"\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = process.waitFor();
            if (result == 0) {
                Toast.makeText(activity, "Root 安装成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Root 安装失败，代码：" + result, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Root 安装异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void requestPermission(AppCompatActivity activity) {
        Toast.makeText(activity, "Root 权限请求接口占位", Toast.LENGTH_SHORT).show();
        // 这里应实现 root 权限检测与请求
    }
}