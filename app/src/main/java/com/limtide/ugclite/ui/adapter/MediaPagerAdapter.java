package com.limtide.ugclite.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ItemMediaImageBinding;
import com.limtide.ugclite.databinding.ItemMediaVideoBinding;
import com.limtide.ugclite.ui.component.VideoPlayerView;
import com.limtide.ugclite.utils.VideoThumbnailUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 媒体适配器 - 用于详情页ViewPager2
 * 支持图片和视频片段
 */
public class MediaPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaPagerAdapter";
    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_VIDEO = 1;

    private Context context;
    private List<Post.Clip> mediaClips;
    private float firstClipAspectRatio; // 首图比例，用于容器高度计算
    private OnMediaClickListener clickListener;

    public interface OnMediaClickListener {
        void onImageClick(Post.Clip clip, int position);
        void onVideoClick(Post.Clip clip, int position);
        void onVideoPlay(Post.Clip clip, int position);
        void onVideoPause(Post.Clip clip, int position);
    }

    public MediaPagerAdapter(Context context, List<Post.Clip> mediaClips) {
        this.context = context;
        this.mediaClips = mediaClips;
        this.firstClipAspectRatio = calculateFirstClipAspectRatio();
    }

    public MediaPagerAdapter(Context context, List<Post.Clip> mediaClips, float firstClipAspectRatio) {
        this.context = context;
        this.mediaClips = mediaClips;
        this.firstClipAspectRatio = firstClipAspectRatio;
    }

    public void setOnMediaClickListener(OnMediaClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 计算首图比例
     */
    private float calculateFirstClipAspectRatio() {
        if (mediaClips == null || mediaClips.isEmpty()) {
            return 1.0f;
        }
        Post.Clip firstClip = mediaClips.get(0);
        float aspectRatio = firstClip.getAspectRatio();
        // 限制在3:4 ~ 16:9之间
        return Math.max(0.75f, Math.min(1.78f, aspectRatio));
    }

    @Override
    public int getItemViewType(int position) {
        if (mediaClips == null || position >= mediaClips.size()) {
            return VIEW_TYPE_IMAGE; // 默认返回图片类型
        }
        Post.Clip clip = mediaClips.get(position);
        return clip.type == 1 ? VIEW_TYPE_VIDEO : VIEW_TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_VIDEO) {
            ItemMediaVideoBinding binding = ItemMediaVideoBinding.inflate(inflater, parent, false);
            return new VideoViewHolder(binding);
        } else {
            ItemMediaImageBinding binding = ItemMediaImageBinding.inflate(inflater, parent, false);
            return new ImageViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mediaClips == null || position >= mediaClips.size()) {
            return;
        }

        Post.Clip clip = mediaClips.get(position);

        if (holder instanceof VideoViewHolder) {
            bindVideo((VideoViewHolder) holder, clip, position);
        } else if (holder instanceof ImageViewHolder) {
            bindImage((ImageViewHolder) holder, clip);
        }

        Log.d(TAG, "Binding media at position " + position + ", type: " + clip.type);
    }

    /**
     * 绑定图片数据
     * 所有图片都按照首图比例显示，确保充满容器
     */
    private void bindImage(ImageViewHolder holder, Post.Clip clip) {
        // 设置ImageView为match_parent，填充ViewPager容器
        android.view.ViewGroup.LayoutParams params = holder.binding.mediaImage.getLayoutParams();
        params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        holder.binding.mediaImage.setLayoutParams(params);

        // 设置ScaleType为centerCrop，确保图片充满容器
        holder.binding.mediaImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

        // 设置点击监听器
        holder.binding.mediaImage.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(clip, holder.getAdapterPosition());
            }
        });

        // 显示加载状态
        showLoadingState(holder);

        // 加载图片
        if (clip.url != null && !clip.url.isEmpty()) {
            Glide.with(context)
                    .load(clip.url)
                    .placeholder(R.drawable.ic_empty_state)
                    .error(R.drawable.ic_empty_state)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            // 加载失败，显示失败态
                            showErrorState(holder);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            // 加载成功，隐藏加载状态
                            hideLoadingState(holder);
                            return false;
                        }
                    })
                    .centerCrop()
                    .into(holder.binding.mediaImage);

            Log.d(TAG, "Loading image: " + clip.url + " using first clip ratio: " + firstClipAspectRatio);
        } else {
            // 没有图片URL，显示错误状态
            showErrorState(holder);
        }
    }

    /**
     * 显示加载状态
     */
    private void showLoadingState(ImageViewHolder holder) {
        // 显示加载进度条
        if (holder.binding.loadingProgress != null) {
            holder.binding.loadingProgress.setVisibility(android.view.View.VISIBLE);
        }
        // 设置占位图
        holder.binding.mediaImage.setImageResource(R.drawable.ic_empty_state);
    }

    /**
     * 隐藏加载状态
     */
    private void hideLoadingState(ImageViewHolder holder) {
        // 隐藏加载进度条
        if (holder.binding.loadingProgress != null) {
            holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
        }
    }

    /**
     * 显示错误状态
     */
    private void showErrorState(ImageViewHolder holder) {
        // 隐藏加载进度条
        if (holder.binding.loadingProgress != null) {
            holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
        }
        // 显示错误图片
        holder.binding.mediaImage.setImageResource(R.drawable.ic_empty_state);
        Log.w(TAG, "Image load failed, showing error state");
    }

    /**
     * 加载视频封面
     */
    private void loadVideoThumbnail(VideoViewHolder holder, String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            // 如果没有视频URL，显示默认占位图
            holder.binding.videoThumbnail.setImageResource(R.drawable.ic_empty_state);
            holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
            return;
        }

        Log.d(TAG, "加载视频封面: " + videoUrl);

        // 使用VideoThumbnailUtil加载封面
        VideoThumbnailUtil.preloadThumbnail(context, videoUrl, holder.binding.videoThumbnail);

        // 设置封面加载监听器 - 使用Glide的监听器
        Glide.with(context)
                .load(videoUrl)
                .apply(new com.bumptech.glide.request.RequestOptions().frame(1000000))
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                              Object model,
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                              boolean isFirstResource) {
                        // 封面加载失败，显示占位图并隐藏加载指示器
                        holder.binding.videoThumbnail.setImageResource(R.drawable.ic_empty_state);
                        holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
                        Log.w(TAG, "视频封面加载失败: " + videoUrl);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                 Object model,
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                 com.bumptech.glide.load.DataSource dataSource,
                                                 boolean isFirstResource) {
                        // 封面加载完成，隐藏加载指示器
                        holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
                        return false;
                    }
                })
                .into(holder.binding.videoThumbnail);

        // 异步生成缓存缩略图（如果还没有缓存的话）
        String cachedPath = VideoThumbnailUtil.getCachedThumbnail(context, videoUrl);
        if (cachedPath == null) {
            VideoThumbnailUtil.generateThumbnail(context, videoUrl,
                new java.io.File(context.getCacheDir(), "thumb_" + Math.abs(videoUrl.hashCode()) + ".jpg"),
                new VideoThumbnailUtil.ThumbnailCallback() {
                    @Override
                    public void onThumbnailReady(@NonNull String thumbnailPath) {
                        Log.d(TAG, "视频缩略图生成完成: " + thumbnailPath);
                    }

                    @Override
                    public void onThumbnailError(@NonNull Exception error) {
                        Log.w(TAG, "视频缩略图生成失败: " + videoUrl, error);
                    }
                });
        }
    }

    /**
     * 绑定视频数据
     */
    private void bindVideo(VideoViewHolder holder, Post.Clip clip, int position) {
        Log.d(TAG, "开始绑定视频数据: " + clip.url + ", position: " + position);

        // 设置VideoPlayerView的尺寸
        android.view.ViewGroup.LayoutParams params = holder.binding.videoContainer.getLayoutParams();
        params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        holder.binding.videoContainer.setLayoutParams(params);

        // 重置封面状态
        holder.binding.videoThumbnail.setVisibility(android.view.View.VISIBLE);
        holder.binding.playButton.setVisibility(android.view.View.VISIBLE);
        holder.binding.loadingProgress.setVisibility(android.view.View.VISIBLE);

        // 加载视频封面
        loadVideoThumbnail(holder, clip.url);

        // 清理之前的视频播放器
        if (holder.videoPlayerView != null) {
            Log.d(TAG, "清理之前的VideoPlayerView");
            holder.videoPlayerView.release();
            holder.binding.videoContainer.removeView(holder.videoPlayerView);
            videoPlayerViews.remove(holder.videoPlayerView);
            holder.videoPlayerView = null;
        }

        // 创建新的VideoPlayerView
        Log.d(TAG, "创建新的VideoPlayerView for URL: " + clip.url);
        holder.videoPlayerView = new VideoPlayerView(context);

        // 添加到容器
        android.view.ViewGroup.LayoutParams playerParams =
            new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
        holder.binding.videoContainer.addView(holder.videoPlayerView, playerParams);

        // 添加到跟踪列表
        addVideoPlayerView(holder.videoPlayerView);

        // 设置视频URL
        if (clip.url != null && !clip.url.isEmpty()) {
            Log.d(TAG, "设置视频URL: " + clip.url);
            holder.videoPlayerView.setVideoUrl(clip.url);

            // 检查是否有保存的状态需要恢复
            VideoState savedState = videoStates.get(clip.url);
            if (savedState != null) {
                Log.d(TAG, "恢复视频状态 - position: " + savedState.position + ", isPlaying: " + savedState.isPlaying);

                // 创建状态恢复监听器
                androidx.media3.common.Player.Listener stateRestoreListener = new androidx.media3.common.Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == androidx.media3.common.Player.STATE_READY) {
                            Log.d(TAG, "视频准备就绪，恢复状态 - position: " + savedState.position);
                            if (savedState.position > 0) {
                                holder.videoPlayerView.seekTo(savedState.position);
                            }
                            if (savedState.isPlaying) {
                                holder.videoPlayerView.start();
                            }
                            // 移除监听器避免重复调用
                            holder.videoPlayerView.removeOnPreparedListener(this);
                        }
                    }

                    @Override
                    public void onPlayerError(androidx.media3.common.PlaybackException error) {
                        Log.e(TAG, "视频播放错误", error);
                        // 移除监听器避免重复调用
                        holder.videoPlayerView.removeOnPreparedListener(this);
                    }
                };

                // 添加状态恢复监听器
                holder.videoPlayerView.setOnPreparedListener(stateRestoreListener);
            }

            // 设置视频事件监听器
            holder.videoPlayerView.setOnVideoEventListener(new VideoPlayerView.OnVideoEventListener() {
                @Override
                public void onVideoStarted() {
                    Log.d(TAG, "视频开始播放: " + clip.url);

                    // 视频开始播放时隐藏封面和播放按钮
                    holder.binding.videoThumbnail.setVisibility(android.view.View.GONE);
                    holder.binding.playButton.setVisibility(android.view.View.GONE);
                    holder.binding.loadingProgress.setVisibility(android.view.View.GONE);

                    if (clickListener != null) {
                        clickListener.onVideoPlay(clip, position);
                    }
                }

                @Override
                public void onVideoPaused() {
                    Log.d(TAG, "视频暂停: " + clip.url);

                    // 视频暂停时显示封面和播放按钮
                    holder.binding.videoThumbnail.setVisibility(android.view.View.VISIBLE);
                    holder.binding.playButton.setVisibility(android.view.View.VISIBLE);

                    if (clickListener != null) {
                        clickListener.onVideoPause(clip, position);
                    }
                }

                @Override
                public void onVideoEnded() {
                    Log.d(TAG, "视频播放结束: " + clip.url);

                    // 视频播放结束时显示封面和播放按钮
                    holder.binding.videoThumbnail.setVisibility(android.view.View.VISIBLE);
                    holder.binding.playButton.setVisibility(android.view.View.VISIBLE);
                }

                @Override
                public void onVideoError(Exception error) {
                    Log.e(TAG, "视频播放错误: " + clip.url, error);

                    // 视频播放错误时显示封面
                    holder.binding.videoThumbnail.setVisibility(android.view.View.VISIBLE);
                    holder.binding.playButton.setVisibility(android.view.View.VISIBLE);
                    holder.binding.loadingProgress.setVisibility(android.view.View.GONE);
                }
            });

            // 设置点击监听器
            holder.binding.videoContainer.setOnClickListener(v -> {
                Log.d(TAG, "视频容器被点击: " + clip.url);
                if (clickListener != null) {
                    clickListener.onVideoClick(clip, position);
                }

                // 切换播放/暂停状态
                if (holder.videoPlayerView.isPlaying()) {
                    Log.d(TAG, "暂停视频: " + clip.url);
                    holder.videoPlayerView.pause();
                } else {
                    Log.d(TAG, "开始播放视频: " + clip.url);
                    holder.videoPlayerView.start();
                }
            });

            Log.d(TAG, "视频数据绑定完成: " + clip.url + ", position: " + position + ", 新创建的播放器: " + (holder.videoPlayerView != null));
        } else {
            Log.w(TAG, "视频URL为空，position: " + position);
        }
    }



    @Override
    public int getItemCount() {
        return mediaClips != null ? mediaClips.size() : 0;
    }

    /**
     * 图片ViewHolder
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ItemMediaImageBinding binding;

        public ImageViewHolder(@NonNull ItemMediaImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * 视频ViewHolder
     */
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ItemMediaVideoBinding binding;
        VideoPlayerView videoPlayerView;

        public VideoViewHolder(@NonNull ItemMediaVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 获取视频播放器视图
         */
        public VideoPlayerView getVideoPlayerView() {
            return videoPlayerView;
        }
    }

    // 用于跟踪所有活跃的VideoPlayerView实例
    private List<VideoPlayerView> videoPlayerViews = new ArrayList<>();

    // 保存视频状态信息的映射
    private java.util.Map<String, VideoState> videoStates = new java.util.HashMap<>();

    /**
     * 视频状态信息类
     */
    private static class VideoState {
        long position;      // 播放位置
        boolean isPlaying;  // 是否正在播放
        boolean isMuted;    // 是否静音

        VideoState(long position, boolean isPlaying, boolean isMuted) {
            this.position = position;
            this.isPlaying = isPlaying;
            this.isMuted = isMuted;
        }
    }

    /**
     * 暂停所有视频播放
     */
    public void pauseAllVideos() {
        Log.d(TAG, "暂停所有视频播放，当前有 " + videoPlayerViews.size() + " 个视频实例");
        for (VideoPlayerView videoPlayer : videoPlayerViews) {
            if (videoPlayer != null && videoPlayer.isPlaying()) {
                videoPlayer.pause();
            }
        }
    }

    /**
     * 释放所有视频资源
     */
    public void releaseAllVideos() {
        Log.d(TAG, "释放所有视频资源，当前有 " + videoPlayerViews.size() + " 个视频实例");
        for (VideoPlayerView videoPlayer : videoPlayerViews) {
            if (videoPlayer != null) {
                videoPlayer.release();
            }
        }
        videoPlayerViews.clear();
        // 清理保存的状态
        videoStates.clear();
        Log.d(TAG, "清理所有视频状态信息");
    }


    /**
     * 彻底移除VideoPlayerView
     */
    private void removeVideoPlayerView(VideoPlayerView videoPlayerView) {
        if (videoPlayerView != null) {
            videoPlayerView.release();
            videoPlayerViews.remove(videoPlayerView);

            // 从父容器中移除
            if (videoPlayerView.getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) videoPlayerView.getParent();
                parent.removeView(videoPlayerView);
            }

            Log.d(TAG, "彻底移除VideoPlayerView，剩余实例: " + videoPlayerViews.size());
        }
    }

    /**
     * 添加视频播放器实例到跟踪列表
     */
    public void addVideoPlayerView(VideoPlayerView videoPlayerView) {
        if (videoPlayerView != null && !videoPlayerViews.contains(videoPlayerView)) {
            videoPlayerViews.add(videoPlayerView);
            Log.d(TAG, "添加视频播放器实例，当前总数: " + videoPlayerViews.size());
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VideoViewHolder) {
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            if (videoHolder.videoPlayerView != null) {
                // 保存当前视频状态
                String videoUrl = videoHolder.videoPlayerView.getVideoUrl();
                if (videoUrl != null) {
                    long currentPosition = videoHolder.videoPlayerView.getCurrentPosition();
                    boolean isPlaying = videoHolder.videoPlayerView.isPlaying();
                    boolean isMuted = videoHolder.videoPlayerView.isMuted();

                    VideoState state = new VideoState(currentPosition, isPlaying, isMuted);
                    videoStates.put(videoUrl, state);

                    Log.d(TAG, "保存视频状态 - URL: " + videoUrl +
                              ", position: " + currentPosition +
                              ", isPlaying: " + isPlaying +
                              ", isMuted: " + isMuted);
                }

                // 从跟踪列表中移除
                videoPlayerViews.remove(videoHolder.videoPlayerView);

                // 完全释放视频播放器资源
                videoHolder.videoPlayerView.release();

                // 从父容器中移除
                if (videoHolder.videoPlayerView.getParent() instanceof android.view.ViewGroup) {
                    android.view.ViewGroup parent = (android.view.ViewGroup) videoHolder.videoPlayerView.getParent();
                    parent.removeView(videoHolder.videoPlayerView);
                }

                // 清空holder的引用
                videoHolder.videoPlayerView = null;

                Log.d(TAG, "VideoViewHolder被回收，保存视频状态，剩余实例: " + videoPlayerViews.size());
            }
        }
    }

}