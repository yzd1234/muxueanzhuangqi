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
import android.content.pm.PackageManager; // 添加这个
import androidx.core.app.ActivityCompat; // 添加这个
import androidx.annotation.NonNull; // 添加这个
import android.util.Log; // <-- 把这个加上哦~
import rikka.shizuku.Shizuku; // 添加这个
import androidx.core.content.FileProvider; // 添加这个 import
import java.io.File; // 添加这个 import
import com.yuanshiguang.muxueanzhuangqi.Api;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_APK = 1;
    private static final int SHIZUKU_REQUEST_PERMISSION_CODE = 123; // 确保这个常量是public的
    private EditText inputBox;
    private SharedPreferences prefs; // 把 SharedPreferences 移到成员变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 读取主题设置并应用
        prefs = getSharedPreferences("settings", MODE_PRIVATE); // 在 onCreate 初始化
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
        // 选择 APK 按钮的逻辑 (可以考虑用更现代的 Activity Result API)
        btnChooseApk.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.android.package-archive");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(
                        Intent.createChooser(intent, "选择 APK 文件"),
                        REQUEST_CODE_PICK_APK);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnInstall = findViewById(R.id.btn_install);
        btnInstall.setOnClickListener(v -> {
            // 直接使用成员变量 prefs
            int method = prefs.getInt("install_method", 0);
            String apkPath = inputBox.getText().toString();

            // 简单的路径检查
            if (apkPath.isEmpty()) {
                Toast.makeText(this, "请先选择或输入 APK 文件路径", Toast.LENGTH_SHORT).show();
                return;
            }
            File apkFile = new File(apkPath);
             if (!apkFile.exists() || !apkFile.isFile()) {
                 Toast.makeText(this, "无效的 APK 文件路径或文件不存在", Toast.LENGTH_SHORT).show();
                 return;
            }


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
            // 可以在这里检查权限，或者由 Handler 内部检查
            // handler.requestPermission(this); // 如果需要先请求权限
            handler.install(this, apkPath);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_APK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 从 Content URI 获取真实文件路径 (这部分逻辑比较复杂且易错，需要适配不同 Android 版本和 Provider)
                // 简单的 getPath() 通常不可靠，这里暂时保留，但实际应用需要更健壮的处理
                // 例如：复制文件到应用缓存目录再使用路径
                String path = getPathFromUri(uri); // 尝试获取路径
                if (path != null) {
                    inputBox.setText(path);
                } else {
                    Toast.makeText(this, "无法获取文件路径，请尝试手动输入", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 尝试从 URI 获取文件路径 (这是一个简化示例，可能不适用于所有情况)
    private String getPathFromUri(Uri uri) {
        String path = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        String displayName = cursor.getString(index);
                        // 尝试复制到缓存目录获取路径
                        File cacheDir = getCacheDir();
                        File tempFile = new File(cacheDir, displayName);
                        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[4 * 1024]; // 4K buffer
                            int read;
                            while ((read = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, read);
                            }
                            outputStream.flush();
                            path = tempFile.getAbsolutePath();
                        } catch (Exception e) {
                            // 复制失败，尝试其他方法或返回 null
                            Log.e("MainActivity", "Error copying file from URI", e);
                        }
                    }
                }
            } catch (Exception e) {
                 Log.e("MainActivity", "Error getting path from content URI", e);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
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

    // 把这个方法移到 MainActivity 类里面！
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SHIZUKU_REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku 权限已授予", Toast.LENGTH_SHORT).show();
                // 可以在这里提示用户再次点击安装按钮
            } else {
                Toast.makeText(this, "Shizuku 权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
        // 如果有其他权限请求，可以在这里添加 else if
    }
}

// 定义接口
interface InstallMethodHandler {
    void install(AppCompatActivity activity, String apkPath);
    void requestPermission(AppCompatActivity activity);
}

// 公共工具类
class InstallUtils {
    static boolean checkApkFile(AppCompatActivity activity, String apkPath) {
        if (apkPath == null || apkPath.isEmpty()) {
            Toast.makeText(activity, "APK 路径不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        java.io.File apkFile = new java.io.File(apkPath);
        if (!apkFile.exists() || !apkFile.isFile()) {
            Toast.makeText(activity, "无效的 APK 文件路径", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    static void showInstallResult(AppCompatActivity activity, int result, String output, String error, String handlerName) {
        try {
            activity.runOnUiThread(() -> {
                try {
                    if (result == 0 && (output == null || output.toLowerCase().contains("success"))) {
                        Toast.makeText(activity, handlerName + " 安装成功", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = handlerName + " 安装失败";
                        if (error != null && !error.isEmpty()) {
                            message += "，错误信息:\n" + error;
                        } else if (output != null && !output.isEmpty()) {
                            message += "，输出信息:\n" + output;
                        } else {
                            message += "，代码：" + result;
                        }
                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e("InstallUtils", "UI thread error", e);
                }
            });
        } catch (Exception e) {
            Log.e("InstallUtils", "Failed to post to UI thread", e);
        }
    }
}

// 系统安装方式
class SystemInstallHandler implements InstallMethodHandler {
    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
             Toast.makeText(activity, "APK 文件不存在", Toast.LENGTH_SHORT).show();
             return;
        }
        // 使用 FileProvider 获取安全的 URI
        Uri apkUri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(activity, "com.yuanshiguang.muxueanzhuangqi.fileprovider", apkFile);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 需要这个 flag，特别是配合 FileProvider
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
             Toast.makeText(activity, "无法启动安装程序: " + e.getMessage(), Toast.LENGTH_LONG).show();
             Log.e("SystemInstall", "Error starting install intent", e);
        }
    }
    @Override
    public void requestPermission(AppCompatActivity activity) {
        // 系统安装通常需要 "REQUEST_INSTALL_PACKAGES" 权限 (Android O+)
        // 但这个权限通常在用户点击安装时由系统处理，应用层面一般无需主动请求
        // 可以在 AndroidManifest.xml 中声明 <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    }
}

// Shizuku 安装方式（需集成 Shizuku SDK）
class ShizukuInstallHandler implements InstallMethodHandler {

    private boolean checkShizukuPermission(AppCompatActivity activity) {
        if (Shizuku.isPreV11()) {
            // Shizuku V11 之前的版本不支持运行时权限
            Toast.makeText(activity, "请升级 Shizuku 到最新版本", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // 用户拒绝过权限，显示更详细的解释
            Toast.makeText(activity, "需要 Shizuku 权限才能安装应用\n请在 Shizuku 应用中授予权限", Toast.LENGTH_LONG).show();
            // 可以添加引导用户去Shizuku应用的逻辑
            return false;
        } else {
            // 请求权限
            requestPermission(activity);
            return false;
        }
    }

    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        if (!InstallUtils.checkApkFile(activity, apkPath)) {
            return;
        }

        // 检查 Shizuku 是否在运行以及是否已授权
        if (!Shizuku.pingBinder()) {
            Toast.makeText(activity, "Shizuku 服务未运行", Toast.LENGTH_SHORT).show();
            // 可以引导用户去 Shizuku 应用启动服务
            return;
        }
        if (!checkShizukuPermission(activity)) {
             // 如果没有权限，checkShizukuPermission 内部会处理请求或提示
            return;
        }

        // 使用 Shizuku 安装 (同样建议后台线程)
        new Thread(() -> {
            Process process = null;
            java.io.BufferedReader stdoutReader = null;
            java.io.BufferedReader stderrReader = null;
            int result = -1;
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            try {
                // 注意：直接拼接字符串可能存在注入风险
                String[] cmd = new String[]{"pm", "install", "-r", apkPath};
                // 使用 Shizuku API 执行命令
                // 使用Shizuku提供的公开API创建进程
                process = Shizuku.Process.create(cmd);
                
                // 读取标准输出和错误输出
                stdoutReader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                stderrReader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = stderrReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            
                result = process.waitFor();
            } catch (Throwable e) { // 捕获 Throwable 以包含 Shizuku 可能抛出的错误
                error.append("Shizuku 执行异常: ").append(e.getMessage());
                result = -1;
            } finally {
                 try {
                    if (stdoutReader != null) stdoutReader.close();
                    if (stderrReader != null) stderrReader.close();
                    if (process != null) process.destroy();
                } catch (Exception ignored) {}
            
                final int finalResult = result;
                final String finalOutput = output.toString().trim();
                final String finalError = error.toString().trim();
            
                InstallUtils.showInstallResult(activity, finalResult, finalOutput, finalError, "Shizuku");
            }
        }).start();
    }
    @Override
    public void requestPermission(AppCompatActivity activity) {
        if (Shizuku.isPreV11()) {
             Toast.makeText(activity, "当前 Shizuku 版本过低，不支持运行时权限", Toast.LENGTH_SHORT).show();
             return;
        }
        
        // 添加权限请求回调
        Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == MainActivity.SHIZUKU_REQUEST_PERMISSION_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(activity, "Shizuku 权限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Shizuku 权限被拒绝", Toast.LENGTH_SHORT).show();
                        // 可以在这里添加引导用户去设置页面的逻辑
                    }
                }
            }
        });
        
        // 使用正确的requestPermission方法
        Shizuku.requestPermission(MainActivity.SHIZUKU_REQUEST_PERMISSION_CODE);
    }
} // ← 这里只需要一个结束 ShizukuInstallHandler 的大括号，不要多写！

// Root 安装方式
class RootInstallHandler implements InstallMethodHandler {
    @Override
    public void install(AppCompatActivity activity, String apkPath) {
        if (!InstallUtils.checkApkFile(activity, apkPath)) {
            return;
        }

        // 尝试执行 Root 安装
        new Thread(() -> { // 在后台线程执行耗时操作
            Process process = null;
            java.io.DataOutputStream os = null;
            java.io.BufferedReader errorReader = null;
            int result = -1;
            StringBuilder errorMsg = new StringBuilder();
        
            try {
                // 使用更安全的命令执行方式
                String[] commands = {
                    "pm",
                    "install",
                    "-r",
                    apkPath
                };
                process = Runtime.getRuntime().exec(commands);
                os = new java.io.DataOutputStream(process.getOutputStream());
                os.writeBytes("exit\n");
                os.flush();
        
                // 读取错误流
                errorReader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorMsg.append(line).append("\n");
                }
        
                result = process.waitFor(); // 等待命令执行完成
        
            } catch (Exception e) {
                errorMsg.append("执行异常: ").append(e.getMessage());
                result = -1; // 标记为异常
            } finally {
                try {
                    if (os != null) os.close();
                    if (errorReader != null) errorReader.close();
                    if (process != null) process.destroy();
                } catch (Exception ignored) {}
        
                // 在 UI 线程更新 Toast
                final int finalResult = result;
                final String finalErrorMsg = errorMsg.toString().trim();
                InstallUtils.showInstallResult(activity, finalResult, null, finalErrorMsg, "Root");
            }
        }).start();
    }

    @Override
    public void requestPermission(AppCompatActivity activity) {
        // 使用更安全的Root权限检查方式
        new Thread(() -> {
            boolean hasRoot = false;
            try {
                // 检查su二进制文件是否存在
                File suFile = new File("/system/bin/su");
                if (!suFile.exists()) {
                    suFile = new File("/system/xbin/su");
                }
                hasRoot = suFile.exists() && suFile.canExecute();
            } catch (Exception e) {
                hasRoot = false;
            }

            final boolean finalHasRoot = hasRoot;
            activity.runOnUiThread(() -> {
                if (finalHasRoot) {
                    Toast.makeText(activity, "设备已Root", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "设备未Root或权限不足", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
} // ← 这里只需要一个结束 RootInstallHandler 的大括号，不要多写！
}