package com.limtide.ugclite.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ItemMediaImageBinding;

import java.util.List;

/**
 * 媒体适配器 - 用于详情页ViewPager2
 * 只支持图片片段
 */
public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.ImageViewHolder> {

    private static final String TAG = "MediaPagerAdapter";

    private Context context;
    private List<Post.Clip> mediaClips;
    private float firstClipAspectRatio; // 首图比例，用于容器高度计算

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

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMediaImageBinding binding = ItemMediaImageBinding.inflate(inflater, parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (mediaClips == null || position >= mediaClips.size()) {
            return;
        }

        Post.Clip clip = mediaClips.get(position);
        bindImage(holder, clip);

        Log.d(TAG, "Binding image at position " + position + ", type: " + clip.type);
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
        holder.binding.mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

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


}