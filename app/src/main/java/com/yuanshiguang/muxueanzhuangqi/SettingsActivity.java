package com.yuanshiguang.muxueanzhuangqi;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("设置");
        }

        RadioGroup installMethodGroup = findViewById(R.id.install_method_group);
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int savedMethod = prefs.getInt("install_method", 0);
        installMethodGroup.check(
            savedMethod == 0 ? R.id.radio_system :
            savedMethod == 1 ? R.id.radio_shizuku :
            R.id.radio_root
        );
        installMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int method = checkedId == R.id.radio_system ? 0 :
                         checkedId == R.id.radio_shizuku ? 1 : 2;
            prefs.edit().putInt("install_method", method).apply();
        });
        RadioGroup themeModeGroup = findViewById(R.id.theme_mode_group);
        // 删除下面这行重复的prefs定义
        // SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int savedTheme = prefs.getInt("theme_mode", 0);
        themeModeGroup.check(
            savedTheme == 0 ? R.id.radio_theme_system :
            savedTheme == 1 ? R.id.radio_theme_light :
            R.id.radio_theme_dark
        );
        themeModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = checkedId == R.id.radio_theme_system ? 0 :
                       checkedId == R.id.radio_theme_light ? 1 : 2;
            prefs.edit().putInt("theme_mode", mode).apply();
            // 立即切换主题
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
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}