package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 关注状态管理器
 * 使用SharedPreferences进行本地持久化存储
 *
 * @Context说明:
 * - 使用Application Context，生命周期与应用绑定
 * - 单例模式，实例在应用运行期间持续存在
 * - 不会因Activity/Fragment销毁而丢失数据
 * - 线程安全，支持多线程访问
 */
public class FollowManager {
    private static final String TAG = "FollowManager";
    private static final String PREFS_NAME = "follow_prefs";
    private static final String KEY_FOLLOWED_USERS = "followed_users";

    private static FollowManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    // 内存缓存
    private Set<String> followedUserIds; // 已关注的用户ID集合

    private FollowManager(Context context) {
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
     * 获取FollowManager单例实例
     *
     * @param context 上下文对象，可以是Activity、Service或Application Context
     *               内部会自动转换为Application Context确保生命周期安全
     * @return FollowManager单例实例
     * @throws IllegalArgumentException 如果context为null
     * @throws IllegalStateException 如果Application Context不可用
     */
    public static synchronized FollowManager getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null when initializing FollowManager");
            }
            try {
                instance = new FollowManager(context);
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize FollowManager: " + e.getMessage(), e);
                throw new RuntimeException("FollowManager initialization failed", e);
            }
        }
        return instance;
    }

    /**
     * 从本地存储加载数据
     */
    private void loadData() {
        try {
            // 加载已关注的用户ID集合
            followedUserIds = prefs.getStringSet(KEY_FOLLOWED_USERS, new HashSet<>());
            Log.d(TAG, "Loaded followed users: " + followedUserIds.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error loading follow data: " + e.getMessage(), e);
            followedUserIds = new HashSet<>();
        }
    }

    /**
     * 保存关注状态到本地存储
     */
    private void saveData() {
        try {
            editor.putStringSet(KEY_FOLLOWED_USERS, followedUserIds);
            editor.apply();
            Log.d(TAG, "Saved followed users: " + followedUserIds.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving follow data: " + e.getMessage(), e);
        }
    }

    /**
     * 检查用户是否已关注
     * @param userId 用户ID
     * @return 是否已关注
     */
    public boolean isUserFollowed(String userId) {
        if (userId == null) return false;
        return followedUserIds.contains(userId);
    }

    /**
     * 切换关注状态
     * @param userId 用户ID
     * @return 新的关注状态（true=已关注，false=未关注）
     */
    public boolean toggleFollow(String userId) {
        if (userId == null) return false;

        boolean wasFollowed = followedUserIds.contains(userId);

        if (wasFollowed) {
            // 取消关注
            followedUserIds.remove(userId);
            Log.d(TAG, "Unfollowed user: " + userId);
        } else {
            // 关注
            followedUserIds.add(userId);
            Log.d(TAG, "Followed user: " + userId);
        }

        saveData();
        return !wasFollowed;
    }

    /**
     * 设置关注状态
     * @param userId 用户ID
     * @param isFollowed 是否已关注
     */
    public void setFollowStatus(String userId, boolean isFollowed) {
        if (userId == null) return;

        if (isFollowed) {
            followedUserIds.add(userId);
        } else {
            followedUserIds.remove(userId);
        }

        saveData();
    }

    /**
     * 获取所有已关注的用户ID
     */
    public Set<String> getAllFollowedUsers() {
        return new HashSet<>(followedUserIds);
    }

    /**
     * 清空所有关注数据
     */
    public void clearAllData() {
        followedUserIds.clear();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Cleared all follow data");
    }

    /**
     * 获取关注总数
     */
    public int getFollowCount() {
        return followedUserIds.size();
    }

    /**
     * 获取关注统计数据
     */
    public void logStats() {
        Log.d(TAG, "Follow stats - Total followed users: " + followedUserIds.size());
    }

    /**
     * 清理资源和内存缓存
     * 通常在应用退出时调用，用于释放内存
     * 注意：此操作不会删除持久化存储的数据
     */
    public void cleanup() {
        try {
            // 清理内存缓存
            if (followedUserIds != null) {
                followedUserIds.clear();
            }
            followedUserIds = null;

            // 清理Editor引用
            editor = null;
            prefs = null;

            Log.d(TAG, "FollowManager cleaned up successfully");
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
            Log.d(TAG, "FollowManager instance reset");
        }
    }
}