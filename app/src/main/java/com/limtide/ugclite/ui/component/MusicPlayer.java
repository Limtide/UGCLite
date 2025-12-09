package com.limtide.ugclite.ui.component;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import com.limtide.ugclite.utils.MusicFileUtils;

import java.io.IOException;

/**
 * MP3音乐播放器组件
 * 支持在线MP3流媒体播放
 */
public class MusicPlayer {

    private static final String TAG = "MusicPlayer";

    private MediaPlayer mediaPlayer;
    private Context context;
    private String currentUrl;
    private boolean isPrepared = false;
    private int startPosition = 0;

    // 播放状态监听器
    public interface MusicPlayerListener {
        void onPrepared();
        void onCompletion();
        void onError(String error);
        void onPlay();
        void onPause();
        void onStop();
    }

    private MusicPlayerListener listener;

    public MusicPlayer(Context context) {
        this.context = context.getApplicationContext();
        mediaPlayer = new MediaPlayer();
        initMediaPlayer();
    }

    /**
     * 初始化MediaPlayer
     */
    private void initMediaPlayer() {
        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer准备完成");
            isPrepared = true;
            if (listener != null) {
                listener.onPrepared();
            }
            // 如果有起始位置，从指定位置开始播放
            if (startPosition > 0) {
                mediaPlayer.seekTo(startPosition);
            }
            mediaPlayer.start();
            if (listener != null) {
                listener.onPlay();
            }
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "播放完成");
            if (listener != null) {
                listener.onCompletion();
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "播放错误 - what: " + what + ", extra: " + extra);
            String errorMsg = "播放错误 (" + what + ":" + extra + ")";
            if (listener != null) {
                listener.onError(errorMsg);
            }
            return true; // 错误已处理
        });

        mediaPlayer.setVolume(1.0f, 1.0f); // 默认音量
    }

    /**
     * 设置音量
     * @param volume 音量值 0-100
     */
    public void setVolume(int volume) {
        if (volume < 0) volume = 0;
        if (volume > 100) volume = 100;

        float volumeFloat = volume / 100.0f;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumeFloat, volumeFloat);
        }
        Log.d(TAG, "设置音量: " + volume + "% (" + volumeFloat + ")");
    }

    /**
     * 加载并准备播放音乐
     * @param url 音乐URL
     * @param seekTime 起始播放位置（毫秒）
     */
    public void loadMusic(String url, int seekTime) {
        loadMusic(url, seekTime, true);
    }

    /**
     * 加载并准备播放音乐
     * @param url 音乐URL
     * @param seekTime 起始播放位置（毫秒）
     * @param enableCache 是否启用缓存
     */
    public void loadMusic(String url, int seekTime, boolean enableCache) {
        Log.d(TAG, "加载音乐: " + url + ", 起始位置: " + seekTime + "ms, 缓存: " + enableCache);

        if (url == null || url.isEmpty()) {
            Log.w(TAG, "音乐URL为空");
            if (listener != null) {
                listener.onError("音乐URL为空");
            }
            return;
        }

        // 如果是相同的URL且已经在播放，则不做任何操作
        if (url.equals(currentUrl) && isPlaying()) {
            Log.d(TAG, "相同音乐已在播放中");
            return;
        }

        // 重置播放器
        reset();
        currentUrl = url;
        startPosition = seekTime;

        // 如果启用缓存，先检查本地缓存
        if (enableCache) {
            String cachedPath = MusicFileUtils.getCachedMusicPath(context, url);
            if (cachedPath != null) {
                Log.d(TAG, "使用缓存文件: " + cachedPath);
                loadMusicFromPath(cachedPath);
                return;
            }

            // 异步下载并缓存
            MusicFileUtils.saveMusicToLocal(context, url, new MusicFileUtils.MusicSaveCallback() {
                @Override
                public void onSuccess(String filePath) {
                    Log.d(TAG, "音乐缓存完成: " + filePath);
                    loadMusicFromPath(filePath);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "音乐缓存失败，使用在线播放: " + error);
                    loadMusicFromUrl(url);
                }

                @Override
                public void onProgress(int progress) {
                    // 可以在这里添加进度回调
                    Log.d(TAG, "音乐下载进度: " + progress + "%");
                }
            });
        } else {
            // 直接在线播放
            loadMusicFromUrl(url);
        }
    }

    /**
     * 从本地文件加载音乐
     */
    private void loadMusicFromPath(String filePath) {
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync();
            Log.d(TAG, "从本地文件加载音乐: " + filePath);
        } catch (IOException e) {
            Log.e(TAG, "设置本地音频源失败: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("音频源设置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 从URL加载音乐
     */
    private void loadMusicFromUrl(String url) {
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            Log.d(TAG, "从URL加载音乐: " + url);
        } catch (IOException e) {
            Log.e(TAG, "设置网络音频源失败: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("音频源设置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 开始播放
     */
    public void play() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            Log.d(TAG, "开始播放");
            if (listener != null) {
                listener.onPlay();
            }
        } else {
            Log.w(TAG, "播放器未准备好，无法播放");
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "暂停播放");
            if (listener != null) {
                listener.onPause();
            }
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            Log.d(TAG, "停止播放");
            if (listener != null) {
                listener.onStop();
            }
        }
    }

    /**
     * 重置播放器
     */
    public void reset() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            isPrepared = false;
            currentUrl = null;
            startPosition = 0;
            Log.d(TAG, "重置播放器");
        }
    }

    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * 检查是否已准备好
     */
    public boolean isPrepared() {
        return isPrepared;
    }

    /**
     * 获取当前播放位置
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 获取音乐总时长
     */
    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    /**
     * 跳转到指定位置
     */
    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
            Log.d(TAG, "跳转到位置: " + position + "ms");
        }
    }

    /**
     * 静音/取消静音
     */
    public void setMuted(boolean muted) {
        if (mediaPlayer != null) {
            if (muted) {
                mediaPlayer.setVolume(0f, 0f);
                Log.d(TAG, "已静音");
            } else {
                mediaPlayer.setVolume(1.0f, 1.0f);
                Log.d(TAG, "已取消静音");
            }
        }
    }

    /**
     * 设置播放监听器
     */
    public void setMusicPlayerListener(MusicPlayerListener listener) {
        this.listener = listener;
    }

    /**
     * 获取当前播放的URL
     */
    public String getCurrentUrl() {
        return currentUrl;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPrepared = false;
        currentUrl = null;
        startPosition = 0;
        Log.d(TAG, "释放音乐播放器资源");
    }
}