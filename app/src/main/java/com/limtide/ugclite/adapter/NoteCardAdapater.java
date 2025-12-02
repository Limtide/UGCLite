package com.limtide.ugclite.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.activity.PostDetailActivity;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.NoteCardBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 瀑布流适配器 - 用于展示note_card内容
 */
public class NoteCardAdapater extends RecyclerView.Adapter<NoteCardAdapater.ViewHolder> {

    private static final String TAG = "WaterfallAdapter";
    private List<Post> postList;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Post post, int position);
    }

    public NoteCardAdapater(Context context) {
        this.context = context;
        this.postList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        NoteCardBinding binding = NoteCardBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        NoteCardBinding binding = holder.getBinding();

        // 设置封面图片 - 显示图片或视频类型的第一个clip作为封面
        if (post.clips != null && !post.clips.isEmpty()) {
            // 查找第一个图片或视频类型的clip
            for (Post.Clip clip : post.clips) {
                if (clip.type == 0 || clip.type == 1) {
                    // 图片类型(type=0)或视频类型(type=1)
                    if (clip.type == 0) {
                        // 图片类型，直接加载图片
                        Glide.with(context)
                                .load(clip.url)
                                .placeholder(R.drawable.ic_empty_state)
                                .error(R.drawable.ic_empty_state)
                                .into(binding.coverImage);
                    } else {
                        // 视频类型，默认显示视频缩略图图标
                        binding.coverImage.setImageResource(R.drawable.ic_empty_state);
                    }
                    break; // 找到第一个图片或视频就停止
                }
            }
        } else {
            // 默认封面
            binding.coverImage.setImageResource(R.drawable.ic_empty_state);
        }

        // 设置标题
        binding.videoTitle.setText(post.title != null ? post.title : "");

        // 设置用户信息
        if (post.author != null) {
            binding.userName.setText(post.author.nickname != null ? post.author.nickname : "");

            // 设置用户头像
            if (!TextUtils.isEmpty(post.author.avatarUrl)) {
                Glide.with(context)
                        .load(post.author.avatarUrl)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .circleCrop()
                        .into(binding.userAvatar);
            } else {
                binding.userAvatar.setImageResource(R.drawable.ic_user);
            }
        }

        // 设置点赞数量
        binding.likeCount.setText("128");

        // 设置点击事件 - 使用当前position而不是绑定时的position
        final int currentPosition = holder.getAdapterPosition();
        binding.getRoot().setOnClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            Post clickedPost = clickedPosition != RecyclerView.NO_POSITION ? postList.get(clickedPosition) : post;

            Log.d(TAG, "Card clicked at position: " + clickedPosition + ", post: " + (clickedPost != null ? clickedPost.title : "null"));

            if (onItemClickListener != null) {
                Log.d(TAG, "Calling onItemClickListener");
                onItemClickListener.onItemClick(clickedPost, clickedPosition);
            } else {
                Log.d(TAG, "onItemClickListener is null, using direct navigation");
                // 如果没有设置监听器，直接跳转到详情页
                navigateToDetailPage(clickedPost);
            }
        });

        // 设置长按事件作为备用的调试手段
        binding.getRoot().setOnLongClickListener(v -> {
            int longPressedPosition = holder.getAdapterPosition();
            Post longPressedPost = longPressedPosition != RecyclerView.NO_POSITION ? postList.get(longPressedPosition) : post;
            Log.d(TAG, "Card long pressed at position: " + longPressedPosition + ", post: " + (longPressedPost != null ? longPressedPost.title : "null"));
            return true;
        });

        Log.d(TAG, "Binding post at position " + position + ": " + post.title);
    }

    /**
     * 跳转到详情页
     */
    private void navigateToDetailPage(Post post) {
        if (context == null || post == null) {
            Log.e(TAG, "Cannot navigate to detail: context or post is null");
            return;
        }

        try {
            Intent intent = new Intent(context, PostDetailActivity.class);

            // 方法1：直接传递Post对象（主要方式）
            intent.putExtra("post", post);

            // 方法2：备用的字段传递（防止Parcelable失败）
            intent.putExtra("post_id", post.postId);
            intent.putExtra("post_title", post.title);
            intent.putExtra("post_content", post.content);
            intent.putExtra("post_create_time", post.createTime);

            Log.d(TAG, "Navigating to detail page for post: " + post.title);
            Log.d(TAG, "Post ID: " + post.postId + ", clips size: " + (post.clips != null ? post.clips.size() : 0));

            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting PostDetailActivity: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    /**
     * 添加新的数据
     */
    public void addPosts(List<Post> newPosts) {
        if (newPosts != null) {
            int oldSize = postList.size();
            postList.addAll(newPosts);
            notifyItemRangeInserted(oldSize, newPosts.size());
            Log.d(TAG, "Added " + newPosts.size() + " new posts");
        }
    }

    /**
     * 设置新数据（替换所有数据）
     */
    public void setPosts(List<Post> posts) {
        this.postList.clear();
        if (posts != null) {
            this.postList.addAll(posts);
            Log.d(TAG, "Posts added successfully:");
            for (int i = 0; i < Math.min(posts.size(), 5); i++) {
                Post post = posts.get(i);
                Log.d(TAG, "Post " + i + ": " + (post != null && post.title != null ? post.title : "null"));
            }
        } else {
            Log.w(TAG, "setPosts called with null posts");
        }
        notifyDataSetChanged();
        Log.d(TAG, "Set " + (posts != null ? posts.size() : 0) + " posts, new total: " + postList.size());
        Log.d(TAG, "onItemClickListener is " + (onItemClickListener != null ? "not null" : "null"));
    }

    /**
     * 清空数据
     */
    public void clearPosts() {
        int oldSize = postList.size();
        postList.clear();
        notifyItemRangeRemoved(0, oldSize);
        Log.d(TAG, "Cleared all posts");
    }

    /**
     * 获取指定位置的数据
     */
    public Post getPost(int position) {
        return postList.get(position);
    }

    /**
     * 设置点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * 格式化时间戳
     */
    private String formatTime(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp * 1000));
        } catch (Exception e) {
            return "刚刚";
        }
    }

    /**
     * ViewHolder类
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final NoteCardBinding binding;

        public ViewHolder(@NonNull NoteCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public NoteCardBinding getBinding() {
            return binding;
        }
    }
}