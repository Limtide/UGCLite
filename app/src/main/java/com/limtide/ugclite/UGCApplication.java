package com.limtide.ugclite;

import android.app.Application;
import android.util.Log;

import com.limtide.ugclite.utils.MuteManager;

/**
 * Application类
 * 用于处理全局状态和应用生命周期管理
 */
public class UGCApplication extends Application {
    private static final String TAG = "UGCApplication";
    private static UGCApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Application onCreate");

        // 重置静音状态（APP冷启后重置为非静音）
        MuteManager muteManager = MuteManager.getInstance(this);
        muteManager.resetForColdStart();

        Log.d(TAG, "Application初始化完成");
    }

    public static UGCApplication getInstance() {
        return instance;
    }
}