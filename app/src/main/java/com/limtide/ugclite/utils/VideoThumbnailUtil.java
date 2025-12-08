package com.limtide.ugclite.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * 视频缩略图工具类
 * 用于从视频URL提取封面图
 */
public class VideoThumbnailUtil {

    private static final String TAG = "VideoThumbnailUtil";

    /**
     * 从视频URL生成缩略图并保存到本地缓存
     * @param context 上下文
     * @param videoUrl 视频URL
     * @param thumbnailFile 缩略图文件
     * @param callback 回调接口
     */
    public static void generateThumbnail(@NonNull Context context,
                                       @NonNull String videoUrl,
                                       @NonNull File thumbnailFile,
                                       @Nullable ThumbnailCallback callback) {

        Log.d(TAG, "开始生成视频缩略图: " + videoUrl);

        // 使用Glide从视频提取第一帧
        Glide.with(context)
                .asBitmap()
                .load(videoUrl)
                .apply(new RequestOptions().frame(1000000)) // 提取第一帧（微秒）
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                               @Nullable Transition<? super Bitmap> transition) {
                        Log.d(TAG, "视频缩略图生成成功: " + resource.getWidth() + "x" + resource.getHeight());

                        // 保存缩略图到文件
                        saveBitmapToFile(resource, thumbnailFile);

                        if (callback != null) {
                            callback.onThumbnailReady(thumbnailFile.getAbsolutePath());
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 资源被清理时的回调
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "视频缩略图生成失败: " + videoUrl);

                        if (callback != null) {
                            callback.onThumbnailError(new Exception("Failed to generate thumbnail"));
                        }
                    }
                });
    }

    /**
     * 同步方式获取视频缩略图
     * @param context 上下文
     * @param videoUrl 视频URL
     * @return 缩略图文件路径，失败返回null
     */
    @Nullable
    public static String getThumbnailSync(@NonNull Context context, @NonNull String videoUrl) {
        try {
            Log.d(TAG, "同步获取视频缩略图: " + videoUrl);

            Bitmap bitmap = Glide.with(context)
                    .asBitmap()
                    .load(videoUrl)
                    .apply(new RequestOptions().frame(1000000))
                    .submit()
                    .get();

            if (bitmap != null) {
                Log.d(TAG, "同步获取视频缩略图成功: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                // 保存到缓存文件
                File cacheFile = getCacheFile(context, videoUrl);
                saveBitmapToFile(bitmap, cacheFile);
                return cacheFile.getAbsolutePath();
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "同步获取视频缩略图失败", e);
        }

        return null;
    }

    /**
     * 将Bitmap保存到文件
     */
    private static void saveBitmapToFile(@NonNull Bitmap bitmap, @NonNull File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            Log.d(TAG, "缩略图已保存到: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "保存缩略图失败: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 获取缓存文件
     */
    private static File getCacheFile(@NonNull Context context, @NonNull String videoUrl) {
        // 使用URL的hash值作为文件名
        String fileName = "thumb_" + Math.abs(videoUrl.hashCode()) + ".jpg";
        return new File(context.getCacheDir(), fileName);
    }

    /**
     * 获取缓存的缩略图路径
     * @param context 上下文
     * @param videoUrl 视频URL
     * @return 缓存文件路径，如果不存在返回null
     */
    @Nullable
    public static String getCachedThumbnail(@NonNull Context context, @NonNull String videoUrl) {
        File cacheFile = getCacheFile(context, videoUrl);
        return cacheFile.exists() ? cacheFile.getAbsolutePath() : null;
    }

    /**
     * 快速预加载视频缩略图 - 优化版本
     * @param context 上下文
     * @param videoUrl 视频URL
     * @param targetImageView 目标ImageView（可选）
     */
    public static void preloadThumbnail(@NonNull Context context,
                                      @NonNull String videoUrl,
                                      @Nullable android.widget.ImageView targetImageView) {

        Log.d(TAG, "快速预加载视频缩略图: " + videoUrl);

        // 先检查是否有缓存
        String cachedPath = getCachedThumbnail(context, videoUrl);
        if (cachedPath != null) {
            Log.d(TAG, "使用缓存的缩略图: " + cachedPath);
            if (targetImageView != null) {
                // 优先使用缓存，并设置优化的Glide选项
                Glide.with(context)
                        .load(cachedPath)
                        .placeholder(android.R.drawable.ic_media_play)
                        .error(android.R.drawable.ic_media_play)
                        .dontAnimate() // 禁用动画以提升速度
                        .centerCrop()
                        .into(targetImageView);
            }
            return;
        }

        Log.d(TAG, "没有缓存，开始快速生成缩略图: " + videoUrl);

        if (targetImageView != null) {
            // 使用更优化的选项直接加载到ImageView
            Glide.with(context)
                    .load(videoUrl)
                    .apply(new RequestOptions()
                            .frame(1000000) // 提取第一帧
                            .centerCrop()
                            .override(400, 400) // 限制尺寸提升速度
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                            .skipMemoryCache(false) // 启用内存缓存
                    )
                    .placeholder(android.R.drawable.ic_media_play) // 使用系统播放图标作为占位图
                    .error(android.R.drawable.ic_media_play)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                  Object model,
                                                  com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                  boolean isFirstResource) {
                            Log.w(TAG, "快速缩略图加载失败，回退到异步生成: " + videoUrl);
                            // 快速加载失败时，启动异步生成
                            generateThumbnailAsync(context, videoUrl);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                     Object model,
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                     com.bumptech.glide.load.DataSource dataSource,
                                                     boolean isFirstResource) {
                            Log.d(TAG, "快速缩略图加载成功: " + videoUrl);
                            // 成功加载后，启动异步缓存生成
                            generateThumbnailAsync(context, videoUrl);
                            return false;
                        }
                    })
                    .into(targetImageView);
        } else {
            // 仅异步生成缓存
            generateThumbnailAsync(context, videoUrl);
        }
    }

    /**
     * 异步生成缩略图（后台任务，不影响UI）
     */
    private static void generateThumbnailAsync(@NonNull Context context, @NonNull String videoUrl) {
        // 在后台线程中生成缓存
        new Thread(() -> {
            try {
                File cacheFile = getCacheFile(context, videoUrl);
                if (!cacheFile.exists()) {
                    generateThumbnail(context, videoUrl, cacheFile, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "异步生成缩略图失败: " + videoUrl, e);
            }
        }).start();
    }

    /**
     * 缩略图生成回调接口
     */
    public interface ThumbnailCallback {
        void onThumbnailReady(@NonNull String thumbnailPath);
        void onThumbnailError(@NonNull Exception error);
    }

    /**
     * 清理所有缓存的缩略图
     * @param context 上下文
     */
    public static void clearThumbnailCache(@NonNull Context context) {
        File cacheDir = context.getCacheDir();
        File[] thumbnailFiles = cacheDir.listFiles((dir, name) ->
            name.startsWith("thumb_") && name.endsWith(".jpg"));

        if (thumbnailFiles != null) {
            for (File file : thumbnailFiles) {
                if (file.delete()) {
                    Log.d(TAG, "删除缩略图缓存: " + file.getName());
                }
            }
        }
    }
}