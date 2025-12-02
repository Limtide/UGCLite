package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * SharedPreferences工具类 - 用于持久化存储用户偏好设置
 * 这些数据会在多次启动之间保持
 */
public class PreferenceManager {
    private static final String TAG = "PreferenceManager";
    private static final String PREFS_NAME = "ugclite_preferences";

    // 键名常量
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_USER_NAME = "current_username";
    public static final String KEY_USER_ID = "current_user_id";
    public static final String KEY_LOGIN_TIME = "login_time";
    public static final String KEY_REMEMBER_PASSWORD = "remember_password";
    public static final String KEY_AUTO_PLAY_VIDEO = "auto_play_video";
    public static final String KEY_IMAGE_QUALITY = "image_quality";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    public static final String KEY_CACHE_SIZE_MB = "cache_size_mb";
    public static final String KEY_LAST_APP_VERSION = "last_app_version";
    public static final String KEY_TOTAL_LAUNCH_COUNT = "total_launch_count";

    // 新增：登录状态相关
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_SESSION_TOKEN = "session_token";
    public static final String KEY_LOGIN_METHOD = "login_method"; // normal, wechat, apple
    public static final String KEY_REMEMBER_LOGIN = "remember_login";
    public static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled";

    private static PreferenceManager instance;
    private SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    // ========== 用户信息相关 ==========

    /**
     * 保存当前用户名
     */
    public void setCurrentUsername(String username) {
        prefs.edit()
                .putString(KEY_USER_NAME, username)
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply();
        Log.d(TAG, "保存用户名: " + username);
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /**
     * 保存当前用户ID
     */
    public void setCurrentUserId(String userId) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .apply();
        Log.d(TAG, "保存用户ID: " + userId);
    }

    /**
     * 获取当前用户ID
     */
    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * 获取登录时间
     */
    public long getLoginTime() {
        return prefs.getLong(KEY_LOGIN_TIME, 0);
    }

    // ==================== 登录状态管理 ====================

