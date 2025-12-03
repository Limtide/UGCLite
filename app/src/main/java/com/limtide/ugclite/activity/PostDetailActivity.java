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
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.MainActivity;
import com.limtide.ugclite.R;
import com.limtide.ugclite.adapter.MediaPagerAdapter;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ActivityPostDetailBinding;
import com.limtide.ugclite.utils.LikeManager;

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
    private boolean isStarred = false;
    private boolean isFollowing = false;

    // ViewPager adapter
    private MediaPagerAdapter mediaPagerAdapter;

    // 点赞管理器
    private LikeManager likeManager;

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
                Toast.makeText(this, "无法获取作品详情", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 初始化点赞管理器
            likeManager = LikeManager.getInstance(this);

            // 初始化界面
            initViews();
            setupClickListeners();
            setupViewPager();

            // 初始化点赞状态
            initLikeStatus();

            Log.d(TAG, "PostDetailActivity created for: " + currentPost.title);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    /**
     * 初始化点赞状态（与NoteCard同步）
     */
    private void initLikeStatus() {
        if (currentPost == null || likeManager == null) {
            return;
        }

        // 从LikeManager获取点赞状态
        isLiked = likeManager.isPostLiked(currentPost.postId);

        // 更新UI显示
        updateLikeButton();
        updateLikeCount();

        Log.d(TAG, "初始化点赞状态 - PostId: " + currentPost.postId +
                  ", IsLiked: " + isLiked);
    }

    /**
     * 获取Intent传递的数据
     */
    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "获取Intent传递的数据");
            Log.d(TAG, "Intent extras: " + intent.getExtras());

            // 从HomeFragment传递的Post对象
            currentPost = (Post) intent.getSerializableExtra("post");
            if (currentPost != null) {
                Log.d(TAG, "Post对象获取成功: " + currentPost.title);
                Log.d(TAG, "Post ID: " + currentPost.postId);
                mediaClips = currentPost.clips != null ? currentPost.clips : new ArrayList<>();
                Log.d(TAG, "媒体片段数量: " + mediaClips.size());
            } else {
                Log.e(TAG, "Post对象为null，可能是Parcelable传递失败");
                Log.e(TAG, "Intent中是否包含post key: " + (intent.hasExtra("post") ? "是" : "否"));

                // 尝试获取传递的字符串数据作为备用方案
                String postId = intent.getStringExtra("post_id");
                String title = intent.getStringExtra("post_title"); // 使用正确的key
                String content = intent.getStringExtra("post_content"); // 使用正确的key
                long createTime = intent.getLongExtra("post_create_time", System.currentTimeMillis() / 1000);

                if (postId != null || title != null || content != null) {
                    // 使用备用数据创建Post对象
                    currentPost = new Post();
                    currentPost.postId = postId != null ? postId : "";
                    currentPost.title = title != null ? title : "未知标题";
                    currentPost.content = content != null ? content : "";
                    currentPost.createTime = createTime;

                    // 创建示例作者
                    currentPost.author = new Post.Author();
                    currentPost.author.nickname = "未知用户";
                    currentPost.author.avatarUrl = "";

                    mediaClips = new ArrayList<>();
                    Log.d(TAG, "使用备用数据创建了Post对象");
                } else {
                    Log.w(TAG, "无法获取任何Post数据，Activity将关闭");
                }
            }
        } else {
            Log.e(TAG, "Intent为null");
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
                // 设置作者昵称 - 使用正确的binding字段
                if (binding.userName != null) {
                    binding.userName.setText(currentPost.author.nickname != null ? currentPost.author.nickname : "");
                }

                // 设置用户头像
                if (binding.userAvatar != null) {
                    String avatarUrl = currentPost.author.avatarUrl;
                    if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                        // 使用Glide加载头像
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_user) // 设置占位图
                                .error(R.drawable.ic_user)      // 设置错误图片
                                .circleCrop()                             // 圆形裁剪
                                .into(binding.userAvatar);
                        Log.d(TAG, "Author avatar loaded: " + avatarUrl);
                    } else {
                        // 没有头像时使用默认头像
                        binding.userAvatar.setImageResource(R.drawable.ic_user);
                        Log.d(TAG, "Author avatar set to default placeholder");
                    }
                }

                Log.d(TAG, "Author nickname set: " + currentPost.author.nickname);
            }

            // 设置关注按钮状态和文本
