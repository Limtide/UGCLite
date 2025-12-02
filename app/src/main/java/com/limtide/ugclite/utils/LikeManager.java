package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 点赞状态管理器
 * 使用SharedPreferences进行本地持久化存储
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
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        loadData();
    }

    public static synchronized LikeManager getInstance(Context context) {
        if (instance == null) {
            instance = new LikeManager(context);
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
}