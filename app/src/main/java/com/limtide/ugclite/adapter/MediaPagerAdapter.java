package com.limtide.ugclite.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ItemMediaImageBinding;
import com.limtide.ugclite.databinding.ItemMediaVideoBinding;

import java.util.List;

/**
 * 媒体适配器 - 用于详情页ViewPager2
 * 支持图片和视频片段
 */
public class MediaPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaPagerAdapter";
    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_VIDEO = 1;

    private Context context;
    private List<Post.Clip> mediaClips;

    public MediaPagerAdapter(Context context, List<Post.Clip> mediaClips) {
        this.context = context;
        this.mediaClips = mediaClips;
    }

    @Override
    public int getItemViewType(int position) {
        if (mediaClips == null || position >= mediaClips.size()) {
            return TYPE_IMAGE;
        }
        Post.Clip clip = mediaClips.get(position);
        return clip.type; // 0: 图片, 1: 视频
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_IMAGE) {
            ItemMediaImageBinding binding = ItemMediaImageBinding.inflate(inflater, parent, false);
            return new ImageViewHolder(binding);
        } else {
            ItemMediaVideoBinding binding = ItemMediaVideoBinding.inflate(inflater, parent, false);
            return new VideoViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mediaClips == null || position >= mediaClips.size()) {
            return;
        }

        Post.Clip clip = mediaClips.get(position);

        if (holder instanceof ImageViewHolder) {
            bindImage((ImageViewHolder) holder, clip);
        } else if (holder instanceof VideoViewHolder) {
            bindVideo((VideoViewHolder) holder, clip);
        }

        Log.d(TAG, "Binding media at position " + position + ", type: " + clip.type);
    }

    /**
     * 绑定图片数据
     */
    private void bindImage(ImageViewHolder holder, Post.Clip clip) {
        // 计算适合的图片高度（3:4 ~ 16:9 比例）
        int targetWidth = holder.binding.mediaImage.getWidth();
        if (targetWidth <= 0) {
            targetWidth = 1080; // 默认宽度
        }

        float aspectRatio = clip.getAspectRatio();
        int targetHeight = (int) (targetWidth / Math.max(aspectRatio, 0.5f)); // 限制最小比例2:1
        targetHeight = Math.min(targetHeight, 2400); // 限制最大高度

        // 设置ImageView尺寸
        android.view.ViewGroup.LayoutParams params = holder.binding.mediaImage.getLayoutParams();
        params.width = targetWidth;
        params.height = targetHeight;
        holder.binding.mediaImage.setLayoutParams(params);

        // 加载图片
        if (clip.url != null && !clip.url.isEmpty()) {
            Glide.with(context)
                    .load(clip.url)
                    .placeholder(R.drawable.ic_empty_state)
                    .error(R.drawable.ic_empty_state)
                    .override(targetWidth, targetHeight)
                    .centerCrop()
                    .into(holder.binding.mediaImage);
        } else {
            holder.binding.mediaImage.setImageResource(R.drawable.ic_empty_state);
        }

        Log.d(TAG, "Loaded image: " + clip.url + ", size: " + targetWidth + "x" + targetHeight);
    }

    /**
     * 绑定视频数据
     */
    private void bindVideo(VideoViewHolder holder, Post.Clip clip) {
        // 计算适合的视频高度
        int targetWidth = holder.binding.videoView.getWidth();
        if (targetWidth <= 0) {
            targetWidth = 1080; // 默认宽度
        }

        float aspectRatio = clip.getAspectRatio();
        int targetHeight = (int) (targetWidth / Math.max(aspectRatio, 0.5f));
        targetHeight = Math.min(targetHeight, 2400);

        // 设置VideoView尺寸
        android.view.ViewGroup.LayoutParams params = holder.binding.videoView.getLayoutParams();
        params.width = targetWidth;
        params.height = targetHeight;
        holder.binding.videoView.setLayoutParams(params);

        // 设置视频封面尺寸
        android.view.ViewGroup.LayoutParams thumbnailParams = holder.binding.videoThumbnail.getLayoutParams();
        thumbnailParams.width = targetWidth;
        thumbnailParams.height = targetHeight;
        holder.binding.videoThumbnail.setLayoutParams(thumbnailParams);

        // 停止之前的播放
        holder.binding.videoView.stopPlayback();

        // 设置视频源但不自动播放
        if (clip.url != null && !clip.url.isEmpty()) {
            holder.binding.videoView.setVideoPath(clip.url);

            // 显示封面和播放按钮
            holder.binding.videoThumbnail.setVisibility(View.VISIBLE);
            holder.binding.playButton.setVisibility(View.VISIBLE);

            // 设置播放按钮点击事件
            holder.binding.playButton.setOnClickListener(v -> {
                holder.binding.videoThumbnail.setVisibility(View.GONE);
                holder.binding.playButton.setVisibility(View.GONE);
                holder.binding.videoView.start();
            });
        }

        // 设置视频封面
        String thumbnailUrl = generateVideoThumbnailUrl(clip.url);
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_empty_state)
                    .error(R.drawable.ic_empty_state)
                    .override(targetWidth, targetHeight)
                    .centerCrop()
                    .into(holder.binding.videoThumbnail);
        } else {
            holder.binding.videoThumbnail.setImageResource(R.drawable.ic_empty_state);
        }

        Log.d(TAG, "Loaded video: " + clip.url + ", size: " + targetWidth + "x" + targetHeight);
    }

    /**
     * 生成视频缩略图URL (模拟）
     */
    private String generateVideoThumbnailUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return null;

        // 模拟生成缩略图URL（实际应该从API获取）
        int lastSlash = videoUrl.lastIndexOf('/');
        String filename = lastSlash > 0 ? videoUrl.substring(lastSlash + 1) : "video";
        String basePath = lastSlash > 0 ? videoUrl.substring(0, lastSlash + 1) : "";

        return basePath + "thumbnails/" + filename + ".jpg";
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

        public VideoViewHolder(@NonNull ItemMediaVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}