//            updateFollowButton();
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
            binding.viewPager.setVisibility(View.GONE);
            binding.homeIndicatorContainer.setVisibility(View.GONE);
            return;
        }

        // 设置ViewPager高度（基于首图比例）
        setupViewPagerHeight();

        // 创建ViewPager适配器，传递首图比例信息
        mediaPagerAdapter = new MediaPagerAdapter(this, mediaClips, getFirstClipAspectRatio());
        binding.viewPager.setAdapter(mediaPagerAdapter);

        // 设置进度条指示器
        setupProgressIndicator();

        // 监听ViewPager页面变化
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentMediaPosition = position;
                updateProgressIndicator();
                Log.d(TAG, "Media page changed to: " + position);
            }
        });

        // 设置当前页
        binding.viewPager.setCurrentItem(currentMediaPosition, false);
    }

    /**
     * 设置ViewPager高度（基于首图比例，在3:4 ~ 16:9范围内使用原始比例，超出则使用上下限）
     */
    private void setupViewPagerHeight() {
        Post.Clip firstClip = mediaClips.get(0);
        float aspectRatio = firstClip.getAspectRatio();

        // 定义宽高比范围：3:4 = 0.75, 16:9 ≈ 1.78
        float minRatio = 0.75f;  // 3:4
        float maxRatio = 1.78f;  // 16:9

        // 计算最终使用的比例
        float finalRatio;
        if (aspectRatio < minRatio) {
            finalRatio = minRatio;  // 小于下限，使用下限
        } else if (aspectRatio > maxRatio) {
            finalRatio = maxRatio;  // 大于上限，使用上限
        } else {
            finalRatio = aspectRatio;  // 在范围内，使用原始比例
        }

        // 获取屏幕宽度
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int targetHeight = (int) (screenWidth / finalRatio);

        // 设置ViewPager容器高度（包含ViewPager和悬浮的进度条）
        android.view.ViewGroup.LayoutParams params = binding.viewpagerContainer.getLayoutParams();
        params.height = targetHeight;
        binding.viewpagerContainer.setLayoutParams(params);

        Log.d(TAG, "ViewPager height setup - Original ratio: " + aspectRatio +
                  ", Final ratio: " + finalRatio + " (" +
                  (aspectRatio < minRatio ? "used min" : aspectRatio > maxRatio ? "used max" : "used original") +
                  "), Height: " + targetHeight + "px");
    }

    /**
     * 获取首图的比例（在3:4 ~ 16:9范围内使用原始比例，超出则使用上下限）
     */
    private float getFirstClipAspectRatio() {
        if (mediaClips == null || mediaClips.isEmpty()) {
            return 1.0f; // 默认1:1
        }

        Post.Clip firstClip = mediaClips.get(0);
        float aspectRatio = firstClip.getAspectRatio();

        // 定义宽高比范围：3:4 = 0.75, 16:9 ≈ 1.78
        float minRatio = 0.75f;  // 3:4
        float maxRatio = 1.78f;  // 16:9

        // 在范围内使用原始比例，超出则使用上下限
        if (aspectRatio < minRatio) {
            return minRatio;  // 小于下限，使用下限
        } else if (aspectRatio > maxRatio) {
            return maxRatio;  // 大于上限，使用上限
        } else {
            return aspectRatio;  // 在范围内，使用原始比例
        }
    }

    /**
     * 设置进度条指示器
     */
    private void setupProgressIndicator() {
        if (mediaClips == null || mediaClips.size() <= 1) {
            // 单图或无图片时隐藏进度条
            binding.tabIndicator.setVisibility(View.GONE);
            return;
        }

        // 多图时显示进度条
        binding.tabIndicator.setVisibility(View.VISIBLE);

        // 清除现有的tabs
        binding.tabIndicator.removeAllTabs();

        // 根据图片数量添加对应的指示点
        for (int i = 0; i < mediaClips.size(); i++) {
            TabLayout.Tab tab = binding.tabIndicator.newTab();
            binding.tabIndicator.addTab(tab);
        }

        // 设置当前选中的tab
        TabLayout.Tab selectedTab = binding.tabIndicator.getTabAt(currentMediaPosition);
        if (selectedTab != null) {
            selectedTab.select();
        }

        Log.d(TAG, "Progress indicator setup with " + mediaClips.size() + " tabs");
    }

    /**
     * 更新进度条指示器
     */
    private void updateProgressIndicator() {
        if (binding.tabIndicator.getTabCount() > 0) {
            TabLayout.Tab selectedTab = binding.tabIndicator.getTabAt(currentMediaPosition);
            if (selectedTab != null) {
                selectedTab.select();
            }
        }
    }

    /**
     * 设置内容区域
     */
    private void setupContentArea() {
        // 设置标题 - 使用binding中可用的字段
        if (binding.titleText != null) {
            binding.titleText.setText(currentPost.title != null ? currentPost.title : "");
        }

        // 设置正文内容和话题标签 - 使用binding中可用的字段
        if (binding.contentText != null) {
            setupContentWithHashtags();
        }

// 设置日期和时间 - 根据新的规则显示
        if (binding.dateText != null && binding.timeText != null) {
            String[] dateParts = formatPostTime(currentPost.createTime);
            binding.dateText.setText(dateParts[0]); // 日期部分
            binding.timeText.setText(dateParts[1]); // 时间部分
        }

        Log.d(TAG, "Content area setup completed");
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

                    @Override
                    public void updateDrawState(@NonNull android.text.TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false); // 移除下划线
                        ds.setColor(Color.parseColor("#1890FF")); // 设置颜色
                    }
                }, 0, hashtagText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                spannableBuilder.append(hashtagSpan);
                lastEnd = hashtag.end;
            }

            // 添加剩余文本
            if (lastEnd < content.length()) {
                spannableBuilder.append(content.substring(lastEnd));
            }

          binding.contentText.setText(spannableBuilder);
            binding.contentText.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            // 没有话题标签，直接显示
            binding.contentText.setText(content);
        }
    }

 

    /**
     * 设置底部交互栏
     */
    private void setupBottomInteractionBar() {
        // 设置点赞状态
//        updateLikeButton();

        // 设置初始数量显示
        setupCountDisplays();
    }

    /**
     * 设置数量显示
     */
    private void setupCountDisplays() {
        // 设置点赞数量
        if (binding.likeCount != null) {
            binding.likeCount.setText("25");
        }

        // 设置评论数量
        if (binding.commentCount != null) {
            binding.commentCount.setText("25");
        }

        // 设置收藏数量
        if (binding.starCount != null) {
            binding.starCount.setText("25");
        }

        // 设置分享数量
        if (binding.shareCount != null) {
            binding.shareCount.setText("25");
        }
    }

    /**
     * 更新关注按钮状态
     */
  private void updateFollowButton() {
        if (binding.follow != null) {
            binding.follow.setText(isFollowing ? "已关注" : "关注");
            binding.follow.setBackgroundColor(isFollowing ?
                    ContextCompat.getColor(this, R.color.darker_gray) :
                    ContextCompat.getColor(this, R.color.holo_blue_dark));
        }
    }

    /**
     * 更新点赞按钮状态
     */
    private void updateLikeButton() {
        // 根据新的XML布局，使用likeButton作为点赞按钮
        // 更新点赞按钮图标
        if (binding.likeButton != null) {
            binding.likeButton.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like);
            Log.d(TAG, "Like status updated: " + (isLiked ? "liked" : "not liked"));
        }
    }

    /**
     * 更新点赞数量显示
     */
    private void updateLikeCount() {
        if (binding.likeCount != null && likeManager != null && currentPost != null) {
            int likeCount = likeManager.getLikeCount(currentPost.postId);
            binding.likeCount.setText(formatLikeCount(likeCount));
            Log.d(TAG, "Like count updated: " + likeCount);
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
     * 更新收藏按钮状态
     */
    private void updateStarButton() {
        // 更新收藏按钮图标
        if (binding.starButton != null) {
            binding.starButton.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);
            Log.d(TAG, "Star status updated: " + (isStarred ? "starred" : "not starred"));
        }
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {

        // 返回按钮 - 直接返回上一页
        binding.backButton.setOnClickListener(v -> {
            Log.d(TAG, "back button clicked");
            finish();
            // 添加返回动画，让用户体验更流畅
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 快捷评论框
        if (binding.quickCommentEdit != null) {
            binding.quickCommentEdit.setOnClickListener(v -> {
                Log.d(TAG, "quick comment edit clicked");
            });
        }

        // 底部栏按钮 - 实现真正的点赞功能
        if (binding.likeButton != null) {
            binding.likeButton.setOnClickListener(v -> {
                Log.d(TAG, "Like button clicked");
                handleLikeClick();
            });
        }

        if (binding.commentButton != null) {
            binding.commentButton.setOnClickListener(v -> {
                Log.d(TAG, "Comment button clicked");
                // 只响应点击，不执行任何操作
            });
        }

        if (binding.starButton != null) {
            binding.starButton.setOnClickListener(v -> {
                Log.d(TAG, "Star button clicked");
                // 只响应点击，不执行任何操作
            });
        }

        if (binding.shareButton != null) {
            binding.shareButton.setOnClickListener(v -> {
                Log.d(TAG, "Share button clicked");
                // 只响应点击，不执行任何操作
            });
        }
    }

    /**
     * 处理点赞点击（与NoteCard同步）
     */
    private void handleLikeClick() {
        if (currentPost == null || likeManager == null) {
            return;
        }

        // 使用LikeManager切换点赞状态
        boolean newLikeStatus = likeManager.toggleLike(currentPost.postId);
        isLiked = newLikeStatus;

        // 更新UI显示
        updateLikeButton();
        updateLikeCount();

        Log.d(TAG, "Like clicked - PostId: " + currentPost.postId +
                  ", NewStatus: " + newLikeStatus +
                  ", TotalLikedPosts: " + likeManager.getAllLikedPosts().size());

        Toast.makeText(this, isLiked ? "已点赞" : "取消点赞", Toast.LENGTH_SHORT).show();
    }

    /**
     * 切换关注状态
     */
    private void toggleFollowStatus() {
        isFollowing = !isFollowing;
//        updateFollowButton();

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
     * 切换点赞状态（保留原方法作为兼容）
     */
    private void toggleLikeStatus() {
        handleLikeClick();
    }

    /**
     * 切换收藏状态
     */
    private void toggleStarStatus() {
        isStarred = !isStarred;
        updateStarButton();

        // 这里应该调用API更新收藏状态
        // APIManager.starPost(currentPost.postId, isStarred, success -> {
        //     if (success) {
        //         runOnUiThread(() -> {
        //             isStarred = !isStarred;
        //             updateStarButton();
        //         });
        //     }
        // });

        Toast.makeText(this, isStarred ? "已收藏" : "取消收藏", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理话题标签点击
     */
    private void handleHashtagClick(String hashtagText) {
        Log.d(TAG, "Hashtag clicked: " + hashtagText);

        // 跳转到话题页面
        Intent intent = new Intent(this, HashtagActivity.class);
        intent.putExtra("hashtag", hashtagText);
        startActivity(intent);
    }

    /**
     * 格式化发布时间 - 根据新的规则显示日期
     * 返回数组：[日期文案, 时间文案]
     * 规则：
     * - 24h内：日期文案="今天"/"昨天"，时间文案=HH:mm
     * - 7天内：日期文案="x天前"，时间文案=""
     * - 其余：日期文案=MM-dd，时间文案=""
     */
    private String[] formatPostTime(long timestamp) {
        try {
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = currentTime - timestamp;

            // 转换为毫秒用于Date对象
            Date postDate = new Date(timestamp * 1000);

            // 计算天数差（忽略时间部分）
            long daysDiff = calculateDaysDiff(timestamp, currentTime);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());

            if (timeDiff < 60) {
                // 1分钟内
                return new String[]{"刚刚", ""};
            } else if (timeDiff < 3600) {
                // 1小时内
                int minutes = (int) (timeDiff / 60);
                return new String[]{minutes + "分钟前", ""};
            } else if (timeDiff < 86400) {
                // 24小时内
                return new String[]{"今天", timeFormat.format(postDate)};
            } else if (daysDiff == 1) {
                // 昨天
                return new String[]{"昨天", timeFormat.format(postDate)};
            } else if (daysDiff <= 7) {
                // 7天内
                return new String[]{daysDiff + "天前", ""};
            } else {
                // 超过7天，显示具体日期
                return new String[]{dateFormat.format(postDate), ""};
            }
        } catch (Exception e) {
            return new String[]{"刚刚", ""};
        }
    }

    /**
     * 计算天数差（忽略时间部分，只计算日期差）
     */
    private long calculateDaysDiff(long timestamp1, long timestamp2) {
        try {
            // 转换为毫秒
            long ms1 = timestamp1 * 1000;
            long ms2 = timestamp2 * 1000;

            // 使用Calendar计算天数差
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal1.setTimeInMillis(ms1);
            cal2.setTimeInMillis(ms2);

            // 设置时间为0点，只比较日期
            cal1.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal1.set(java.util.Calendar.MINUTE, 0);
            cal1.set(java.util.Calendar.SECOND, 0);
            cal1.set(java.util.Calendar.MILLISECOND, 0);

            cal2.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal2.set(java.util.Calendar.MINUTE, 0);
            cal2.set(java.util.Calendar.SECOND, 0);
            cal2.set(java.util.Calendar.MILLISECOND, 0);

            long diffInMillis = cal2.getTimeInMillis() - cal1.getTimeInMillis();
            return diffInMillis / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return 0;
        }
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