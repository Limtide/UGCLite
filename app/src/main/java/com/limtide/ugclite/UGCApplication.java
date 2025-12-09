package com.limtide.ugclite;

import android.app.Application;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.limtide.ugclite.utils.MuteManager;
import com.limtide.ugclite.utils.CacheManager;
import com.limtide.ugclite.utils.VideoThumbnailUtil;
import com.limtide.ugclite.utils.MusicFileUtils;
import com.bumptech.glide.Glide;

/**
 * Application类
 * 用于处理全局状态和应用生命周期管理
 */
public class UGCApplication extends Application {
    private static final String TAG = "UGCApplication";
    private static UGCApplication instance;

    // 缓存管理相关
    private CacheManager cacheManager;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Application onCreate");

        // 重置静音状态（APP冷启后重置为非静音）
        MuteManager muteManager = MuteManager.getInstance(this);
        muteManager.resetForColdStart();

        // 初始化缓存管理器
        initCacheManager();

        // 配置Glide缓存限制（解决4GB问题）
        configureGlideCache();

        Log.d(TAG, "Application初始化完成");
    }

    /**
     * 初始化缓存管理器
     */
    private void initCacheManager() {
        try {
            cacheManager = CacheManager.getInstance(this);
            Log.d(TAG, "缓存管理器初始化完成");

            // 应用启动时检查是否需要立即清理缓存（针对9GB内存问题）
            if (isCriticalMemoryUsage()) {
                Log.w(TAG, "检测到严重的内存使用情况，立即执行缓存清理");
                forceCleanupAllCaches();
            } else {
                // 正常情况：延迟检查，避免影响启动速度
                scheduleStartupCacheCheck();
            }

        } catch (Exception e) {
            Log.e(TAG, "初始化缓存管理器失败", e);
        }
    }

    /**
     * 检查是否处于严重的内存使用状态
     */
    private boolean isCriticalMemoryUsage() {
        try {
            // 获取当前应用内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory(); // 应用最大可用内存
            long totalMemory = runtime.totalMemory(); // 当前分配的内存
            long freeMemory = runtime.freeMemory(); // 空闲内存
            long usedMemory = totalMemory - freeMemory; // 已使用内存

            // 计算内存使用率
            float memoryUsagePercent = (float) usedMemory / maxMemory * 100;

            Log.d(TAG, String.format("内存使用情况: 已使用=%dMB, 最大可用=%dMB, 使用率=%.1f%%",
                    usedMemory / (1024 * 1024), maxMemory / (1024 * 1024), memoryUsagePercent));

            // 如果内存使用率超过80%，认为处于严重状态
            return memoryUsagePercent > 80.0f;

        } catch (Exception e) {
            Log.w(TAG, "检查内存使用状态失败", e);
            return false;
        }
    }

    /**
     * 强制清理所有缓存
     */
    private void forceCleanupAllCaches() {
        if (cacheManager == null) return;

        cacheManager.forceCleanupAll();

        // 同时清理VideoThumbnailUtil的缓存
        try {
            VideoThumbnailUtil.cleanupExpiredThumbnails(this, 1); // 清理1天前的缓存
            Log.d(TAG, "强制清理视频缩略图缓存完成");
        } catch (Exception e) {
            Log.w(TAG, "强制清理缩略图缓存失败", e);
        }

        Log.w(TAG, "强制清理所有缓存完成");
    }

    /**
     * 调度启动时的缓存检查
     */
    private void scheduleStartupCacheCheck() {
        // 延迟3秒执行，避免影响应用启动
        mainHandler.postDelayed(() -> {
            try {
                // 立即强制检查并清理缓存（针对9GB问题）
                Log.w(TAG, "应用启动立即强制清理缓存，解决9GB存储问题");

                cacheManager.performCleanup(new CacheManager.CleanupCallback() {
                    @Override
                    public void onSuccess(CacheManager.CleanupResult result) {
                        Log.w(TAG, "启动强制缓存清理完成: " + result.toString());

                        // 计算清理的数据量
                        long cleanedMB = result.totalCleanedSize / (1024 * 1024);
                        if (cleanedMB > 0) {
                            Log.w(TAG, "释放了约 " + cleanedMB + " MB 存储空间");
                        }

                        // 在主线程显示清理结果（可选）
                        mainHandler.post(() -> {
                            Log.i(TAG, "强制缓存清理成功，释放了 " +
                                 formatFileSize(result.totalCleanedSize) + " 空间");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "启动强制缓存清理失败: " + error);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "执行启动强制缓存检查时出错", e);
            }
        }, 1000); // 延迟1秒，立即执行清理
    }

    /**
     * 配置Glide缓存限制（解决4GB存储问题）
     */
    private void configureGlideCache() {
        try {
            Log.d(TAG, "配置Glide缓存限制");

            // 清理现有的Glide缓存
            Glide.get(this).clearMemory();

            // 在后台线程清理磁盘缓存
            new Thread(() -> {
                try {
                    Glide.get(this).clearDiskCache();
                    Log.d(TAG, "Glide磁盘缓存已清理");
                } catch (Exception e) {
                    Log.w(TAG, "清理Glide磁盘缓存失败", e);
                }
            }).start();

            Log.d(TAG, "Glide缓存配置完成");

        } catch (Exception e) {
            Log.e(TAG, "配置Glide缓存失败", e);
        }
    }

    /**
     * 获取缓存管理器实例
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 获取缓存统计信息
     */
    public void getCacheStats(CacheManager.CacheStatsCallback callback) {
        if (cacheManager != null) {
            cacheManager.getCacheStats(callback);
        }
    }

    /**
     * 手动触发缓存清理
     */
    public void cleanupCache(CacheManager.CleanupCallback callback) {
        if (cacheManager != null) {
            cacheManager.performCleanup(callback);
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 获取内存使用情况信息
     */
    public String getMemoryUsageInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            return String.format("内存使用情况: 已使用=%dMB, 总分配=%dMB, 可用最大=%dMB, 使用率=%.1f%%",
                    usedMemory / (1024 * 1024),
                    totalMemory / (1024 * 1024),
                    maxMemory / (1024 * 1024),
                    (float) usedMemory / maxMemory * 100);
        } catch (Exception e) {
            return "获取内存信息失败: " + e.getMessage();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "系统内存不足，强制清理缓存");

        if (cacheManager != null) {
            forceCleanupAllCaches();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        String levelDesc;
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN:
                levelDesc = "UI不可见";
                break;
            case TRIM_MEMORY_MODERATE:
                levelDesc = "中等程度内存压力";
                break;
            case TRIM_MEMORY_COMPLETE:
                levelDesc = "严重内存压力";
                Log.w(TAG, "收到严重内存压力警告，执行缓存清理");
                if (cacheManager != null) {
                    cacheManager.performCleanup();
                }
                break;
            default:
                levelDesc = "其他(" + level + ")";
                break;
        }

        Log.d(TAG, "收到内存trim请求: " + levelDesc);
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "Application终止");

        // 关闭缓存管理器
        if (cacheManager != null) {
            cacheManager.shutdown();
        }

        // 关闭其他资源
        try {
            MusicFileUtils.shutdown();
        } catch (Exception e) {
            Log.w(TAG, "关闭MusicFileUtils失败", e);
        }

        super.onTerminate();
    }

    public static UGCApplication getInstance() {
        return instance;
    }
}