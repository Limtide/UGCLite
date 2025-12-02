package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 应用启动辅助类 - 处理启动流程和自动登录
 * 职责：
 * 1. 检查启动流程（首次启动/未登录/自动登录）
 * 2. 处理自动登录逻辑
 * 3. 管理登录过期检查
 */
public class AppStartupHelper {
    private static final String TAG = "AppStartupHelper";

    // 登录过期时间：7天
    private static final long LOGIN_EXPIRY_DURATION = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 检查启动流程：确定应该显示哪个页面
     * @param context 上下文
     * @return StartupResult 启动结果
     */
    public static StartupResult checkStartupFlow(Context context) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        Log.d(TAG, "==== 应用启动流程检查 ====");
        prefs.printAllPreferences();

        // 1. 检查是否首次启动
        boolean isFirstLaunch = prefs.isFirstLaunch();
        if (isFirstLaunch) {
            Log.d(TAG, "首次启动应用，需要显示引导页");
            return new StartupResult(true, false, false, "首次启动");
        }

        // 2. 检查是否已登录
        boolean isLoggedIn = prefs.isLoggedIn();
        if (!isLoggedIn) {
            Log.d(TAG, "用户未登录，需要显示登录页");
            return new StartupResult(true, false, false, "未登录");
        }

        // 3. 检查登录是否过期
        boolean isLoginExpired = prefs.isLoginExpired(LOGIN_EXPIRY_DURATION);
        if (isLoginExpired) {
            Log.d(TAG, "登录已过期，需要重新登录");
            // 清除过期的登录状态
            prefs.clearLoginState();
            return new StartupResult(true, false, false, "登录过期");
        }

        // 4. 检查是否启用自动登录
        boolean isAutoLoginEnabled = prefs.isAutoLoginEnabled();
        if (!isAutoLoginEnabled) {
            Log.d(TAG, "用户未启用自动登录，需要显示登录页");
            return new StartupResult(true, false, false, "未启用自动登录");
        }

        // 5. 可以自动登录，进入主应用
        Log.d(TAG, "可以自动登录，直接进入主应用");
        String currentUsername = prefs.getCurrentUsername();
        return new StartupResult(false, true, true, currentUsername);
    }

    /**
     * 处理自动登录成功
     */
    public static void handleAutoLoginSuccess(Context context, String newSessionToken) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        // 更新Session Token和登录时间
        prefs.updateSessionToken(newSessionToken);

        Log.d(TAG, "自动登录成功，Session Token已更新");
    }

    /**
     * 处理自动登录失败
     */
    public static void handleAutoLoginFailure(Context context, String errorMessage) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        Log.w(TAG, "自动登录失败: " + errorMessage);

        // 如果是认证失败，清除登录状态
        if (errorMessage.contains("认证") || errorMessage.contains("token") ||
            errorMessage.contains("unauthorized") || errorMessage.contains("401")) {
            Log.d(TAG, "认证失败，清除登录状态");
            prefs.clearLoginState();
        }
    }

    /**
     * 强制重新登录（用于检测到token失效时）
     */
    public static void forceRelogin(Context context) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        // 清除登录状态，但保留用户偏好设置
        prefs.clearLoginState();

        Log.d(TAG, "强制重新登录，已清除登录状态");
    }

    /**
     * 创建跳转到主应用的Intent
     */
    public static Intent createMainIntent(Context context) {
        Intent intent = new Intent(context, com.limtide.ugclite.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    /**
     * 创建跳转到登录页的Intent
     */
    public static Intent createLoginIntent(Context context) {
        Intent intent = new Intent(context, com.limtide.ugclite.activity.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    /**
     * 启动结果类
     */
    public static class StartupResult {
        public final boolean needLogin;      // 是否需要登录
        public final boolean needGuide;      // 是否需要引导
        public final boolean canAutoLogin;   // 是否可以自动登录
        public final String reason;          // 原因或用户名

        public StartupResult(boolean needLogin, boolean needGuide, boolean canAutoLogin, String reason) {
            this.needLogin = needLogin;
            this.needGuide = needGuide;
            this.canAutoLogin = canAutoLogin;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "StartupResult{" +
                    "needLogin=" + needLogin +
                    ", needGuide=" + needGuide +
                    ", canAutoLogin=" + canAutoLogin +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /**
     * 记录应用启动统计
     */
    public static void recordAppLaunch(Context context) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        if (prefs.isFirstLaunch()) {
            // 首次启动的设置在LoginActivity中处理
            Log.d(TAG, "首次启动，记录初始设置");
        } else {
            // 增加启动次数
            prefs.incrementLaunchCount();
            Log.d(TAG, "应用启动次数: " + prefs.getTotalLaunchCount());
        }
    }

    /**
     * 检查应用版本更新
     */
    public static boolean checkAppUpdate(Context context, String currentVersion) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);
        String lastVersion = prefs.getLastAppVersion();

        boolean isUpdated = !currentVersion.equals(lastVersion);

        if (isUpdated) {
            Log.d(TAG, "检测到应用版本更新: " + lastVersion + " -> " + currentVersion);
            // 这里可以触发版本更新后的处理逻辑
        }

        return isUpdated;
    }

    /**
     * 执行应用退出时的清理工作
     */
    public static void onAppExit(Context context) {
        PreferenceManager prefs = PreferenceManager.getInstance(context);

        Log.d(TAG, "应用退出，保存当前状态");

        // 这里可以保存一些需要在下次启动时恢复的状态
        // 例如：当前选中的标签页、滚动位置等
    }
}