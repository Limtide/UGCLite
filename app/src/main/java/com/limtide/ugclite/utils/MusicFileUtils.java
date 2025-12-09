package com.limtide.ugclite.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MP3文件保存和缓存工具类
 */
public class MusicFileUtils {

    private static final String TAG = "MusicFileUtils";

    // 缓存目录
    private static final String MUSIC_CACHE_DIR = "music_cache";

    // 严格限制缓存大小和文件数量（解决4GB问题）
    private static final long MAX_MUSIC_CACHE_SIZE = 10 * 1024 * 1024; // 10MB限制
    private static final int MAX_MUSIC_FILES = 20; // 最多20个音乐文件
    private static final long MAX_SINGLE_FILE_SIZE = 5 * 1024 * 1024; // 单个文件最大5MB

    // 线程池
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // 回调接口
    public interface MusicSaveCallback {
        void onSuccess(String filePath);
        void onError(String error);
        void onProgress(int progress);
    }

    /**
     * 保存MP3文件到本地缓存 - 严格限制版本（解决4GB问题）
     * @param context 上下文
     * @param musicUrl 音乐URL
     * @param callback 回调接口
     */
    public static void saveMusicToLocal(Context context, String musicUrl, MusicSaveCallback callback) {
        if (musicUrl == null || musicUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("音乐URL为空");
            }
            return;
        }

        // 立即执行严格检查和清理（解决4GB问题）
        if (!isMusicDownloadAllowed(context, musicUrl, callback)) {
            return;
        }

