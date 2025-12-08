package com.limtide.ugclite.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import com.bumptech.glide.Glide;
import com.limtide.ugclite.R;
import com.limtide.ugclite.ui.adapter.MediaPagerAdapter;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.ActivityPostDetailBinding;
import com.limtide.ugclite.utils.LikeManager;
import com.limtide.ugclite.utils.FollowManager;
import com.limtide.ugclite.utils.MuteManager;

import java.lang.ref.WeakReference;
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

    // ViewPager回调
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    // 点赞管理器
    private LikeManager likeManager;

    // 关注管理器
    private FollowManager followManager;

    // 静音管理器
    private MuteManager muteManager;

    // 自动轮播相关
    private Handler autoPlayHandler;
    private Runnable autoPlayRunnable;
    private static final long AUTO_PLAY_DELAY = 4000; // 4秒
    private boolean isAutoPlaying = false;

  // 静音和手动打断相关
    private boolean isMuted = false;
    private boolean isManuallyInterrupted = false;
    private boolean isUserTouching = false;
    private int lastKnownPosition = -1;
    private long lastAutoPlayTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // 设置窗口转场动画
            setupWindowTransitions();

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

            // 初始化关注管理器
            followManager = FollowManager.getInstance(this);

            // 初始化静音管理器
            initMuteManager();

            // 初始化自动轮播Handler
            initAutoPlay();

            // 初始化界面
            initViews();
            setupClickListeners();
            setupViewPager();

            // 初始化点赞状态
            initLikeStatus();

            // 初始化关注状态
            initFollowStatus();

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
     * 初始化关注状态
     */
    private void initFollowStatus() {
        if (currentPost == null || currentPost.author == null || followManager == null) {
            return;
        }

        // 从FollowManager获取关注状态
        isFollowing = followManager.isUserFollowed(currentPost.author.userId);

        // 更新UI显示
        updateFollowButton();

        Log.d(TAG, "初始化关注状态 - UserId: " + currentPost.author.userId +
                  ", IsFollowing: " + isFollowing);
    }

    /**
     * 初始化静音管理器
     */
    private void initMuteManager() {
        muteManager = MuteManager.getInstance(this);
        isMuted = muteManager.isMuted();

        // 设置静音状态变化监听器
        muteManager.setOnMuteStateChangeListener(new MuteManager.OnMuteStateChangeListener() {
            @Override
            public void onMuteStateChanged(boolean muted) {
                isMuted = muted;
                updateVolumeButtonIcon();
                applyMuteStateToAllMedia();

                Log.d(TAG, "静音状态变化: " + muted);

                // 静音状态变化时的播控逻辑
                if (isMuted) {
                    // 静音时停止自动轮播
                    stopAutoPlay();
                } else {
                    // 取消静音时，如果用户没有手动打断，恢复自动轮播
                    if (!isManuallyInterrupted && !isUserTouching) {
                        startAutoPlay();
                    }
                }
            }
        });

        Log.d(TAG, "静音管理器初始化完成，当前状态: " + isMuted);
    }

    /**
     * 初始化自动轮播
     */
    private void initAutoPlay() {
        autoPlayHandler = new Handler(Looper.getMainLooper());
        autoPlayRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaClips != null && mediaClips.size() > 1 && isAutoPlaying) {
                    // 更新自动轮播时间戳
                    lastAutoPlayTime = System.currentTimeMillis();

                    // 计算下一页的位置，循环播放
                    int nextPosition = (currentMediaPosition + 1) % mediaClips.size();

                    // 切换到下一页
                    if (binding != null && binding.viewPager != null) {
                        binding.viewPager.setCurrentItem(nextPosition, true);
                    }

                    Log.d(TAG, "Auto play switched to position: " + nextPosition + " at time: " + lastAutoPlayTime);

                    // 继续下一次轮播
                    autoPlayHandler.postDelayed(this, AUTO_PLAY_DELAY);
                }
            }
        };
    }

    /**
     * 开始自动轮播
     */
    private void startAutoPlay() {
        if (mediaClips != null && mediaClips.size() > 1 && !isAutoPlaying && !isMuted && !isManuallyInterrupted) {
            isAutoPlaying = true;
            lastAutoPlayTime = System.currentTimeMillis();
            autoPlayHandler.postDelayed(autoPlayRunnable, AUTO_PLAY_DELAY);
            Log.d(TAG, "Auto play started at time: " + lastAutoPlayTime);
        }
    }

    /**
     * 停止自动轮播
     */
    private void stopAutoPlay() {
        if (isAutoPlaying) {
            isAutoPlaying = false;
            autoPlayHandler.removeCallbacks(autoPlayRunnable);
            Log.d(TAG, "Auto play stopped");
        }
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
                Log.d(TAG, "=== POST DATA LOG START ===");
                Log.d(TAG, "Post对象获取成功: " + currentPost.title);
                Log.d(TAG, "Post ID: " + currentPost.postId);
                Log.d(TAG, "Post Title: " + currentPost.title);
                Log.d(TAG, "Post Content: " + currentPost.content);
                Log.d(TAG, "Post Create Time: " + currentPost.createTime);

                // 打印作者信息
                if (currentPost.author != null) {
                    Log.d(TAG, "Author ID: " + currentPost.author.userId);
                    Log.d(TAG, "Author Nickname: " + currentPost.author.nickname);
                    Log.d(TAG, "Author Avatar URL: " + currentPost.author.avatarUrl);
                } else {
                    Log.d(TAG, "Author: null");
                }

                // 打印话题标签信息
                if (currentPost.hashtags != null && !currentPost.hashtags.isEmpty()) {
                    Log.d(TAG, "Hashtags count: " + currentPost.hashtags.size());
                    for (int i = 0; i < currentPost.hashtags.size(); i++) {
                        Post.Hashtag hashtag = currentPost.hashtags.get(i);
                        Log.d(TAG, "Hashtag[" + i + "]: start=" + hashtag.start + ", end=" + hashtag.end);
                        if (currentPost.content != null && hashtag.start >= 0 && hashtag.end <= currentPost.content.length()) {
                            String tagText = currentPost.content.substring(hashtag.start, hashtag.end);
                            Log.d(TAG, "Hashtag[" + i + "] text: " + tagText);
                        }
                    }
                } else {
                    Log.d(TAG, "Hashtags: null or empty");
                }

                // 打印媒体片段信息
                if (currentPost.clips != null && !currentPost.clips.isEmpty()) {
                    Log.d(TAG, "Media clips count: " + currentPost.clips.size());
                    for (int i = 0; i < currentPost.clips.size(); i++) {
                        Post.Clip clip = currentPost.clips.get(i);
                        Log.d(TAG, "Clip[" + i + "]: type=" + clip.type + ", width=" + clip.width + ", height=" + clip.height + ", url=" + clip.url);
                        Log.d(TAG, "Clip[" + i + "] aspect ratio: " + clip.getAspectRatio());
                    }
                } else {
                    Log.d(TAG, "Media clips: null or empty");
                }

                // 打印音乐信息
                if (currentPost.music != null) {
                    Log.d(TAG, "Music volume: " + currentPost.music.volume);
                    Log.d(TAG, "Music seek time: " + currentPost.music.seekTime);
                    Log.d(TAG, "Music URL: " + currentPost.music.url);
                } else {
                    Log.d(TAG, "Music: null");
                }

                Log.d(TAG, "=== POST DATA LOG END ===");

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
            // updateFollowButton(); // 移到initFollowStatus中统一调用
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

        // 设置媒体点击事件监听器
        mediaPagerAdapter.setOnMediaClickListener(new MediaPagerAdapter.OnMediaClickListener() {
            @Override
            public void onImageClick(Post.Clip clip, int position) {
                handleImageClick(clip, position);
            }

            @Override
            public void onVideoClick(Post.Clip clip, int position) {
                handleVideoClick(clip, position);
            }

            @Override
            public void onVideoPlay(Post.Clip clip, int position) {
                handleVideoPlay(clip, position);
            }

            @Override
            public void onVideoPause(Post.Clip clip, int position) {
                handleVideoPause(clip, position);
            }
        });

        binding.viewPager.setAdapter(mediaPagerAdapter);

        // 设置进度条指示器
        setupProgressIndicator();

        // 监听ViewPager页面变化 - 使用安全的回调避免内存泄漏
        pageChangeCallback = new SafePageChangeCallback(this);
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback);

        // 设置当前页
        binding.viewPager.setCurrentItem(currentMediaPosition, false);

        // 初始化位置信息
        lastKnownPosition = currentMediaPosition;

        // 预加载当前页面的图片以确保转场动画流畅
        preloadCurrentPageImage();

        // 启动自动轮播
        startAutoPlay();
    }

    /**
     * 预加载当前页面的图片以确保转场动画流畅
     */
    private void preloadCurrentPageImage() {
        if (mediaClips == null || mediaClips.isEmpty()) {
            return;
        }

        Post.Clip currentClip = mediaClips.get(currentMediaPosition);
        if (currentClip != null && currentClip.type == 0 && currentClip.url != null) {
            // 使用Glide预加载首图 - 使用安全回调避免内存泄漏
            Glide.with(this)
                    .load(currentClip.url)
                    .listener(new SafeGlideRequestListener(this, currentClip.url, binding.viewPager))
                    .preload();
        }
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
                hashtagSpan.setSpan(new SafeClickableSpan(this, hashtagText), 0, hashtagText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

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

            if (isFollowing) {
                // 已关注状态：灰色文字，灰色边框
                binding.follow.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                binding.follow.setBackgroundResource(R.drawable.follow_button_gray_border);
            } else {
                // 未关注状态：红色文字，红色边框
                binding.follow.setTextColor(ContextCompat.getColor(this, R.color.holo_red_light));
                binding.follow.setBackgroundResource(R.drawable.follow_button_red_border);
            }
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
     * 设置窗口转场动画
     */
    private void setupWindowTransitions() {
        // 设置共享元素进入转场
        TransitionSet enterTransition = new TransitionSet();
        enterTransition.addTransition(new ChangeBounds());
        enterTransition.addTransition(new ChangeTransform());
        enterTransition.addTransition(new ChangeImageTransform());
        enterTransition.setDuration(400);
        enterTransition.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

        // 设置共享元素退出转场
        TransitionSet returnTransition = new TransitionSet();
        returnTransition.addTransition(new ChangeBounds());
        returnTransition.addTransition(new ChangeTransform());
        returnTransition.addTransition(new ChangeImageTransform());
        returnTransition.setDuration(350);
        returnTransition.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

        // 设置窗口转场
        getWindow().setSharedElementEnterTransition(enterTransition);
        getWindow().setSharedElementReturnTransition(returnTransition);

        // 设置非共享元素（背景区域）的渐显效果
        Fade fade = new Fade(Fade.IN);
        fade.addTarget(R.id.top_bar);
        fade.addTarget(R.id.content_container);
        fade.addTarget(R.id.bottom_interaction_bar);
        fade.addTarget(R.id.home_indicator_container);
        fade.setDuration(300);
        fade.setStartDelay(200); // 延迟开始，让图片放大动画先执行
        fade.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

        getWindow().setEnterTransition(fade);
        getWindow().setReturnTransition(fade);

        // 设置背景色过渡
        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(300);
        autoTransition.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

        // 设置ViewPager的转场名称
        ViewCompat.setTransitionName(binding.viewPager, "cover_image_transition");

        Log.d(TAG, "Window transitions setup completed");
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {

        // 返回按钮 - 直接返回上一页
        binding.backButton.setOnClickListener(v -> {
            Log.d(TAG, "back button clicked");
            // 使用共享元素返回动画，替代之前的淡出效果
            finish();
            // 不再需要手动设置overridePendingTransition，因为系统会处理共享元素转场
        });

        // 关注按钮点击事件
        if (binding.follow != null) {
            binding.follow.setOnClickListener(v -> {
                Log.d(TAG, "follow button clicked");
                handleFollowClick();
            });
        }

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
                handleShareClick();
            });
        }

        // 音量按钮点击事件
        if (binding.volumeIcon != null) {
            binding.volumeIcon.setOnClickListener(v -> {
                Log.d(TAG, "Volume button clicked");
                handleVolumeClick();
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
     * 处理关注点击
     */
    private void handleFollowClick() {
        if (currentPost == null || currentPost.author == null || followManager == null) {
            return;
        }

        String userId = currentPost.author.userId;
        if (userId == null || userId.trim().isEmpty()) {
            Log.w(TAG, "User ID is null or empty, cannot follow");
            return;
        }

        // 使用FollowManager切换关注状态
        boolean newFollowStatus = followManager.toggleFollow(userId);
        isFollowing = newFollowStatus;

        // 更新UI显示
        updateFollowButton();

        Log.d(TAG, "Follow clicked - UserId: " + userId +
                  ", NewStatus: " + newFollowStatus +
                  ", TotalFollowedUsers: " + followManager.getAllFollowedUsers().size());

        Toast.makeText(this, isFollowing ? "已关注" : "取消关注", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理分享点击
     */
    private void handleShareClick() {
        if (currentPost == null) {
            Log.w(TAG, "Cannot share: currentPost is null");
            return;
        }

        try {
            // 构建分享内容
            String shareText = buildShareText();

            // 创建分享Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentPost.title != null ? currentPost.title : "分享内容");

            // 启动分享选择器
            startActivity(Intent.createChooser(shareIntent, "分享作品"));

            Log.d(TAG, "Share initiated for post: " + currentPost.title);
            Toast.makeText(this, "分享成功", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error sharing post: " + e.getMessage(), e);
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 构建分享文本内容
     */
    private String buildShareText() {
        StringBuilder shareText = new StringBuilder();

        // 添加标题
        if (currentPost.title != null && !currentPost.title.trim().isEmpty()) {
            shareText.append(currentPost.title).append("\n\n");
        }

        // 添加正文内容
        if (currentPost.content != null && !currentPost.content.trim().isEmpty()) {
            shareText.append(currentPost.content);
        }

        // 添加作者信息
        if (currentPost.author != null && currentPost.author.nickname != null) {
            shareText.append("\n\n— 作者: ").append(currentPost.author.nickname);
        }

        // 添加话题标签
        if (currentPost.hashtags != null && !currentPost.hashtags.isEmpty() && currentPost.content != null) {
            shareText.append("\n");
            for (Post.Hashtag hashtag : currentPost.hashtags) {
                try {
                    // 从content中根据位置提取话题标签文本
                    if (hashtag.start >= 0 && hashtag.end <= currentPost.content.length() && hashtag.start < hashtag.end) {
                        String tagText = currentPost.content.substring(hashtag.start, hashtag.end);
                        if (tagText != null && !tagText.trim().isEmpty()) {
                            shareText.append(" ").append(tagText); // 话题标签本身就包含#号
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error extracting hashtag text: " + e.getMessage());
                }
            }
        }

        return shareText.toString();
    }

    /**
     * 处理音量按钮点击
     */
    private void handleVolumeClick() {
        // 使用MuteManager切换静音状态
        boolean newMutedState = muteManager.toggleMute();

        // 显示提示信息
        Toast.makeText(this, newMutedState ? "已静音" : "已取消静音", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "音量按钮点击，新状态: " + newMutedState);
    }

    /**
     * 更新音量按钮图标
     */
    private void updateVolumeButtonIcon() {
        if (binding.volumeIcon != null) {
            int iconResource = muteManager.getMuteIconResource();
            binding.volumeIcon.setImageResource(iconResource);
            Log.d(TAG, "更新音量按钮图标: " + (isMuted ? "静音" : "非静音"));
        }
    }

    /**
     * 应用静音状态到所有媒体播放器
     */
    private void applyMuteStateToAllMedia() {
        // 应用到视频播放器
        muteAllVideoPlayers();

        // 应用到音乐播放器（如果有）
        if (currentPost != null && currentPost.music != null && mediaPagerAdapter != null) {
            // 这里可以添加音乐播放器的静音逻辑
            Log.d(TAG, "应用静音状态到音乐播放器: " + isMuted);
        }

        Log.d(TAG, "静音状态已应用到所有媒体: " + isMuted);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "PostDetailActivity resumed - 恢复播放状态");

        // 从MuteManager获取最新状态
        isMuted = muteManager.isMuted();

        // 更新音量按钮图标
        updateVolumeButtonIcon();

        // 恢复媒体播放（应用当前的静音状态）
        resumeMediaPlayback();

        // Activity重新可见时重置用户触摸状态
        isUserTouching = false;

        // 如果不在静音状态且用户没有手动打断，恢复自动轮播
        if (!isMuted && !isManuallyInterrupted && !isUserTouching) {
            startAutoPlay();
        }

        Log.d(TAG, "onResume完成 - isMuted: " + isMuted + ", isManuallyInterrupted: " + isManuallyInterrupted);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "PostDetailActivity paused - 暂停播放状态");

        // 暂停媒体播放
        pauseMediaPlayback();

        // Activity不可见时停止自动轮播
        stopAutoPlay();
    }

    /**
     * 恢复媒体播放
     * 从后台或返回详情页时调用
     */
    private void resumeMediaPlayback() {
        Log.d(TAG, "恢复媒体播放，静音状态: " + isMuted);

        // 应用静音状态到所有媒体播放器
        applyMuteStateToAllMedia();

        // 如果有音乐，恢复音乐播放（非静音状态下）
        if (currentPost != null && currentPost.music != null && !isMuted) {
            // 这里可以添加音乐播放器的恢复逻辑
            Log.d(TAG, "恢复音乐播放");
        }

        // 视频播放器会根据用户交互自动播放
        // 这里不需要强制恢复视频播放，让用户主动触发
    }

    /**
     * 暂停媒体播放
     * 退后台或关闭页面时调用
     */
    private void pauseMediaPlayback() {
        Log.d(TAG, "暂停媒体播放");

        // 暂停所有视频播放
        pauseAllVideoPlayers();

        // 如果有音乐，暂停音乐播放
        if (currentPost != null && currentPost.music != null) {
            // 这里可以添加音乐播放器的暂停逻辑
            Log.d(TAG, "暂停音乐播放");
        }
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

        // 停止自动轮播
        stopAutoPlay();

        // 清理Handler
        if (autoPlayHandler != null) {
            autoPlayHandler.removeCallbacksAndMessages(null);
            autoPlayHandler = null;
        }

        // 注销ViewPager2回调以防止内存泄漏
        if (binding != null && binding.viewPager != null && pageChangeCallback != null) {
            try {
                binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
                pageChangeCallback = null;
                Log.d(TAG, "ViewPager2 callback unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering ViewPager2 callback: " + e.getMessage(), e);
            }
        }

        // 释放所有视频播放器资源
        releaseAllVideoPlayers();

        // 清理Glide请求以防止内存泄漏 - 使用ApplicationContext确保安全
        if (binding != null) {
            try {
                // 使用ApplicationContext而不是Activity Context
                Glide.with(getApplicationContext()).clear(binding.viewPager);
                Glide.with(getApplicationContext()).clear(binding.userAvatar);
                Log.d(TAG, "Glide requests cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing Glide requests: " + e.getMessage(), e);
            }
        }

        // 清理ViewBinding
        binding = null;

        // 清理静音管理器监听器
        if (muteManager != null) {
            muteManager.setOnMuteStateChangeListener(null);
        }

        // 清理管理器引用（如果需要的话）
        // likeManager = null; // 单例通常不需要手动清理
        // followManager = null;
        // muteManager = null; // 单例通常不需要手动清理
    }

    /**
     * 安全的ViewPager2页面变化回调 - 使用WeakReference避免内存泄漏
     */
    private static class SafePageChangeCallback extends ViewPager2.OnPageChangeCallback {
        private final WeakReference<PostDetailActivity> activityRef;

        SafePageChangeCallback(PostDetailActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onPageSelected(int position) {
            PostDetailActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long timeSinceLastAutoPlay = currentTime - activity.lastAutoPlayTime;

            // 判断是否为手动滑动（如果距离上次自动轮播时间很短，说明是自动轮播；否则是手动滑动）
            boolean isManualSwipe = timeSinceLastAutoPlay > 1000; // 1秒内的滑动认为是自动轮播

            activity.currentMediaPosition = position;

            // 安全地更新进度条指示器
            try {
                if (activity.binding != null && activity.binding.tabIndicator != null) {
                    TabLayout.Tab selectedTab = activity.binding.tabIndicator.getTabAt(position);
                    if (selectedTab != null) {
                        selectedTab.select();
                    }
                }
            } catch (Exception e) {
                Log.e(activity.TAG, "Error updating progress indicator: " + e.getMessage(), e);
            }

            // 如果是手动滑动且当前正在自动轮播，则打断轮播
            if (isManualSwipe && activity.isAutoPlaying && !activity.isManuallyInterrupted) {
                activity.isManuallyInterrupted = true;
                activity.stopAutoPlay();
                Log.d(activity.TAG, "Auto play manually interrupted by user swipe");
            }

            // 如果是手动滑动，自动静音
            if (isManualSwipe && !activity.isMuted) {
                activity.isMuted = true;
                activity.updateVolumeButtonIcon();
                activity.muteAllVideoPlayers();
                Log.d(activity.TAG, "Auto muted due to user swipe");
            }

            Log.d(activity.TAG, "Media page changed to: " + position +
                      ", isManualSwipe: " + isManualSwipe +
                      ", timeSinceLastAutoPlay: " + timeSinceLastAutoPlay +
                      ", isMuted: " + activity.isMuted +
                      ", isManuallyInterrupted: " + activity.isManuallyInterrupted);
        }
    }

    /**
     * 安全的Glide请求监听器 - 使用WeakReference避免内存泄漏
     */
    private static class SafeGlideRequestListener implements com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
        private final WeakReference<PostDetailActivity> activityRef;
        private final String imageUrl;
        private final WeakReference<ViewPager2> viewPagerRef;

        SafeGlideRequestListener(PostDetailActivity activity, String imageUrl, ViewPager2 viewPager) {
            this.activityRef = new WeakReference<>(activity);
            this.imageUrl = imageUrl;
            this.viewPagerRef = new WeakReference<>(viewPager);
        }

        @Override
        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                  Object model,
                                  com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                  boolean isFirstResource) {
            PostDetailActivity activity = activityRef.get();
            if (activity != null) {
                Log.w(activity.TAG, "Failed to preload transition image: " + imageUrl);
            }
            return false;
        }

        @Override
        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                     Object model,
                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                     com.bumptech.glide.load.DataSource dataSource,
                                     boolean isFirstResource) {
            PostDetailActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return false;
            }

            Log.d(activity.TAG, "Transition image preloaded successfully: " + imageUrl);

            // 图片加载完成后，延迟启动转场动画以确保流畅性
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ViewPager2 viewPager = viewPagerRef.get();
                if (viewPager != null) {
                    activity.postponeEnterTransition();
                    // 给图片一点时间渲染
                    viewPager.post(() -> {
                        PostDetailActivity currentActivity = activityRef.get();
                        if (currentActivity != null && !currentActivity.isFinishing() && !currentActivity.isDestroyed()) {
                            currentActivity.startPostponedEnterTransition();
                        }
                    });
                }
            }
            return false;
        }
    }

    /**
     * 安全的ClickableSpan - 使用WeakReference避免内存泄漏
     */
    private static class SafeClickableSpan extends ClickableSpan {
        private final WeakReference<PostDetailActivity> activityRef;
        private final String hashtagText;

        SafeClickableSpan(PostDetailActivity activity, String hashtagText) {
            this.activityRef = new WeakReference<>(activity);
            this.hashtagText = hashtagText;
        }

        @Override
        public void onClick(@NonNull View widget) {
            PostDetailActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            try {
                // 点击话题标签跳转
                Intent intent = new Intent(activity, HashtagActivity.class);
                intent.putExtra("hashtag", hashtagText);
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(activity.TAG, "Error starting hashtag activity: " + e.getMessage(), e);
            }
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false); // 移除下划线
            ds.setColor(Color.parseColor("#1890FF")); // 设置颜色
        }
    }

    // ==================== 视频播放事件处理 ====================

    /**
     * 处理图片点击事件
     */
    private void handleImageClick(Post.Clip clip, int position) {
        Log.d(TAG, "Image clicked: " + clip.url + " at position " + position);
        // 暂停自动轮播
        stopAutoPlay();
        // 可以在这里添加图片放大查看等功能
    }

    /**
     * 处理视频点击事件
     */
    private void handleVideoClick(Post.Clip clip, int position) {
        Log.d(TAG, "Video clicked: " + clip.url + " at position " + position);
        // 暂停自动轮播
        stopAutoPlay();
        // 视频播放状态切换在MediaPagerAdapter中处理
    }

    /**
     * 处理视频播放事件
     */
    private void handleVideoPlay(Post.Clip clip, int position) {
        Log.d(TAG, "Video started: " + clip.url + " at position " + position);
        // 视频开始播放时，暂停自动轮播
        stopAutoPlay();

        // 如果视频播放时是静音状态，可以在这里处理音量
        if (isMuted) {
            // 视频播放时保持静音状态
            Log.d(TAG, "Video playing with muted state");
        }
    }

    /**
     * 处理视频暂停事件
     */
    private void handleVideoPause(Post.Clip clip, int position) {
        Log.d(TAG, "Video paused: " + clip.url + " at position " + position);
        // 视频暂停时，如果不在手动打断状态，可以恢复自动轮播
        if (!isManuallyInterrupted && !isUserTouching) {
            startAutoPlay();
        }
    }

    /**
     * 释放所有视频播放器资源
     */
    private void releaseAllVideoPlayers() {
        if (mediaPagerAdapter != null) {
            mediaPagerAdapter.releaseAllVideos();
        }
    }

    /**
     * 暂停所有视频播放
     */
    private void pauseAllVideoPlayers() {
        if (mediaPagerAdapter != null) {
            mediaPagerAdapter.pauseAllVideos();
        }
    }

    /**
     * 静音所有视频播放器
     */
    private void muteAllVideoPlayers() {
        if (mediaPagerAdapter != null) {
            // 通过RecyclerView来访问ViewHolder
            RecyclerView recyclerView = (RecyclerView) binding.viewPager.getChildAt(0);
            if (recyclerView != null) {
                for (int i = 0; i < recyclerView.getAdapter().getItemCount(); i++) {
                    RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                    if (holder instanceof MediaPagerAdapter.VideoViewHolder) {
                        MediaPagerAdapter.VideoViewHolder videoHolder = (MediaPagerAdapter.VideoViewHolder) holder;
                        if (videoHolder.getVideoPlayerView() != null) {
                            videoHolder.getVideoPlayerView().setMuted(isMuted);
                        }
                    }
                }
            }
        }
    }
}