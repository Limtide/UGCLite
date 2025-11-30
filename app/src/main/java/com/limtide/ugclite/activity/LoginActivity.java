package com.limtide.ugclite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.limtide.ugclite.MainActivity;
import com.limtide.ugclite.databinding.ActivityLoginBinding;
import com.limtide.ugclite.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // ViewBinding
    private ActivityLoginBinding binding;

    // ViewModel
    private LoginViewModel loginViewModel;

//    protected boolean is_offline_mode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity is being created.");

        // 初始化ViewBinding - 使用登录页面布局
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel
        loginViewModel = new ViewModelProvider(this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(LoginViewModel.class);

        // 设置数据观察
        setupObservers();
        // 设置点击事件
        setupClickListeners();
        // 设置文本监听
        setupTextWatchers();
        setupFocusListeners();
        // 初始化时需要预埋测试数据
        loginViewModel.createTestUsers();
    }

    /**
     * 设置数据观察者
     */
    private void setupObservers() {
        // 观察加载状态
        loginViewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnWechatLogin.setEnabled(!isLoading);
            binding.btnAppleLogin.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnLogin.setText("登录中...");
            } else {
                binding.btnLogin.setText("登录");
            }
        });

        loginViewModel.getLoginResult().observe(this,loginResult -> {
            if (loginResult != null) {
                if (loginResult.isSuccess()) {
                    // === 登录成功 ===
                    showSuccess(loginResult.getMessage());
                    // 保存用户、跳转主页
                    saveCurrentUser();
                    navigateToMain();
                } else {
                    //应该在这里修改登录状态吗，在view层？
                    loginViewModel.setIsLoading(false);
                    showError(loginResult.getMessage());
                }
            }
        });


//        // 观察成功消息
//        loginViewModel.getSuccessMessage().observe(this, successMessage -> {
//            if (successMessage != null && !successMessage.isEmpty()) {
//                showSuccess(successMessage);
//                loginViewModel.clearAllMessages();
//
//                // 如果是登录成功，保存当前用户信息并跳转到主页面
//                if (successMessage.contains("登录成功")) {
//                    // 保存当前登录用户信息
//                    saveCurrentUser();
//                    navigateToMain();
//                }
//            }
//        });

        // 观察表单验证状态
//        loginViewModel.getIsUsernameValid().observe(this, isValid -> {
//            updateInputLayoutValidation(binding.tilUsername, isValid, "请输入有效的用户名");
//        });

//        loginViewModel.getIsPasswordValid().observe(this, isValid -> {
//            updateInputLayoutValidation(binding.tilPassword, isValid, "请输入至少6位密码");
//        });
        //
        loginViewModel.getIsLoginFormValid().observe(this, isValid -> {
            binding.btnLogin.setEnabled(isValid);
        });

        // 观察记住密码状态
        loginViewModel.getRememberPassword().observe(this, remember -> {

        });
    }

    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            loginViewModel.login();
        });

