package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存管理器
 * 负责管理应用中的所有缓存：音乐文件、视频缩略图、图片缓存等
 */
public class CacheManager {

    private static final String TAG = "CacheManager";
    private static CacheManager instance;
    private final Context context;
    private final SharedPreferences preferences;

    // 缓存配置常量
    private static final String PREF_NAME = "cache_settings";
    private static final String KEY_LAST_CLEANUP_TIME = "last_cleanup_time";
    private static final String KEY_APP_VERSION = "app_version";

    // 清理策略配置（优化为更频繁的清理，解决4GB问题）
    private static final long CLEANUP_INTERVAL_DAYS = 1; // 改为每1天清理一次
    private static final long MAX_MUSIC_CACHE_SIZE = 50 * 1024 * 1024; // 改为50MB音乐缓存
    private static final long MAX_THUMBNAIL_CACHE_SIZE = 50 * 1024 * 1024; // 改为50MB缩略图缓存
    private static final long MAX_FILE_AGE_DAYS = 3; // 改为文件最多保留3天

    // 线程池用于异步清理
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor();

    private CacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取单例实例
     */
    public static synchronized CacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new CacheManager(context);
        }
        return instance;
    }

    /**
     * 检查是否需要执行缓存清理
     */
    public boolean shouldCleanup() {
        long lastCleanupTime = preferences.getLong(KEY_LAST_CLEANUP_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long daysSinceLastCleanup = (currentTime - lastCleanupTime) / (1000 * 60 * 60 * 24);

        // 距离上次清理超过配置天数，则需要清理
        boolean shouldClean = daysSinceLastCleanup >= CLEANUP_INTERVAL_DAYS;

        Log.d(TAG, "Should cleanup: " + shouldClean +
                  ", days since last cleanup: " + daysSinceLastCleanup);

        return shouldClean;
    }

    /**
     * 强制检查并清理缓存（忽略时间限制）
     */
    public boolean forceShouldCleanup() {
        Log.w(TAG, "强制检查缓存清理，忽略时间限制");
        return true; // 强制返回true，立即执行清理
    }

    /**
     * 执行完整的缓存清理
     */
    public void performCleanup() {
        performCleanup(null);
    }

    /**
     * 执行缓存清理并回调结果
     */
    public void performCleanup(CleanupCallback callback) {
        if (cleanupExecutor.isShutdown()) {
            if (callback != null) {
                callback.onError("清理线程池已关闭");
            }
            return;
        }

        cleanupExecutor.execute(() -> {
            try {
                Log.d(TAG, "开始执行缓存清理");
                long startTime = System.currentTimeMillis();

                CleanupResult result = new CleanupResult();

                // 1. 清理音乐缓存
                result.musicCleanupResult = cleanupMusicCache();

                // 2. 清理视频缩略图缓存
                result.thumbnailCleanupResult = cleanupThumbnailCache();

                // 3. 清理Glide内存缓存
                cleanupGlideMemoryCache();

                // 4. 清理临时文件
                result.tempFilesCleanupResult = cleanupTempFiles();

                // 计算总清理大小
                result.totalCleanedSize = result.musicCleanupResult.cleanedSize +
                                          result.thumbnailCleanupResult.cleanedSize +
                                          result.tempFilesCleanupResult.cleanedSize;

                result.totalDeletedFiles = result.musicCleanupResult.deletedFiles +
                                          result.thumbnailCleanupResult.deletedFiles +
                                          result.tempFilesCleanupResult.deletedFiles;

                result.duration = System.currentTimeMillis() - startTime;

                // 更新最后清理时间
                preferences.edit()
                        .putLong(KEY_LAST_CLEANUP_TIME, System.currentTimeMillis())
                        .apply();

                Log.d(TAG, "缓存清理完成: " + result.toString());

                // 在主线程回调
                if (callback != null) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onSuccess(result));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "缓存清理过程中出错", e);
                if (callback != null) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> callback.onError(e.getMessage()));
                    }
                }
            }
        });
    }

    /**
     * 清理音乐缓存
     */
    private CleanupResult.ItemCleanupResult cleanupMusicCache() {
        CleanupResult.ItemCleanupResult result = new CleanupResult.ItemCleanupResult();

        try {
            File musicCacheDir = new File(context.getExternalFilesDir(null), "music_cache");
            if (!musicCacheDir.exists()) {
                Log.d(TAG, "音乐缓存目录不存在");
                return result;
            }

            File[] files = musicCacheDir.listFiles();
            if (files == null) return result;

            long totalSize = 0;
            int deletedCount = 0;

            for (File file : files) {
                if (file.isFile() && shouldDeleteFile(file, MAX_FILE_AGE_DAYS)) {
                    long fileSize = file.length();
                    if (file.delete()) {
                        totalSize += fileSize;
                        deletedCount++;
                        Log.d(TAG, "删除过期音乐文件: " + file.getName() +
                                  ", 大小: " + formatFileSize(fileSize));
                    }
                }
            }

            // 如果缓存目录仍然过大，删除最旧的文件直到满足大小限制
            long currentSize = getDirectorySize(musicCacheDir);
            while (currentSize > MAX_MUSIC_CACHE_SIZE && files.length > 0) {
                File oldestFile = findOldestFile(files);
                if (oldestFile != null && oldestFile.delete()) {
                    currentSize -= oldestFile.length();
                    deletedCount++;
                    Log.d(TAG, "删除音乐文件以控制缓存大小: " + oldestFile.getName());
                } else {
                    break;
                }
            }

            result.cleanedSize = totalSize;
            result.deletedFiles = deletedCount;

        } catch (Exception e) {
            Log.e(TAG, "清理音乐缓存时出错", e);
        }

        return result;
    }

    /**
     * 清理视频缩略图缓存
     */
    private CleanupResult.ItemCleanupResult cleanupThumbnailCache() {
        CleanupResult.ItemCleanupResult result = new CleanupResult.ItemCleanupResult();

        try {
            File cacheDir = context.getCacheDir();
            if (!cacheDir.exists()) {
                return result;
            }

            File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
            if (files == null) return result;

            long totalSize = 0;
            int deletedCount = 0;

            for (File file : files) {
                if (shouldDeleteFile(file, MAX_FILE_AGE_DAYS)) {
                    long fileSize = file.length();
                    if (file.delete()) {
                        totalSize += fileSize;
                        deletedCount++;
                        Log.d(TAG, "删除过期缩略图: " + file.getName() +
                                  ", 大小: " + formatFileSize(fileSize));
                    }
                }
            }

            // 如果缩略图缓存仍然过大，删除最旧的文件
            files = cacheDir.listFiles((dir, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
            long currentSize = getThumbnailsCacheSize();
            while (currentSize > MAX_THUMBNAIL_CACHE_SIZE && files != null && files.length > 0) {
                File oldestFile = findOldestFile(files);
                if (oldestFile != null && oldestFile.delete()) {
                    currentSize -= oldestFile.length();
                    deletedCount++;
                    Log.d(TAG, "删除缩略图以控制缓存大小: " + oldestFile.getName());
                } else {
                    break;
                }
            }

            result.cleanedSize = totalSize;
            result.deletedFiles = deletedCount;

        } catch (Exception e) {
            Log.e(TAG, "清理缩略图缓存时出错", e);
        }

        return result;
    }

    /**
     * 清理Glide内存缓存
     */
    private void cleanupGlideMemoryCache() {
        try {
            // 在主线程清理Glide内存缓存
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Glide.get(context).clearMemory();
                });
            }
            Log.d(TAG, "Glide内存缓存已清理");
        } catch (Exception e) {
            Log.w(TAG, "清理Glide内存缓存时出错", e);
        }
    }

    /**
     * 清理临时文件
     */
    private CleanupResult.ItemCleanupResult cleanupTempFiles() {
        CleanupResult.ItemCleanupResult result = new CleanupResult.ItemCleanupResult();

        try {
            File tempDir = new File(context.getCacheDir(), "temp");
            if (!tempDir.exists()) {
                return result;
            }

            File[] files = tempDir.listFiles();
            if (files == null) return result;

            long totalSize = 0;
            int deletedCount = 0;

            for (File file : files) {
                if (file.isFile() && shouldDeleteFile(file, 1)) { // 临时文件最多保留1天
                    long fileSize = file.length();
                    if (file.delete()) {
                        totalSize += fileSize;
                        deletedCount++;
                        Log.d(TAG, "删除临时文件: " + file.getName() +
                                  ", 大小: " + formatFileSize(fileSize));
                    }
                }
            }

            result.cleanedSize = totalSize;
            result.deletedFiles = deletedCount;

        } catch (Exception e) {
            Log.e(TAG, "清理临时文件时出错", e);
        }

        return result;
    }

    /**
     * 判断文件是否应该被删除
     */
    private boolean shouldDeleteFile(File file, long maxAgeDays) {
        if (!file.isFile()) return false;

        long lastModified = file.lastModified();
        long currentTime = System.currentTimeMillis();
        long ageInDays = (currentTime - lastModified) / (1000 * 60 * 60 * 24);

        return ageInDays > maxAgeDays;
    }

    /**
     * 查找最旧的文件
     */
    private File findOldestFile(File[] files) {
        File oldest = null;
        long oldestTime = Long.MAX_VALUE;

        for (File file : files) {
            if (file.isFile() && file.lastModified() < oldestTime) {
                oldestTime = file.lastModified();
                oldest = file;
            }
        }

        return oldest;
    }

    /**
     * 获取目录大小
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * 获取缩略图缓存总大小
     */
    private long getThumbnailsCacheSize() {
        File cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) return 0;

        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
        if (files == null) return 0;

        long totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
        }

        return totalSize;
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
     * 获取缓存统计信息
     */
    public void getCacheStats(CacheStatsCallback callback) {
        cleanupExecutor.execute(() -> {
            try {
                CacheStats stats = new CacheStats();

                // 音乐缓存统计
                File musicCacheDir = new File(context.getExternalFilesDir(null), "music_cache");
                stats.musicCacheSize = getDirectorySize(musicCacheDir);
                stats.musicFileCount = musicCacheDir.exists() ? musicCacheDir.listFiles() != null ? musicCacheDir.listFiles().length : 0 : 0;

                // 缩略图缓存统计
                File cacheDir = context.getCacheDir();
                File[] thumbnailFiles = cacheDir.listFiles((dir, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
                stats.thumbnailCacheSize = getThumbnailsCacheSize();
                stats.thumbnailFileCount = thumbnailFiles != null ? thumbnailFiles.length : 0;

                // 临时文件统计
                File tempDir = new File(cacheDir, "temp");
                stats.tempCacheSize = getDirectorySize(tempDir);
                stats.tempFileCount = tempDir.exists() ? tempDir.listFiles() != null ? tempDir.listFiles().length : 0 : 0;

                stats.totalCacheSize = stats.musicCacheSize + stats.thumbnailCacheSize + stats.tempCacheSize;

                if (callback != null && context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onStatsReady(stats));
                }

            } catch (Exception e) {
                Log.e(TAG, "获取缓存统计信息时出错", e);
                if (callback != null && context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * 强制清理所有缓存
     */
    public void forceCleanupAll() {
        cleanupExecutor.execute(() -> {
            try {
                Log.d(TAG, "开始强制清理所有缓存");

                // 强制清理音乐缓存
                MusicFileUtils.clearCache(context);

                // 强制清理缩略图缓存
                File cacheDir = context.getCacheDir();
                File[] thumbnailFiles = cacheDir.listFiles((dir, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
                if (thumbnailFiles != null) {
                    int deletedCount = 0;
                    for (File file : thumbnailFiles) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                    Log.d(TAG, "强制清理缩略图缓存，删除了 " + deletedCount + " 个文件");
                }

                // 清理Glide缓存
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Glide.get(context).clearMemory();
                        Glide.get(context).clearDiskCache();
                    });
                }

                Log.d(TAG, "强制清理所有缓存完成");

            } catch (Exception e) {
                Log.e(TAG, "强制清理缓存时出错", e);
            }
        });
    }

    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Log.d(TAG, "缓存管理器已关闭");
    }

    /**
     * 清理回调接口
     */
    public interface CleanupCallback {
        void onSuccess(CleanupResult result);
        void onError(String error);
    }

    /**
     * 缓存统计回调接口
     */
    public interface CacheStatsCallback {
        void onStatsReady(CacheStats stats);
        void onError(String error);
    }

    /**
     * 清理结果
     */
    public static class CleanupResult {
        public ItemCleanupResult musicCleanupResult;
        public ItemCleanupResult thumbnailCleanupResult;
        public ItemCleanupResult tempFilesCleanupResult;
        public long totalCleanedSize;
        public int totalDeletedFiles;
        public long duration; // 清理耗时（毫秒）

        @Override
        public String toString() {
            return "CleanupResult{" +
                    "totalCleanedSize=" + formatSize(totalCleanedSize) +
                    ", totalDeletedFiles=" + totalDeletedFiles +
                    ", duration=" + duration + "ms" +
                    '}';
        }

        private static String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }

        public static class ItemCleanupResult {
            public long cleanedSize;
            public int deletedFiles;
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public long musicCacheSize;
        public int musicFileCount;
        public long thumbnailCacheSize;
        public int thumbnailFileCount;
        public long tempCacheSize;
        public int tempFileCount;
        public long totalCacheSize;

        @Override
        public String toString() {
            return "CacheStats{" +
                    "musicCacheSize=" + formatSize(musicCacheSize) +
                    ", musicFileCount=" + musicFileCount +
                    ", thumbnailCacheSize=" + formatSize(thumbnailCacheSize) +
                    ", thumbnailFileCount=" + thumbnailFileCount +
                    ", tempCacheSize=" + formatSize(tempCacheSize) +
                    ", tempFileCount=" + tempFileCount +
                    ", totalCacheSize=" + formatSize(totalCacheSize) +
                    '}';
        }

        private static String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}