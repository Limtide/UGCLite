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

import java.util.List;

/**
 * 媒体适配器 - 用于详情页ViewPager2
 * 只支持图片片段
 */
public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.ImageViewHolder> {

    private static final String TAG = "MediaPagerAdapter";

    private Context context;
    private List<Post.Clip> mediaClips;

    public MediaPagerAdapter(Context context, List<Post.Clip> mediaClips) {
        this.context = context;
        this.mediaClips = mediaClips;
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