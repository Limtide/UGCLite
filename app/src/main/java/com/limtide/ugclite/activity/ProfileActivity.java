package com.limtide.ugclite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limtide.ugclite.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        // ViewBinding已经自动绑定了所有视图组件
        // 无需手动findViewById
    }

    private void setupClickListeners() {
        // 返回按钮点击事件
        binding.backArrow.setOnClickListener(v -> {
            Log.d(TAG, "返回按钮被点击");
            finish(); // 关闭当前Activity，返回上一个页面
        });

        // 获取传递的数据并显示
        displayUserInfo();

        // 这里可以添加其他点击事件
        setupProfileOptionClickListeners();
    }

    private void displayUserInfo() {
        // 获取传递的数据
        Intent intent = getIntent();
        if (intent != null) {
            String username = intent.getStringExtra("username");
            String nickname = intent.getStringExtra("nickname");
            String avatarUrl = intent.getStringExtra("avatarUrl");
            String signature = intent.getStringExtra("signature");

            Log.d(TAG, "接收到的数据 - username: " + username + ", nickname: " + nickname);
            Log.d(TAG, "接收到的数据 - avatarUrl: " + avatarUrl + ", signature: " + signature);

            // 显示用户信息
            binding.profileTitle.setText(nickname != null ? nickname : username);
            binding.profileSubtitle.setText(signature != null ? signature : "这个人很懒，什么都没留下");

            // 加载头像
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // 这里可以添加图片加载库（如Glide）来加载头像
                // Glide.with(this).load(avatarUrl).into(binding.backArrow);
            }
        }
    }

    private void setupProfileOptionClickListeners() {
        // 使用ViewBinding直接访问视图组件
        binding.optionSettings.setOnClickListener(v -> {
            Log.d(TAG, "设置被点击");
        });

        binding.optionPrivacy.setOnClickListener(v -> {
            Log.d(TAG, "隐私政策被点击");
        });

        binding.optionHelp.setOnClickListener(v -> {
            Log.d(TAG, "帮助中心被点击");
        });

        binding.optionAbout.setOnClickListener(v -> {
            Log.d(TAG, "关于我们被点击");
        });

        binding.optionLogout.setOnClickListener(v -> {
            Log.d(TAG, "退出登录被点击");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理ViewBinding以防止内存泄漏
        binding = null;
    }
}