//        new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // v 代表被点击的 View
//            }
//        };
//        binding.btnLogin.setOnClickListener();

        // 微信登录点击事件
        binding.btnWechatLogin.setOnClickListener(v -> {
            Log.d(TAG, "WeChat login button clicked");
            loginViewModel.loginWithWeChat();
        });

        // Apple登录点击事件
        binding.btnAppleLogin.setOnClickListener(v -> {
            Log.d(TAG, "Apple login button clicked");
            loginViewModel.loginWithApple();
        });

        // 忘记密码
        binding.tvForgotPassword.setOnClickListener(v -> {
            Log.d(TAG, "Forgot password clicked");
            loginViewModel.onForgotPassword();
        });

        // 注册提示
        binding.tvRegisterHint.setOnClickListener(v -> {
            Log.d(TAG, "Register hint clicked");
            loginViewModel.onRegisterClick();
        });
    }

    /**
     * 设置文本监听器
     */
    private void setupTextWatchers() {
        // 用户名文本变化监听
        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 输入前的处理逻辑
                Log.d(TAG, "before Username changed: " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Username changed: " + s.toString());
                Log.d(TAG, "Username start: " + start);
                Log.d(TAG, "Username before: " + before);
                Log.d(TAG, "Username count: " + count);
                loginViewModel.setUsername(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 输入后的处理逻辑
                Log.d(TAG, "after Username changed: " + s.toString());
            }
        });

        // 密码文本变化监听
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 输入前的处理逻辑
                Log.d(TAG, "before Password changed length: " + s.length());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Password changed length: " + s.length());
                loginViewModel.setPassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 输入后的处理逻辑
                Log.d(TAG, "after Password changed length: " + s.length());
            }
        });

        // 记住密码状态变化监听
        binding.cbRememberPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Remember password changed: " + isChecked);
            loginViewModel.setRememberPassword(isChecked);
        });
    }

    /**
     * 更新输入框验证状态
     */
    private void updateInputLayoutValidation(com.google.android.material.textfield.TextInputLayout inputLayout,
                                             boolean isValid, String errorMessage) {
        if (isValid) {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
        } else {
            inputLayout.setError(errorMessage);
            inputLayout.setErrorEnabled(true);
        }
    }

    private void setupFocusListeners() {
        // 1. 用户名输入框焦点监听
        binding.etUsername.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                binding.tilUsername.setError(null);
                binding.tilUsername.setErrorEnabled(false);
                loginViewModel.clearAllMessages();
            }
            else {
                // 失去焦点时，主动调用 ViewModel 的 set 方法
                //loginViewModel.setUsername(binding.etUsername.getText().toString());

                // 检查 ViewModel 的验证结果
                // 使用 Boolean.FALSE.equals 可以安全地处理 null 情况
                if (Boolean.FALSE.equals(loginViewModel.getIsUsernameValid().getValue())) {
                    // 直接使用 ViewModel 刚刚生成的错误信息，不再硬编码
                    String error = loginViewModel.getErrorMessage().getValue();
                    showError(error);
                }
            }
        });

        // 2. 密码输入框焦点监听
        binding.etPassword.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                // 获取焦点时清除错误显示
                binding.tilPassword.setError(null);
                binding.tilPassword.setErrorEnabled(false);
                // 可选：清除 ViewModel 中的通用错误信息，避免干扰
                loginViewModel.clearAllMessages();
            } else {
                //loginViewModel.setPassword(binding.etPassword.getText().toString());

                if (Boolean.FALSE.equals(loginViewModel.getIsPasswordValid().getValue())) {
                    // 从ViewModel获取具体的错误文案（比如"请输入密码"或"长度至少6位"）
                    // 这样由于逻辑都在ViewModel里，减少View层逻辑
                    String errorMsg = loginViewModel.getErrorMessage().getValue();
                    binding.tilPassword.setError(errorMsg != null ? errorMsg : "密码格式错误");
                }
            }
        });
    }

    /**
     * 显示错误消息
     */
    private void showError(String message) {
        Log.d(TAG, "Showing error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示成功消息
     */
    private void showSuccess(String message) {
        Log.d(TAG, "Showing success: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 跳转到主页面
     */
    private void navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//      intent.putExtra("is_offline_mode", is_offline_mode);
        startActivity(intent);
        finish();
    }

    /**
     * 保存当前登录用户信息到SharedPreferences
     */
    private void saveCurrentUser() {
        String username = loginViewModel.getUsername().getValue();
        if (username != null && !username.trim().isEmpty()) {
            // 保存到SharedPreferences
            getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("current_username", username.trim())
                    .putLong("login_time", System.currentTimeMillis())
                    .apply();//异步保存，不会卡主线程。
            //      .commit()同步保存。

            Log.d(TAG, "Saved current user: " + username);
        }
    }

//    //个人中心跳转
//    private void navigateToProfile(User user) {
//        Log.d(TAG, "Navigating to ProfileActivity for user: " + user.getUsername());
//        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
//        // 传递用户信息
//        intent.putExtra("username", user.getUsername());
//        intent.putExtra("nickname", user.getDisplayName());
//        intent.putExtra("avatarUrl", user.getAvatarUrl());
//        intent.putExtra("signature", user.getSignature());
//        intent.putExtra("is_offline_mode", is_offline_mode);
//
//        startActivity(intent);
//        finish();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // 清除之前的消息
        loginViewModel.clearAllMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }


    // private  void enterMainActiviy(){
    //     if (is_offline_mode){
    //         Log.d(TAG, "Entering Offline Mode");
    //     }else {
    //         Log.d(TAG, "Entering Online Mode");
    //     }
    //     //显式Intent跳转到登录页面
    //     Intent intent = new Intent(LoginActivity.this,LoginActivity.class);
    //     intent.putExtra("is_offline_mode", is_offline_mode);
    //     startActivity(intent);
    //     finish();
    // }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity is being destroy.");
        // 清理ViewBinding
        binding = null;
    }
}
