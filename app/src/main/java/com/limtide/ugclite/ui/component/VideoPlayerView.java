package com.limtide.ugclite.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.PlaybackException;

import com.limtide.ugclite.R;

/**
 * 自定义视频播放器组件
 * 基于ExoPlayer实现，支持自适应布局和播放控制
 */
public class VideoPlayerView extends FrameLayout implements Player.Listener {

    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private String videoUrl;
    private boolean isPrepared = false;
    private OnVideoEventListener listener;

    public interface OnVideoEventListener {
        void onVideoStarted();
        void onVideoPaused();
        void onVideoEnded();
        void onVideoError(Exception error);
    }

    public VideoPlayerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.component_video_player, this, true);
        playerView = findViewById(R.id.player_view);

        // 禁用自动播放控制器，使用自定义控制
        playerView.setControllerAutoShow(false);
        playerView.setUseController(false);

        initializePlayer();
    }

    /**
     * 初始化ExoPlayer
     */
    private void initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(getContext()).build();
            exoPlayer.addListener(this);
            playerView.setPlayer(exoPlayer);

            // 设置默认音量
            exoPlayer.setVolume(1.0f);

            // 设置循环播放
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
    }

    /**
     * 设置视频URL并准备播放
     */
    public void setVideoUrl(String url) {
        Log.d("VideoPlayerView", "设置视频URL: " + url);
        this.videoUrl = url;

        if (exoPlayer == null) {
            Log.w("VideoPlayerView", "ExoPlayer未初始化，重新初始化");
            initializePlayer();
        }

        if (url != null && !url.isEmpty()) {
            // 停止当前播放
            exoPlayer.stop();

            // 设置新媒体项
            MediaItem mediaItem = MediaItem.fromUri(url);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            isPrepared = true;

            Log.d("VideoPlayerView", "视频URL设置完成并准备播放");
        } else {
            Log.w("VideoPlayerView", "视频URL为空");
        }
    }

    /**
     * 开始播放视频
     */
    public void start() {
        if (exoPlayer != null && isPrepared) {
            exoPlayer.setPlayWhenReady(true);
            if (listener != null) {
                listener.onVideoStarted();
            }
        }
    }

    /**
     * 暂停播放视频
     */
    public void pause() {
        if (exoPlayer != null && isPrepared) {
            exoPlayer.setPlayWhenReady(false);
            if (listener != null) {
                listener.onVideoPaused();
            }
        }
    }

    /**
     * 释放播放器资源
     */
    public void release() {
        Log.d("VideoPlayerView", "释放VideoPlayerView资源");
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
            isPrepared = false;
        }
        videoUrl = null;
        listener = null;
        Log.d("VideoPlayerView", "VideoPlayerView资源释放完成");
    }

    /**
     * 设置音量
     * @param volume 音量值 (0.0f - 1.0f)
     */
    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(Math.max(0.0f, Math.min(1.0f, volume)));
        }
    }

    /**
     * 设置静音
     */
    public void setMuted(boolean muted) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(muted ? 0.0f : 1.0f);
        }
    }

    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    /**
     * 检查是否已准备完成
     */
    public boolean isPrepared() {
        return isPrepared;
    }

    /**
     * 设置视频事件监听器
     */
    public void setOnVideoEventListener(OnVideoEventListener listener) {
        this.listener = listener;
    }

    /**
     * 获取视频URL
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    /**
     * 获取当前播放位置
     */
    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    /**
     * 检查是否静音
     */
    public boolean isMuted() {
        return exoPlayer != null && exoPlayer.getVolume() == 0.0f;
    }

    /**
     * 设置准备监听器
     */
    public void setOnPreparedListener(androidx.media3.common.Player.Listener listener) {
        if (exoPlayer != null) {
            exoPlayer.addListener(listener);
        }
    }

    /**
     * 移除准备监听器
     */
    public void removeOnPreparedListener(androidx.media3.common.Player.Listener listener) {
        if (exoPlayer != null) {
            exoPlayer.removeListener(listener);
        }
    }

    /**
     * 获取视频总时长
     */
    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }

    /**
     * 跳转到指定位置
     */
    public void seekTo(long positionMs) {
        if (exoPlayer != null && isPrepared) {
            exoPlayer.seekTo(positionMs);
        }
    }

    // Player.Listener 接口实现
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                // 播放器已准备好
                break;

            case Player.STATE_ENDED:
                // 播放结束
                if (listener != null) {
                    listener.onVideoEnded();
                }
                break;

            case Player.STATE_BUFFERING:
                // 缓冲中
                break;

            case Player.STATE_IDLE:
                // 空闲状态
                break;
        }
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        if (playWhenReady) {
            if (listener != null) {
                listener.onVideoStarted();
            }
        } else {
            if (listener != null) {
                listener.onVideoPaused();
            }
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        if (listener != null) {
            listener.onVideoError(new Exception(error));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }
}