    /**
     * 保存完整登录状态
     */
    public void saveLoginState(String username, String userId, String sessionToken,
                              String loginMethod, boolean rememberLogin, boolean autoLogin) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_NAME, username);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_SESSION_TOKEN, sessionToken);
        editor.putString(KEY_LOGIN_METHOD, loginMethod);
        editor.putBoolean(KEY_REMEMBER_LOGIN, rememberLogin);
        editor.putBoolean(KEY_AUTO_LOGIN_ENABLED, autoLogin);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "保存登录状态 - 用户: " + username + ", ID: " + userId +
                  ", 方法: " + loginMethod + ", 记住登录: " + rememberLogin +
                  ", 自动登录: " + autoLogin);
    }

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        String username = prefs.getString(KEY_USER_NAME, null);
        String userId = prefs.getString(KEY_USER_ID, null);

        // 更严格的检查：必须同时满足登录状态和用户信息
        boolean hasValidUserInfo = username != null && !username.trim().isEmpty()
                                && userId != null && !userId.trim().isEmpty();

        Log.d(TAG, "检查登录状态 - isLoggedIn: " + isLoggedIn + ", hasValidUserInfo: " + hasValidUserInfo);

        return isLoggedIn && hasValidUserInfo;
    }

    /**
     * 检查是否启用自动登录
     */
    public boolean isAutoLoginEnabled() {
        return isLoggedIn() && prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false);
    }

    /**
     * 设置自动登录开关
     */
    public void setAutoLoginEnabled(boolean enabled) {
        prefs.edit()
              .putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled)
              .apply();
        Log.d(TAG, "设置自动登录: " + enabled);
    }

    /**
     * 检查是否记住登录状态
     */
    public boolean isRememberLogin() {
        return prefs.getBoolean(KEY_REMEMBER_LOGIN, true);
    }

    /**
     * 设置记住登录状态
     */
    public void setRememberLogin(boolean remember) {
        prefs.edit()
              .putBoolean(KEY_REMEMBER_LOGIN, remember)
              .apply();
        Log.d(TAG, "设置记住登录: " + remember);
    }

    /**
     * 获取Session Token
     */
    public String getSessionToken() {
        return prefs.getString(KEY_SESSION_TOKEN, null);
    }

    /**
     * 更新Session Token
     */
    public void updateSessionToken(String newToken) {
        prefs.edit()
              .putString(KEY_SESSION_TOKEN, newToken)
              .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
              .apply();
        Log.d(TAG, "更新Session Token");
    }

    /**
     * 获取登录方法
     */
    public String getLoginMethod() {
        return prefs.getString(KEY_LOGIN_METHOD, "normal");
    }

    /**
     * 检查登录是否过期（例如超过7天）
     */
    public boolean isLoginExpired(long maxDurationMs) {
        long loginTime = getLoginTime();
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - loginTime;

        boolean expired = duration > maxDurationMs;

        if (expired) {
            Log.w(TAG, "登录已过期 - 登录时间: " + loginTime + ", 当前时间: " + currentTime +
                      ", 时长: " + duration + "ms, 限制: " + maxDurationMs + "ms");
        } else {
            Log.d(TAG, "登录有效 - 时长: " + duration + "ms");
        }

        return expired;
    }

    /**
     * 清除登录状态（退出登录时使用）
     */
    public void clearLoginState() {
        // 保留"记住登录"和"自动登录"的设置，只清除登录信息
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_SESSION_TOKEN);
        editor.remove(KEY_LOGIN_TIME);
        editor.remove(KEY_LOGIN_METHOD);
        editor.apply();

        Log.d(TAG, "清除登录状态，保留偏好设置");
    }

    /**
     * 是否记住密码
     */
    public void setRememberPassword(boolean remember) {
        prefs.edit()
                .putBoolean(KEY_REMEMBER_PASSWORD, remember)
                .apply();
        Log.d(TAG, "设置记住密码: " + remember);
    }

    public boolean isRememberPassword() {
        return prefs.getBoolean(KEY_REMEMBER_PASSWORD, false);
    }

    // ========== 应用设置相关 ==========

    /**
     * 设置是否自动播放视频
     */
    public void setAutoPlayVideo(boolean autoPlay) {
        prefs.edit()
                .putBoolean(KEY_AUTO_PLAY_VIDEO, autoPlay)
                .apply();
        Log.d(TAG, "设置自动播放视频: " + autoPlay);
    }

    public boolean isAutoPlayVideo() {
        return prefs.getBoolean(KEY_AUTO_PLAY_VIDEO, true);
    }

    /**
     * 设置图片质量 (low:1, medium:2, high:3)
     */
    public void setImageQuality(int quality) {
        prefs.edit()
                .putInt(KEY_IMAGE_QUALITY, quality)
                .apply();
        Log.d(TAG, "设置图片质量: " + quality);
    }

    public int getImageQuality() {
        return prefs.getInt(KEY_IMAGE_QUALITY, 2); // 默认中等质量
    }

    /**
     * 设置主题模式 (0:浅色, 1:深色, 2:跟随系统)
     */
    public void setThemeMode(int mode) {
        prefs.edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
        Log.d(TAG, "设置主题模式: " + mode);
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, 2); // 默认跟随系统
    }

    /**
     * 设置推送通知开关
     */
    public void setNotificationEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
                .apply();
        Log.d(TAG, "设置推送通知: " + enabled);
    }

    public boolean isNotificationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    // ========== 应用状态相关 ==========

    /**
     * 是否首次启动
     */
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    /**
     * 标记首次启动完成
     */
    public void setFirstLaunchComplete() {
        prefs.edit()
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .putLong(KEY_TOTAL_LAUNCH_COUNT, getTotalLaunchCount() + 1)
                .putString(KEY_LAST_APP_VERSION, getCurrentAppVersion())
                .apply();
        Log.d(TAG, "首次启动标记完成");
    }

    /**
     * 增加启动次数
     */
    public void incrementLaunchCount() {
        long currentCount = getTotalLaunchCount();
        prefs.edit()
                .putLong(KEY_TOTAL_LAUNCH_COUNT, currentCount + 1)
                .putString(KEY_LAST_APP_VERSION, getCurrentAppVersion())
                .apply();
        Log.d(TAG, "启动次数: " + (currentCount + 1));
    }

    public long getTotalLaunchCount() {
        return prefs.getLong(KEY_TOTAL_LAUNCH_COUNT, 0);
    }

    /**
     * 获取上次使用的应用版本
     */
    public String getLastAppVersion() {
        return prefs.getString(KEY_LAST_APP_VERSION, "");
    }

    /**
     * 获取当前应用版本
     */
    private String getCurrentAppVersion() {
        // 这里应该从BuildConfig获取，暂时返回默认值
        return "1.0.0";
    }

    /**
     * 设置缓存大小限制(MB)
     */
    public void setCacheSizeLimit(int sizeMB) {
        prefs.edit()
                .putInt(KEY_CACHE_SIZE_MB, sizeMB)
                .apply();
        Log.d(TAG, "设置缓存大小限制: " + sizeMB + "MB");
    }

    public int getCacheSizeLimit() {
        return prefs.getInt(KEY_CACHE_SIZE_MB, 100); // 默认100MB
    }

    // ========== 工具方法 ==========

    /**
     * 清除所有用户相关数据（退出登录时使用）
     */
    public void clearUserData() {
        prefs.edit()
                .remove(KEY_USER_NAME)
                .remove(KEY_LOGIN_TIME)
                .remove(KEY_REMEMBER_PASSWORD)
                .apply();
        Log.d(TAG, "已清除用户数据");
    }

    /**
     * 清除所有偏好设置
     */
    public void clearAllPreferences() {
        prefs.edit().clear().apply();
        Log.d(TAG, "已清除所有偏好设置");
    }

    /**
     * 打印所有存储的数据（调试用）
     */
    public void printAllPreferences() {
        Log.d(TAG, "===== 所有SharedPreferences数据 =====");
        for (String key : prefs.getAll().keySet()) {
            Object value = prefs.getAll().get(key);
            Log.d(TAG, key + ": " + value);
        }
        Log.d(TAG, "================================");
    }

    /**
     * 获取数据存储大小（近似值）
     */
    public int getPreferencesDataSize() {
        return prefs.getAll().toString().length();
    }
}