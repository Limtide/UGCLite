package com.limtide.ugclite.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.adapter.MediaPagerAdapter;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ActivityPostDetailBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 作品详情页Activity
 * 按照111.md文档第二部分实现
 */
public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";

    // ViewBinding
    private ActivityPostDetailBinding binding;

    // 数据
    private Post currentPost;
    private List<Post.Clip> mediaClips;
    private int currentMediaPosition = 0;
    private boolean isLiked = false;
    private boolean isFollowing = false;

    // ViewPager adapter
    private MediaPagerAdapter mediaPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // 获取传递的数据
            getIntentData();

            // 检查Post数据是否有效
            if (currentPost == null) {
                Log.e(TAG, "Post data is null, finishing activity");
                finish();
                return;
            }

            // 初始化界面
            initViews();
            setupClickListeners();
            setupViewPager();

            Log.d(TAG, "PostDetailActivity created for: " + currentPost.title);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    /**
     * 获取Intent传递的数据
     */
    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "获取Intent传递的数据");

            // 从HomeFragment传递的Post对象
            currentPost = intent.getParcelableExtra("post");
            if (currentPost != null) {
                Log.d(TAG, "Post对象获取成功: " + currentPost.title);
                mediaClips = currentPost.clips != null ? currentPost.clips : new ArrayList<>();
                Log.d(TAG, "媒体片段数量: " + mediaClips.size());
            } else {
                Log.e(TAG, "Post对象为null，可能是Parcelable传递失败");

                // 创建一个示例Post对象用于测试
                currentPost = new Post();
                currentPost.title = "示例作品标题";
                currentPost.content = "这是一个示例作品内容，用于测试详情页面的显示效果。";
                currentPost.createTime = System.currentTimeMillis() / 1000;

                // 创建示例作者
                currentPost.author = new Post.Author();
                currentPost.author.nickname = "示例用户";
                currentPost.author.avatarUrl = "";

                // 创建空的媒体列表
                mediaClips = new ArrayList<>();
                Log.d(TAG, "创建了示例Post对象用于测试");
            }
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        if (currentPost == null) {
            Log.e(TAG, "Post data is null");
            finish();
            return;
        }

        try {
            // 设置顶部导航栏
            setupTopNavigation();

            // 设置内容区域
            setupContentArea();

            // 设置底部交互栏
            setupBottomInteractionBar();
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage(), e);
        }
    }

    /**
     * 设置顶部导航栏
     */
    private void setupTopNavigation() {
        try {
            // 设置作者信息
            if (currentPost.author != null) {
                // 设置作者昵称 (注意：XML中ID是author_nickname)
                if (binding.authorNickname != null) {
                    binding.authorNickname.setText(currentPost.author.nickname != null ? currentPost.author.nickname : "");
                }

                // 注意：authorAvatar已在XML中移除，根据HTML模板只需要显示昵称
                Log.d(TAG, "Author nickname set: " + currentPost.author.nickname);
            }

            // 设置关注按钮状态和文本
            updateFollowButton();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupTopNavigation: " + e.getMessage(), e);
        }
    }

    /**
     * 设置ViewPager和媒体容器
     */
    private void setupViewPager() {
        if (mediaClips == null || mediaClips.isEmpty()) {
            // 没有媒体内容时隐藏ViewPager
            binding.mediaViewpager.setVisibility(View.GONE);
            binding.progressIndicatorContainer.setVisibility(View.GONE);
            return;
        }

        // 创建ViewPager适配器
        mediaPagerAdapter = new MediaPagerAdapter(this, mediaClips);
        binding.mediaViewpager.setAdapter(mediaPagerAdapter);

        // 设置进度条指示器
        setupProgressIndicator();

        // 监听ViewPager页面变化
        binding.mediaViewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentMediaPosition = position;
                updateProgressIndicator();
                Log.d(TAG, "Media page changed to: " + position);
            }
        });

        // 设置当前页
        binding.mediaViewpager.setCurrentItem(currentMediaPosition, false);
    }

    /**
     * 设置进度条指示器
     */
    private void setupProgressIndicator() {
        if (mediaClips == null || mediaClips.size() <= 1) {
            binding.progressIndicatorContainer.setVisibility(View.GONE);
            return;
        }

        binding.progressIndicatorContainer.setVisibility(View.VISIBLE);
        updateProgressIndicator();
    }

    /**
     * 更新进度条指示器
     */
    private void updateProgressIndicator() {
        if (mediaClips == null || mediaClips.size() <= 1) return;

        // 根据新的XML布局，更新预定义的指示器View
        // 这里我们使用更简单的方式，只更新第一个指示器的状态
        // 因为新设计中只有有限的几个指示条
        if (currentMediaPosition < mediaClips.size()) {
            // 设置主指示条为白色，其他为半透明
            binding.mainIndicator.setBackgroundColor(Color.WHITE);
            binding.secondaryIndicator1.setBackgroundColor(Color.parseColor("#57FFFFFF"));
            binding.secondaryIndicator2.setBackgroundColor(Color.parseColor("#57FFFFFF"));
            binding.secondaryIndicator3.setBackgroundColor(Color.parseColor("#57FFFFFF"));

            // 如果有多于1个clip，可以设置对应的指示器为白色
            // 这里简化处理，根据当前页设置不同的指示器状态
            switch (currentMediaPosition) {
                case 0:
                    binding.mainIndicator.setBackgroundColor(Color.WHITE);
                    break;
                case 1:
                    binding.secondaryIndicator1.setBackgroundColor(Color.WHITE);
                    binding.mainIndicator.setBackgroundColor(Color.parseColor("#57FFFFFF"));
                    break;
                case 2:
                    binding.secondaryIndicator2.setBackgroundColor(Color.WHITE);
                    binding.mainIndicator.setBackgroundColor(Color.parseColor("#57FFFFFF"));
                    break;
                case 3:
                    binding.secondaryIndicator3.setBackgroundColor(Color.WHITE);
                    binding.mainIndicator.setBackgroundColor(Color.parseColor("#57FFFFFF"));
                    break;
                default:
                    binding.mainIndicator.setBackgroundColor(Color.WHITE);
                    break;
            }
        }
    }

    /**
     * 设置内容区域
     */
    private void setupContentArea() {
        // 设置标题
        if (binding.postTitle != null) {
            binding.postTitle.setText(currentPost.title != null ? currentPost.title : "");
        }

        // 设置简短描述
        if (binding.postBrief != null) {
            // 简化处理：使用content的前50个字符作为简短描述
            String briefText = currentPost.content != null && currentPost.content.length() > 50 ?
                    currentPost.content.substring(0, 50) + "..." :
                    (currentPost.content != null ? currentPost.content : "");
            binding.postBrief.setText(briefText);
        }

        // 设置详细内容（包含话题高亮）
        if (binding.postContent != null) {
            setupContentWithHashtags();
        }

        // 设置扩展内容
        if (binding.postExpandedContent != null) {
            // 使用完整的content作为扩展内容
            binding.postExpandedContent.setText(currentPost.content != null ? currentPost.content : "");
        }

        // 设置话题标签
        if (binding.postTags != null) {
            // 生成话题标签文本
            String tagsText = "#塞维利亚 #最美日落 #最难忘的一次旅行"; // 示例文本
            binding.postTags.setText(tagsText);
        }

        // 设置发布时间
        setupPublishTime();
    }

    /**
     * 设置正文内容和话题标签高亮
     */
    private void setupContentWithHashtags() {
        String content = currentPost.content != null ? currentPost.content : "";

        if (currentPost.hashtags != null && !currentPost.hashtags.isEmpty()) {
            SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();

            int lastEnd = 0;
            // 按话题标签位置排序
            for (Post.Hashtag hashtag : currentPost.hashtags) {
                // 添加普通文本
                if (hashtag.start > lastEnd) {
                    spannableBuilder.append(content.substring(lastEnd, hashtag.start));
                }

                // 添加高亮的话题标签
                String hashtagText = content.substring(hashtag.start, hashtag.end);
                SpannableString hashtagSpan = new SpannableString(hashtagText);
                hashtagSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#1890FF")),
                        0, hashtagText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                hashtagSpan.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        // 点击话题标签跳转
                        handleHashtagClick(hashtagText);
                    }
                }, 0, hashtagText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                spannableBuilder.append(hashtagSpan);
                lastEnd = hashtag.end;
            }

            // 添加剩余文本
            if (lastEnd < content.length()) {
                spannableBuilder.append(content.substring(lastEnd));
            }

            binding.postContent.setText(spannableBuilder);
            binding.postContent.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            // 没有话题标签，直接显示
            binding.postContent.setText(content);
        }
    }

    /**
     * 设置发布时间
     */
    private void setupPublishTime() {
        String timeText = formatPublishTime(currentPost.createTime);
        if (binding.publishTime != null) {
            binding.publishTime.setText(timeText);
        }

        // 根据HTML模板，还需要设置小时和分钟
        if (binding.publishHour != null) {
            // 这里简化处理，使用数字2作为示例
            binding.publishHour.setText("12");
        }
        if (binding.publishMinute != null) {
            // 这里简化处理，使用数字28作为示例
            binding.publishMinute.setText("28");
        }
    }

    /**
     * 格式化发布时间
     */
    private String formatPublishTime(long createTime) {
        try {
            long currentTime = System.currentTimeMillis();

            // 如果createTime看起来像秒级时间戳（长度小于11位），转换为毫秒
            if (createTime < 10000000000L) {
                createTime = createTime * 1000;
            }

            long diffMillis = currentTime - createTime;
            long diffSeconds = diffMillis / 1000;

            if (diffSeconds < 60) { // 1分钟内
                return "刚刚";
            } else if (diffSeconds < 3600) { // 1小时内
                long minutes = diffSeconds / 60;
                return minutes + "分钟前";
            } else if (diffSeconds < 86400) { // 24小时内
                long hours = diffSeconds / 3600;
                return hours + "小时前";
            } else if (diffSeconds < 604800) { // 7天内
                long days = diffSeconds / 86400;
                return days + "天前";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());
                return sdf.format(new Date(createTime));
            }
        } catch (Exception e) {
            return "刚刚";
        }
    }

    /**
     * 设置底部交互栏
     */
    private void setupBottomInteractionBar() {
        // 设置点赞状态
        updateLikeButton();
    }

    /**
     * 更新关注按钮状态
     */
    private void updateFollowButton() {
        if (binding.followButton != null) {
            binding.followButton.setText(isFollowing ? "已关注" : "关注");
            binding.followButton.setBackgroundColor(isFollowing ?
                    ContextCompat.getColor(this, R.color.darker_gray) :
                    ContextCompat.getColor(this, R.color.holo_blue_dark));
        }
    }

    /**
     * 更新点赞按钮状态
     */
    private void updateLikeButton() {
        // 根据新的XML布局，使用collectButton作为点赞按钮
        if (binding.collectButton != null) {
            binding.collectButton.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like);

            // 注意：新布局中没有likeCount，如果需要显示数量可以：
            // 1. 添加Toast提示
            // 2. 使用其他方式显示数量变化
            Log.d(TAG, "Like status updated: " + (isLiked ? "liked" : "not liked"));
        }
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 返回按钮 - 使用searchButton作为返回按钮（根据HTML模板布局）
        if (binding.searchButton != null) {
            binding.searchButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish();
            });
        }

        // 关注按钮
        if (binding.followButton != null) {
            binding.followButton.setOnClickListener(v -> {
                Log.d(TAG, "Follow button clicked");
                toggleFollowStatus();
            });
        }

        // 评论按钮（可选功能）
        if (binding.commentButton != null) {
            binding.commentButton.setOnClickListener(v -> {
                Log.d(TAG, "Comment button clicked");
                Toast.makeText(this, "评论功能开发中", Toast.LENGTH_SHORT).show();
            });
        }

        // 分享按钮
        if (binding.shareButton != null) {
            binding.shareButton.setOnClickListener(v -> {
                Log.d(TAG, "Share button clicked");
                sharePost();
            });
        }

        // 收藏按钮
        if (binding.collectButton != null) {
            binding.collectButton.setOnClickListener(v -> {
                Log.d(TAG, "Collect button clicked");
                toggleLikeStatus(); // 使用现有的点赞逻辑
            });
        }

        // 更多操作按钮
        if (binding.moreActionsButton != null) {
            binding.moreActionsButton.setOnClickListener(v -> {
                Log.d(TAG, "More actions button clicked");
                Toast.makeText(this, "更多操作功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * 切换关注状态
     */
    private void toggleFollowStatus() {
        isFollowing = !isFollowing;
        updateFollowButton();

        // 这里应该调用API更新关注状态
        // APIManager.followUser(currentPost.author.userId, isFollowing, success -> {
        //     if (success) {
        //         runOnUiThread(() -> {
        //             isFollowing = !isFollowing;
        //             updateFollowButton();
        //         });
        //     }
        // });

        Toast.makeText(this, isFollowing ? "已关注" : "取消关注", Toast.LENGTH_SHORT).show();
    }

    /**
     * 切换点赞状态
     */
    private void toggleLikeStatus() {
        isLiked = !isLiked;
        updateLikeButton();

        // 这里应该调用API更新点赞状态
        // APIManager.likePost(currentPost.postId, isLiked, success -> {
        //     if (success) {
        //         runOnUiThread(() -> {
        //             isLiked = !isLiked;
        //             updateLikeButton();
        //         });
        //     }
        // });

        Toast.makeText(this, isLiked ? "已点赞" : "取消点赞", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理话题标签点击
     */
    private void handleHashtagClick(String hashtagText) {
        Log.d(TAG, "Hashtag clicked: " + hashtagText);
        Toast.makeText(this, "话题: " + hashtagText, Toast.LENGTH_SHORT).show();

        // 这里可以跳转到话题页面
        // Intent intent = new Intent(this, HashtagActivity.class);
        // intent.putExtra("hashtag", hashtagText);
        // startActivity(intent);
    }

    /**
     * 分享作品
     */
    private void sharePost() {
        if (currentPost == null) return;

        String shareText = (currentPost.title != null ? currentPost.title : "") +
                "\n" + (currentPost.content != null ? currentPost.content : "");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "分享作品"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PostDetailActivity destroyed");

        // 清理ViewBinding
        binding = null;
    }
}