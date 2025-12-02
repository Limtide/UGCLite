package com.limtide.ugclite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.limtide.ugclite.MainActivity;
import com.limtide.ugclite.databinding.ActivityLoginBinding;
import com.limtide.ugclite.utils.AppStartupHelper;
import com.limtide.ugclite.utils.PreferenceManager;
import com.limtide.ugclite.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // ViewBinding
    private ActivityLoginBinding binding;

    // ViewModel
    private LoginViewModel loginViewModel;

    // PreferenceManager
    private PreferenceManager preferenceManager;

    // 自动登录模式标记
    private boolean isAutoLoginMode = false;

    // 离线模式标记
    protected boolean is_offline_mode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity is being created.");

        // 1. 先初始化PreferenceManager
        preferenceManager = PreferenceManager.getInstance(this);

        // 2. 检查自动登录（首次启动检查在MainActivity中处理）
        if (checkAndHandleAutoLogin()) {
            return; // 自动登录成功，直接跳转
        }

        // 3. 如果不能自动登录，显示登录页面
        initializeLoginUI();
    }

    /**
     * 检查并处理自动登录
     * @return true 如果自动登录成功
     */
    private boolean checkAndHandleAutoLogin() {
        Log.d(TAG, "==== 检查自动登录 ====");

        // 使用AppStartupHelper检查启动流程
        AppStartupHelper.StartupResult result = AppStartupHelper.checkStartupFlow(this);
        Log.d(TAG, "启动流程检查结果: " + result.toString());

        if (result.canAutoLogin) {
            // 可以自动登录
            Log.d(TAG, "自动登录成功，用户: " + result.reason);

            // 设置自动登录模式标记，避免UI恢复和用户名重复写入
            isAutoLoginMode = true;

            performAutoLogin(result.reason);
            return true;
        }

        // 不能自动登录，需要显示登录页面
        Log.d(TAG, "需要登录，原因: " + result.reason);

        // 重置自动登录模式标记
        isAutoLoginMode = false;

        return false;
    }

    /**
     * 执行自动登录
     */
    private void performAutoLogin(String username) {
        // 显示加载状态
        showLoadingState(true);

        // 这里可以调用后端API验证token有效性
        // 为了演示，我们直接模拟成功
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            showLoadingState(false);
            android.widget.Toast.makeText(this, "欢迎回来，" + username + "！", android.widget.Toast.LENGTH_SHORT).show();
            navigateToMain();
        }, 1000);
    }

    /**
     * 初始化登录界面
     */
    private void initializeLoginUI() {
        Log.d(TAG, "初始化登录界面");

        // 初始化ViewBinding - 使用登录页面布局
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewModel
        loginViewModel = new ViewModelProvider(this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(LoginViewModel.class);

        // 恢复记住的登录信息（仅在非自动登录时）
        restoreLoginInfo();

        // 设置数据观察
        setupObservers();
        // 设置点击事件
        setupClickListeners();
        // 设置文本监听
        setupTextWatchers();
        // 设置焦点监听
        setupFocusListeners();
        // 初始化时需要预埋测试数据
        loginViewModel.createTestUsers();
    }

    /**
     * 恢复记住的登录信息
     */
    private void restoreLoginInfo() {
        // 不再恢复保存的用户名，即使用户选择了记住密码
        // String savedUsername = preferenceManager.getCurrentUsername();
        boolean rememberPassword = preferenceManager.isRememberLogin();

        Log.d(TAG, "恢复登录信息 - 记住密码: " + rememberPassword + " (不恢复用户名)");

        // 不再恢复用户名，即使用户选择了记住密码也不恢复
        if (!isAutoLoginMode) {
            // 用户名输入框保持空白，用户需要手动输入
            // if (savedUsername != null && !savedUsername.trim().isEmpty()) {
            //     binding.etUsername.setText(savedUsername);
            //     binding.etUsername.setSelection(savedUsername.length()); // 移动光标到末尾
            // } else {
            //     Log.d(TAG, "保存的用户名为空或null，不恢复");
            // }
            Log.d(TAG, "不恢复用户名，用户需要手动输入");

            // 只恢复记住密码状态
            binding.cbRememberPassword.setChecked(rememberPassword);
        } else {
            Log.d(TAG, "自动登录模式，跳过UI恢复");
        }
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

        loginViewModel.getLoginResult().observe(this, loginResult -> {
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
        binding.etUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 输入前的处理逻辑
                Log.d(TAG, "before Username changed: " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Username changed: " + s.toString());
                loginViewModel.setUsername(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 输入后的处理逻辑
                Log.d(TAG, "after Username changed: " + s.toString());
            }
        });

        // 密码文本变化监听
        binding.etPassword.addTextChangedListener(new android.text.TextWatcher() {
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
    }

    /**
     * 设置焦点监听器
     */
    private void setupFocusListeners() {
        // 用户名输入框焦点监听
        binding.etUsername.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                binding.tilUsername.setError(null);
                binding.tilUsername.setErrorEnabled(false);
            } else {
                // 失去焦点时，主动调用 ViewModel 的 set 方法
                String errorMsg = loginViewModel.getErrorMessage().getValue();
                if (!android.text.TextUtils.isEmpty(loginViewModel.getUsername().getValue())) {
                    binding.tilUsername.setError(null);
                    binding.tilUsername.setErrorEnabled(false);
                } else {
                    binding.tilUsername.setError(errorMsg != null ? errorMsg : "请输入有效的用户名");
                    binding.tilUsername.setErrorEnabled(true);
                }
            }
        });

        // 密码输入框焦点监听
        binding.etPassword.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                binding.tilPassword.setError(null);
                binding.tilPassword.setErrorEnabled(false);
            } else {
                String errorMsg = loginViewModel.getErrorMessage().getValue();
                if (!android.text.TextUtils.isEmpty(loginViewModel.getPassword().getValue())) {
                    binding.tilPassword.setError(null);
                    binding.tilPassword.setErrorEnabled(false);
                } else {
                    binding.tilPassword.setError(errorMsg != null ? errorMsg : "请输入至少6位密码");
                    binding.tilPassword.setErrorEnabled(true);
                }
            }
        });

        // 记住密码状态变化监听
        binding.cbRememberPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Remember password changed: " + isChecked);
            loginViewModel.setRememberPassword(isChecked);
        });
    }

    /**
     * 保存当前登录用户信息到SharedPreferences
     */
    private void saveCurrentUser() {
        String username = loginViewModel.getUsername().getValue();
        if (username != null && !username.trim().isEmpty()) {
            // 获取用户选择的设置
            boolean rememberLogin = binding.cbRememberPassword.isChecked();

            // 使用PreferenceManager保存用户信息
            preferenceManager.saveLoginState(
                    username.trim(),           // 用户名
                    generateUserId(username),    // 用户ID（简化生成）
                    generateSessionToken(),      // Session Token（简化生成）
                    "normal",                 // 登录方式
                    rememberLogin,             // 是否记住登录
                    rememberLogin              // 是否自动登录（和记住密码一致）
            );

            Log.d(TAG, "保存登录状态 - 用户: " + username +
                      ", 记住登录: " + rememberLogin +
                      ", 自动登录: " + rememberLogin);
        }
    }

    /**
     * 生成用户ID（简化版）
     */
    private String generateUserId(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "user_" + System.currentTimeMillis();
        }
        // 简单的哈希生成用户ID
        return "user_" + Math.abs(username.hashCode()) + "_" + System.currentTimeMillis();
    }

    /**
     * 生成会话令牌（简化版）
     */
    private String generateSessionToken() {
        // 使用当前时间戳和随机数生成简单的token
        return "token_" + System.currentTimeMillis() + "_" +
               Integer.toString((int)(Math.random() * 10000));
    }

    /**
     * 检查是否是预设用户名（简化版，只检查demo）
     */
    private boolean isPresetUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // 只检查demo用户名
        return "demo".equals(username);
    }

    /**
     * 显示错误消息
     */
    private void showError(String message) {
        Log.d(TAG, "Showing error: " + message);
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示加载状态
     */
    private void showLoadingState(boolean show) {
        if (binding != null) {
            // 如果显示加载状态，禁用所有按钮
            binding.btnLogin.setEnabled(!show);
            binding.btnWechatLogin.setEnabled(!show);
            binding.btnAppleLogin.setEnabled(!show);
            binding.etUsername.setEnabled(!show);
            binding.etPassword.setEnabled(!show);

            // 更新登录按钮文本
            if (show) {
                binding.btnLogin.setText("登录中...");
            } else {
                binding.btnLogin.setText("登录");
            }
        }
    }

    /**
     * 显示成功消息
     */
    private void showSuccess(String message) {
        Log.d(TAG, "Showing success: " + message);
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * 跳转到主页面
     */
    private void navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity is being destroy.");
        // 清理ViewBinding
        binding = null;
    }
}