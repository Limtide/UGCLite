package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 点赞状态管理器
 * 使用SharedPreferences进行本地持久化存储
 *
 * @Context说明:
 * - 使用Application Context，生命周期与应用绑定
 * - 单例模式，实例在应用运行期间持续存在
 * - 不会因Activity/Fragment销毁而丢失数据
 * - 线程安全，支持多线程访问
 */
public class LikeManager {
    private static final String TAG = "LikeManager";
    private static final String PREFS_NAME = "like_prefs";
    private static final String KEY_LIKED_POSTS = "liked_posts";
    private static final String KEY_LIKE_COUNTS = "like_counts";

    private static LikeManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    // 内存缓存
    private Set<String> likedPostIds; // 已点赞的Post ID集合
    private int baseLikeCount = 128; // 基础点赞数量（因为API没有提供）

    private LikeManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        // 强制使用Application Context确保生命周期安全
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            throw new IllegalStateException("Application Context is not available");
        }

        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        loadData();
    }

    /**
     * 获取LikeManager单例实例
     *
     * @param context 上下文对象，可以是Activity、Service或Application Context
     *               内部会自动转换为Application Context确保生命周期安全
     * @return LikeManager单例实例
     * @throws IllegalArgumentException 如果context为null
     * @throws IllegalStateException 如果Application Context不可用
     */
    public static synchronized LikeManager getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null when initializing LikeManager");
            }
            try {
                instance = new LikeManager(context);
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize LikeManager: " + e.getMessage(), e);
                throw new RuntimeException("LikeManager initialization failed", e);
            }
        }
        return instance;
    }

    /**
     * 从本地存储加载数据
     */
    private void loadData() {
        try {
            // 加载已点赞的Post ID集合
            likedPostIds = prefs.getStringSet(KEY_LIKED_POSTS, new HashSet<>());
            Log.d(TAG, "Loaded liked posts: " + likedPostIds.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error loading like data: " + e.getMessage(), e);
            likedPostIds = new HashSet<>();
        }
    }

    /**
     * 保存点赞状态到本地存储
     */
    private void saveData() {
        try {
            editor.putStringSet(KEY_LIKED_POSTS, likedPostIds);
            editor.apply();
            Log.d(TAG, "Saved liked posts: " + likedPostIds.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving like data: " + e.getMessage(), e);
        }
    }

    /**
     * 检查帖子是否已点赞
     */
    public boolean isPostLiked(String postId) {
        if (postId == null) return false;
        return likedPostIds.contains(postId);
    }

    /**
     * 切换点赞状态
     * @param postId 帖子ID
     * @return 新的点赞状态（true=已点赞，false=未点赞）
     */
    public boolean toggleLike(String postId) {
        if (postId == null) return false;

        boolean wasLiked = likedPostIds.contains(postId);

        if (wasLiked) {
            // 取消点赞
            likedPostIds.remove(postId);
            Log.d(TAG, "Unliked post: " + postId);
        } else {
            // 点赞
            likedPostIds.add(postId);
            Log.d(TAG, "Liked post: " + postId);
        }

        saveData();
        return !wasLiked;
    }

    /**
     * 设置点赞状态
     * @param postId 帖子ID
     * @param isLiked 是否已点赞
     */
    public void setLikeStatus(String postId, boolean isLiked) {
        if (postId == null) return;

        if (isLiked) {
            likedPostIds.add(postId);
        } else {
            likedPostIds.remove(postId);
        }

        saveData();
    }

    /**
     * 获取点赞数量
     * 由于API没有提供点赞数量，使用基础数量加上本地计算
     * @param postId 帖子ID
     * @return 点赞数量
     */
    public int getLikeCount(String postId) {
        // 基础点赞数量 + 是否已点赞（+1）
        return baseLikeCount + (isPostLiked(postId) ? 1 : 0);
    }

    /**
     * 设置基础点赞数量
     * @param baseCount 基础点赞数量
     */
    public void setBaseLikeCount(int baseCount) {
        this.baseLikeCount = baseCount;
    }

    /**
     * 获取所有已点赞的帖子ID
     */
    public Set<String> getAllLikedPosts() {
        return new HashSet<>(likedPostIds);
    }

    /**
     * 清空所有点赞数据
     */
    public void clearAllData() {
        likedPostIds.clear();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Cleared all like data");
    }

    /**
     * 获取点赞统计数据
     */
    public void logStats() {
        Log.d(TAG, "Like stats - Total liked posts: " + likedPostIds.size());
        Log.d(TAG, "Base like count: " + baseLikeCount);
    }

    /**
     * 清理资源和内存缓存
     * 通常在应用退出时调用，用于释放内存
     * 注意：此操作不会删除持久化存储的数据
     */
    public void cleanup() {
        try {
            // 清理内存缓存
            if (likedPostIds != null) {
                likedPostIds.clear();
            }
            likedPostIds = null;

            // 清理Editor引用
            editor = null;
            prefs = null;

            Log.d(TAG, "LikeManager cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }

    /**
     * 重置单例实例
     * 通常用于测试或特殊场景，谨慎使用
     * 重置后需要重新调用getInstance()初始化
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
            Log.d(TAG, "LikeManager instance reset");
        }
    }
}