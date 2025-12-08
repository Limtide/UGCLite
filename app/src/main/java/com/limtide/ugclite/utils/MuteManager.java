package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 静音状态管理器
 * 支持APP生命周期内生效，APP冷启后重置
 */
public class MuteManager {
    private static final String TAG = "MuteManager";
    private static final String PREFS_NAME = "mute_settings";
    private static final String KEY_IS_MUTED = "is_muted";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    private static MuteManager instance;
    private SharedPreferences preferences;
    private boolean isMuted = false;
    private boolean isInitialized = false;

    // 静音状态变化监听器
    public interface OnMuteStateChangeListener {
        void onMuteStateChanged(boolean isMuted);
    }

    private OnMuteStateChangeListener listener;

    private MuteManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeState();
    }

    /**
     * 获取单例实例
     */
    public static synchronized MuteManager getInstance(Context context) {
        if (instance == null) {
            instance = new MuteManager(context);
        }
        return instance;
    }

    /**
     * 初始化静音状态
     * APP冷启后重置为非静音状态
     */
    private void initializeState() {
        if (!isInitialized) {
            // 检查是否是首次启动
            boolean isFirstLaunch = preferences.getBoolean(KEY_IS_FIRST_LAUNCH, true);

            if (isFirstLaunch) {
                // 首次启动，设置为非静音状态
                Log.d(TAG, "首次启动APP，设置默认非静音状态");
                isMuted = false;
                preferences.edit()
                        .putBoolean(KEY_IS_MUTED, false)
                        .putBoolean(KEY_IS_FIRST_LAUNCH, false)
                        .apply();
            } else {
                // 非首次启动，读取保存的状态
                isMuted = preferences.getBoolean(KEY_IS_MUTED, false);
                Log.d(TAG, "读取保存的静音状态: " + isMuted);
            }

            isInitialized = true;
        }
    }

    /**
     * 切换静音状态
     * @return 新的静音状态
     */
    public boolean toggleMute() {
        isMuted = !isMuted;

        // 保存状态到SharedPreferences
        preferences.edit()
                .putBoolean(KEY_IS_MUTED, isMuted)
                .apply();

        Log.d(TAG, "静音状态切换为: " + isMuted);

        // 通知监听器
        if (listener != null) {
            listener.onMuteStateChanged(isMuted);
        }

        return isMuted;
    }

    /**
     * 设置静音状态
     * @param muted 静音状态
     */
    public void setMuted(boolean muted) {
        if (isMuted != muted) {
            isMuted = muted;

            // 保存状态到SharedPreferences
            preferences.edit()
                    .putBoolean(KEY_IS_MUTED, isMuted)
                    .apply();

            Log.d(TAG, "静音状态设置为: " + isMuted);

            // 通知监听器
            if (listener != null) {
                listener.onMuteStateChanged(isMuted);
            }
        }
    }

    /**
     * 获取当前静音状态
     * @return 是否静音
     */
    public boolean isMuted() {
        return isMuted;
    }

    /**
     * 设置静音状态变化监听器
     * @param listener 监听器
     */
    public void setOnMuteStateChangeListener(OnMuteStateChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 重置静音状态（用于测试或特殊情况）
     */
    public void reset() {
        Log.d(TAG, "重置静音状态管理器");
        isMuted = false;
        isInitialized = false;
        preferences.edit()
                .clear()
                .apply();
        initializeState();
    }

    /**
     * 重置为默认状态（APP冷启时调用）
     */
    public void resetForColdStart() {
        Log.d(TAG, "APP冷启，重置静音状态为默认");
        isMuted = false;
        preferences.edit()
                .putBoolean(KEY_IS_MUTED, false)
                .putBoolean(KEY_IS_FIRST_LAUNCH, true)
                .apply();
        isInitialized = false;
        initializeState();
    }

    /**
     * 获取静音状态的图标资源
     * @return 图标资源ID
     */
    public int getMuteIconResource() {
        return isMuted ?
            com.limtide.ugclite.R.drawable.ic_mute_speaker_background :
            com.limtide.ugclite.R.drawable.ic_speaker_background;
    }

    /**
     * 获取静音状态的描述文字
     * @return 描述文字
     */
    public String getMuteStateDescription() {
        return isMuted ? "已静音" : "未静音";
    }
}