        // 在后台线程执行下载
        executorService.execute(() -> {
            try {
                String fileName = generateFileName(musicUrl);
                File cacheDir = getMusicCacheDir(context);
                File musicFile = new File(cacheDir, fileName);

                // 检查文件是否已存在
                if (musicFile.exists()) {
                    Log.d(TAG, "音乐文件已存在: " + musicFile.getAbsolutePath());
                    if (callback != null) {
                        callback.onSuccess(musicFile.getAbsolutePath());
                    }
                    return;
                }

                // 下载文件前再次检查限制
                if (!checkCacheLimits(context)) {
                    if (callback != null) {
                        callback.onError("音乐缓存已满，无法下载新文件");
                    }
                    return;
                }

                // 下载文件
                downloadMusicFile(musicUrl, musicFile, callback);

            } catch (Exception e) {
                Log.e(TAG, "保存音乐文件失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onError("保存失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取本地缓存的MP3文件路径
     * @param context 上下文
     * @param musicUrl 音乐URL
     * @return 本地文件路径，如果不存在返回null
     */
    public static String getCachedMusicPath(Context context, String musicUrl) {
        if (musicUrl == null || musicUrl.isEmpty()) {
            return null;
        }

        try {
            String fileName = generateFileName(musicUrl);
            File cacheDir = getMusicCacheDir(context);
            File musicFile = new File(cacheDir, fileName);

            if (musicFile.exists() && musicFile.length() > 0) {
                Log.d(TAG, "找到缓存的音乐文件: " + musicFile.getAbsolutePath());
                return musicFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存音乐路径失败: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * 检查音乐文件是否已缓存
     * @param context 上下文
     * @param musicUrl 音乐URL
     * @return true表示已缓存
     */
    public static boolean isMusicCached(Context context, String musicUrl) {
        return getCachedMusicPath(context, musicUrl) != null;
    }

    /**
     * 获取音乐缓存目录
     */
    private static File getMusicCacheDir(Context context) {
        // 优先使用外部存储
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File externalDir = new File(context.getExternalFilesDir(null), MUSIC_CACHE_DIR);
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
            return externalDir;
        } else {
            // 使用内部存储
            File internalDir = new File(context.getFilesDir(), MUSIC_CACHE_DIR);
            if (!internalDir.exists()) {
                internalDir.mkdirs();
            }
            return internalDir;
        }
    }

    /**
     * 生成文件名
     */
    private static String generateFileName(String musicUrl) {
        // 使用URL的hash值作为文件名，避免特殊字符问题
        return "music_" + Math.abs(musicUrl.hashCode()) + ".mp3";
    }

    /**
     * 下载音乐文件
     */
    private static void downloadMusicFile(String musicUrl, File targetFile, MusicSaveCallback callback) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URL url = new URL(musicUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000); // 15秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            connection.setUseCaches(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP错误: " + responseCode);
            }

            int fileSize = connection.getContentLength();
            Log.d(TAG, "开始下载音乐文件，大小: " + fileSize + " 字节");

            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 报告进度
                if (callback != null && fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    callback.onProgress(progress);
                }
            }

            outputStream.flush();
            Log.d(TAG, "音乐文件下载完成: " + targetFile.getAbsolutePath() +
                      ", 大小: " + targetFile.length() + " 字节");

            if (callback != null) {
                callback.onSuccess(targetFile.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "下载音乐文件失败: " + e.getMessage(), e);
            // 删除不完整的文件
            if (targetFile.exists()) {
                targetFile.delete();
            }
            if (callback != null) {
                callback.onError("下载失败: " + e.getMessage());
            }
        } finally {
            // 关闭资源
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭资源失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 清理缓存目录
     * @param context 上下文
     */
    public static void clearCache(Context context) {
        executorService.execute(() -> {
            try {
                File cacheDir = getMusicCacheDir(context);
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    int deletedCount = 0;
                    for (File file : files) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                    Log.d(TAG, "清理缓存完成，删除了 " + deletedCount + " 个文件");
                }
            } catch (Exception e) {
                Log.e(TAG, "清理缓存失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 获取缓存大小
     * @param context 上下文
     * @return 缓存大小（字节）
     */
    public static long getCacheSize(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            File[] files = cacheDir.listFiles();
            if (files == null) {
                return 0;
            }

            long totalSize = 0;
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }
            return totalSize;
        } catch (Exception e) {
            Log.e(TAG, "获取缓存大小失败: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 检查是否允许下载音乐（解决4GB问题的关键检查）
     */
    private static boolean isMusicDownloadAllowed(Context context, String musicUrl, MusicSaveCallback callback) {
        try {
            // 1. 立即强制清理缓存（不等待3天）
            forceCleanupIfNeeded(context);

            // 2. 检查缓存限制
            if (!checkCacheLimits(context)) {
                Log.w(TAG, "音乐缓存超过限制，禁止下载");
                if (callback != null) {
                    callback.onError("音乐缓存已满，无法下载新文件");
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "检查音乐下载权限时出错", e);
            return false;
        }
    }

    /**
     * 强制清理缓存（不等待时间限制）
     */
    private static void forceCleanupIfNeeded(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            if (!cacheDir.exists()) return;

            // 获取当前缓存状态
            File[] files = cacheDir.listFiles();
            if (files == null) return;

            long totalSize = 0;
            int fileCount = 0;
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                    fileCount++;
                }
            }

            // 如果超过限制，立即清理
            if (totalSize > MAX_MUSIC_CACHE_SIZE || fileCount > MAX_MUSIC_FILES) {
                Log.w(TAG, "强制清理音乐缓存: 当前" + fileCount + "个文件，" +
                     formatFileSize(totalSize) + "超过限制");

                // 删除最旧的文件直到符合限制
                cleanMusicCacheToLimit(context);

                Log.w(TAG, "强制清理完成");
            }
        } catch (Exception e) {
            Log.e(TAG, "强制清理音乐缓存失败", e);
        }
    }

    /**
     * 检查缓存限制
     */
    private static boolean checkCacheLimits(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            if (!cacheDir.exists()) return true;

            File[] files = cacheDir.listFiles();
            if (files == null) return true;

            long totalSize = 0;
            int fileCount = 0;

            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                    fileCount++;

                    // 单个文件大小检查
                    if (file.length() > MAX_SINGLE_FILE_SIZE) {
                        Log.w(TAG, "发现超大音乐文件: " + file.getName() +
                                  " (" + formatFileSize(file.length()) + ")，删除");
                        file.delete();
                        totalSize -= file.length();
                        fileCount--;
                    }
                }
            }

            boolean withinLimits = totalSize <= MAX_MUSIC_CACHE_SIZE && fileCount <= MAX_MUSIC_FILES;

            if (!withinLimits) {
                Log.w(TAG, "缓存限制检查失败: " + fileCount + "个文件，" +
                     formatFileSize(totalSize) + " (限制: " + MAX_MUSIC_FILES + "个，" +
                     formatFileSize(MAX_MUSIC_CACHE_SIZE) + ")");
            }

            return withinLimits;

        } catch (Exception e) {
            Log.e(TAG, "检查缓存限制失败", e);
            return false;
        }
    }

    /**
     * 清理音乐缓存到限制范围内
     */
    private static void cleanMusicCacheToLimit(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            File[] files = cacheDir.listFiles();
            if (files == null) return;

            // 按修改时间排序（最旧的在前面）
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

            long totalSize = 0;
            int fileCount = 0;
            long cleanedSize = 0;
            int cleanedCount = 0;

            // 先计算当前总量
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                    fileCount++;
                }
            }

            // 删除最旧的文件直到符合限制
            for (File file : files) {
                if (!file.isFile()) continue;

                if (totalSize <= MAX_MUSIC_CACHE_SIZE && fileCount <= MAX_MUSIC_FILES) {
                    break;
                }

                long fileSize = file.length();
                if (file.delete()) {
                    totalSize -= fileSize;
                    fileCount--;
                    cleanedSize += fileSize;
                    cleanedCount++;
                    Log.d(TAG, "删除音乐文件: " + file.getName() +
                              " (" + formatFileSize(fileSize) + ")");
                }
            }

            Log.w(TAG, "音乐缓存清理完成: 删除了" + cleanedCount + "个文件，" +
                      "释放了" + formatFileSize(cleanedSize) +
                      "空间 (剩余: " + fileCount + "个文件，" + formatFileSize(totalSize) + ")");

        } catch (Exception e) {
            Log.e(TAG, "清理音乐缓存到限制失败", e);
        }
    }

    /**
     * 检查并执行必要的缓存清理
     */
    private static void checkAndCleanupIfNeeded(Context context) {
        try {
            CacheManager cacheManager = CacheManager.getInstance(context);
            if (cacheManager.shouldCleanup()) {
                Log.d(TAG, "触发音乐缓存清理");
                cacheManager.performCleanup();
            }
        } catch (Exception e) {
            Log.w(TAG, "检查缓存清理时出错", e);
        }
    }

    /**
     * 增强版清理缓存方法 - 支持回调
     */
    public static void clearCache(Context context, boolean forceClear) {
        if (forceClear) {
            // 强制清理
            Log.d(TAG, "强制清理音乐缓存");
            CacheManager.getInstance(context).forceCleanupAll();
        } else {
            // 智能清理
            CacheManager.getInstance(context).performCleanup();
        }
    }

    /**
     * 获取音乐缓存大小
     */
    public static long getMusicCacheSize(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            return getDirectorySize(cacheDir);
        } catch (Exception e) {
            Log.e(TAG, "获取音乐缓存大小失败", e);
            return 0;
        }
    }

    /**
     * 获取音乐缓存文件数量
     */
    public static int getMusicCacheFileCount(Context context) {
        try {
            File cacheDir = getMusicCacheDir(context);
            if (!cacheDir.exists()) return 0;
            File[] files = cacheDir.listFiles();
            return files != null ? files.length : 0;
        } catch (Exception e) {
            Log.e(TAG, "获取音乐缓存文件数量失败", e);
            return 0;
        }
    }

    /**
     * 获取目录大小
     */
    private static long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // 注意：缓存管理器会在CacheManager.getInstance()时自动管理生命周期
        // 不需要在这里手动关闭
    }
}