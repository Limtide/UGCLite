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
import com.limtide.ugclite.utils.LikeManager;

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
    private LikeManager likeManager;

    public interface OnItemClickListener {
        void onItemClick(Post post, int position);
    }

    public NoteCardAdapater(Context context) {
        Log.d(TAG, "NoteCardAdapter constructor called - Context: " + (context != null ? context.getClass().getSimpleName() : "null"));
        this.context = context;
        this.postList = new ArrayList<>();
        this.likeManager = LikeManager.getInstance(context);
        Log.d(TAG, "NoteCardAdapter initialized successfully - LikeManager: " + (likeManager != null ? "initialized" : "failed"));
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

        // 设置标题 - 优先展示标题，没有标题时展示正文
        String displayText = "";
        if (post.title != null && !post.title.trim().isEmpty()) {
            displayText = post.title.trim();
        } else if (post.content != null && !post.content.trim().isEmpty()) {
            displayText = post.content.trim();
        }
        binding.videoTitle.setText(displayText);

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

        // 设置点赞状态和数量
        updateLikeDisplay(binding, post);

        final int currentPosition = holder.getAdapterPosition();

        // 设置点赞区域点击事件（图标和数量）- 使用binding绑定
        binding.likeIcon.setOnClickListener(v -> {
            Log.d(TAG, "Like icon clicked");
            int clickedPosition = holder.getAdapterPosition();
            Post clickedPost = clickedPosition != RecyclerView.NO_POSITION ? postList.get(clickedPosition) : post;

            if (clickedPost != null) {
                Log.d(TAG, "Like icon clicked - Post: " + clickedPost.title +
                          ", Position: " + clickedPosition +
                          ", PostId: " + clickedPost.postId);
                handleLikeClick(clickedPost, binding);
            } else {
                Log.w(TAG, "Like icon clicked but post is null or position invalid");
            }
        });

        binding.likeCount.setOnClickListener(v -> {
            Log.d(TAG, "Like count clicked");
            int clickedPosition = holder.getAdapterPosition();
            Post clickedPost = clickedPosition != RecyclerView.NO_POSITION ? postList.get(clickedPosition) : post;

            if (clickedPost != null) {
                Log.d(TAG, "Like count clicked - Post: " + clickedPost.title +
                          ", Position: " + clickedPosition +
                          ", PostId: " + clickedPost.postId);
                handleLikeClick(clickedPost, binding);
            } else {
                Log.w(TAG, "Like count clicked but post is null or position invalid");
            }
        });

        // 设置卡片整体点击事件，但排除点赞区域
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Card main area clicked");
            int clickedPosition = holder.getAdapterPosition();
            Post clickedPost = clickedPosition != RecyclerView.NO_POSITION ? postList.get(clickedPosition) : post;

            if (clickedPost != null) {
                Log.d(TAG, "Card main area clicked - Post: " + clickedPost.title +
                          ", Position: " + clickedPosition +
                          ", PostId: " + clickedPost.postId +
                          ", Author: " + (clickedPost.author != null ? clickedPost.author.nickname : "unknown"));

                // 首先检查是否有点击监听器设置
                if (onItemClickListener != null) {
                    Log.d(TAG, "Calling onItemClickListener for post navigation");
                    onItemClickListener.onItemClick(clickedPost, clickedPosition);
                } else {
                    Log.d(TAG, "No onItemClickListener set, using direct navigation");
                    // 如果没有设置监听器，直接跳转到详情页
                    navigateToDetailPage(clickedPost);
                }
            } else {
                Log.w(TAG, "Card main area clicked but post is null or position invalid");
            }
        });

        // 设置长按事件作为备用的调试手段
        binding.getRoot().setOnLongClickListener(v -> {
            Log.d(TAG, "Card long pressed");
            int longPressedPosition = holder.getAdapterPosition();
            Post longPressedPost = longPressedPosition != RecyclerView.NO_POSITION ? postList.get(longPressedPosition) : post;

            if (longPressedPost != null) {
                Log.d(TAG, "Card long pressed - Post: " + longPressedPost.title +
                          ", Position: " + longPressedPosition +
                          ", PostId: " + longPressedPost.postId +
                          ", Author: " + (longPressedPost.author != null ? longPressedPost.author.nickname : "unknown") +
                          ", Clips: " + (longPressedPost.clips != null ? longPressedPost.clips.size() : 0));
            } else {
                Log.w(TAG, "Card long pressed but post is null or position invalid");
            }

            return true; // 消费长按事件
        });

        Log.d(TAG, "Binding post at position " + position + ": " + post.title);
    }

    /**
     * 更新点赞显示状态和数量
     */
    private void updateLikeDisplay(NoteCardBinding binding, Post post) {
        if (post == null || binding == null) {
            Log.w(TAG, "updateLikeDisplay: binding or post is null");
            return;
        }

        boolean isLiked = likeManager.isPostLiked(post.postId);
        int likeCount = likeManager.getLikeCount(post.postId);

        // 设置点赞图标
        int iconResource = isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like;
        binding.likeIcon.setImageResource(iconResource);

        // 设置点赞数量 - 格式化大数字显示
        String likeCountStr = formatLikeCount(likeCount);
        binding.likeCount.setText(likeCountStr);

        Log.d(TAG, "Like display updated - Post: " + post.title +
                  ", PostId: " + post.postId +
                  ", IsLiked: " + isLiked +
                  ", LikeCount: " + likeCount +
                  ", IconResource: " + (isLiked ? "ic_like_filled" : "ic_like") +
                  ", DisplayText: " + likeCountStr);
    }

    /**
     * 处理点赞点击事件
     */
    private void handleLikeClick(Post post, NoteCardBinding binding) {
        if (post == null || binding == null) {
            Log.w(TAG, "handleLikeClick: binding or post is null");
            return;
        }

        // 获取切换前的状态
        boolean wasLiked = likeManager.isPostLiked(post.postId);
        int oldLikeCount = likeManager.getLikeCount(post.postId);

        Log.d(TAG, "Like click processing - Before toggle - Post: " + post.title +
                  ", PostId: " + post.postId +
                  ", WasLiked: " + wasLiked +
                  ", OldLikeCount: " + oldLikeCount);

        // 切换点赞状态
        boolean newLikeStatus = likeManager.toggleLike(post.postId);
        int newLikeCount = likeManager.getLikeCount(post.postId);

        // 更新显示
        updateLikeDisplay(binding, post);

        Log.d(TAG, "Like click processed - After toggle - Post: " + post.title +
                  ", PostId: " + post.postId +
                  ", NewLikeStatus: " + newLikeStatus +
                  ", NewLikeCount: " + newLikeCount +
                  ", CountChange: " + (newLikeStatus ? "+1" : "-1") +
                  ", TotalLikedPosts: " + likeManager.getAllLikedPosts().size());
    }

    /**
     * 跳转到详情页
     */
    private void navigateToDetailPage(Post post) {
        if (context == null || post == null) {
            Log.e(TAG, "navigateToDetailPage: context or post is null - Context: " + (context != null ? "not null" : "null") +
                      ", Post: " + (post != null ? "not null" : "null"));
            return;
        }

        Log.d(TAG, "Starting navigation to PostDetailActivity");
        Log.d(TAG, "Navigation target - Post: " + post.title +
                  ", PostId: " + post.postId +
                  ", Author: " + (post.author != null ? post.author.nickname : "unknown") +
                  ", Clips: " + (post.clips != null ? post.clips.size() : 0) +
                  ", Content length: " + (post.content != null ? post.content.length() : 0));

        try {
            Intent intent = new Intent(context, PostDetailActivity.class);

            // 方法1：直接传递Post对象（主要方式）
            intent.putExtra("post", post);

            // 方法2：备用的字段传递（防止Parcelable失败）
            intent.putExtra("post_id", post.postId);
            intent.putExtra("post_title", post.title);
            intent.putExtra("post_content", post.content);
            intent.putExtra("post_create_time", post.createTime);

            Log.d(TAG, "Intent created with extras - post_id: " + post.postId +
                      ", post_title: " + post.title +
                      ", context: " + context.getClass().getSimpleName());

            context.startActivity(intent);
            Log.d(TAG, "Successfully started PostDetailActivity for post: " + post.title);
        } catch (Exception e) {
            Log.e(TAG, "Error starting PostDetailActivity for post: " + post.title + " - Error: " + e.getMessage(), e);
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
        Log.d(TAG, "setOnItemClickListener called - Listener: " + (listener != null ? "not null" : "null"));
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
     * 格式化点赞数量显示
     */
    private String formatLikeCount(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 10000) {
            return String.format("%.1fK", count / 1000.0);
        } else if (count < 1000000) {
            return String.format("%dK", count / 1000);
        } else {
            return String.format("%.1fM", count / 1000000.0);
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