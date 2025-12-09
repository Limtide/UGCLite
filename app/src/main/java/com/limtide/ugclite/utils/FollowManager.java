package com.limtide.ugclite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 关注状态管理器
 * 使用SharedPreferences进行本地持久化存储
 *
 * @Context说明:
 * - 使用Application Context，生命周期与应用绑定
 * - 单例模式，实例在应用运行期间持续存在
 * - 不会因Activity/Fragment销毁而丢失数据
 * - 线程安全，支持多线程访问
 *
 * @线程安全说明:
 * - 使用ConcurrentHashMap存储关注状态，支持并发读写
 * - 使用ReentrantReadWriteLock保护复合操作
 * - 使用volatile保证变量可见性
 * - 同步化SharedPreferences操作避免竞态条件
 */
public class FollowManager {
    private static final String TAG = "FollowManager";
    private static final String PREFS_NAME = "follow_prefs";
    private static final String KEY_FOLLOWED_USERS = "followed_users";

    private static volatile FollowManager instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    // 内存缓存 - 使用ConcurrentHashMap保证线程安全
    private ConcurrentHashMap<String, Boolean> followedUserIds; // 使用ConcurrentHashMap替代HashSet

    // 读写锁保护复合操作
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
    public static FollowManager getInstance(Context context) {
        // 双重检查锁定模式，确保线程安全的单例创建
        if (instance == null) {
            synchronized (FollowManager.class) {
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
            }
        }
        return instance;
    }

    /**
     * 从本地存储加载数据 - 线程安全
     */
    private void loadData() {
        try {
            // 检查SharedPreferences文件是否存在
            boolean hasPrefs = prefs.contains(KEY_FOLLOWED_USERS);
            Log.d(TAG, "SharedPreferences has follow data: " + hasPrefs);

            // 加载已关注的用户ID集合
            Set<String> savedFollows = prefs.getStringSet(KEY_FOLLOWED_USERS, new HashSet<>());
            Log.d(TAG, "Raw data from SharedPreferences: " + savedFollows.size() + " items, content: " + savedFollows);

            // 转换为ConcurrentHashMap保证线程安全
            followedUserIds = new ConcurrentHashMap<>();
            for (String userId : savedFollows) {
                followedUserIds.put(userId, true);
            }
            Log.d(TAG, "Loaded followed users: " + followedUserIds.size() + " items");

            // 验证数据是否正确加载
            if (followedUserIds.size() > 0) {
                Log.d(TAG, "Followed users loaded successfully: " + followedUserIds.keySet());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading follow data: " + e.getMessage(), e);
            followedUserIds = new ConcurrentHashMap<>();
        }
    }

    /**
     * 保存关注状态到本地存储 - 线程安全
     */
    private void saveData() {
        lock.readLock().lock();
        try {
            // 创建快照避免在保存过程中数据变化
            Set<String> snapshot = new HashSet<>(followedUserIds.keySet());

            // 使用新的Editor实例避免竞态，使用commit确保数据同步写入磁盘
            SharedPreferences.Editor newEditor = prefs.edit();
            newEditor.putStringSet(KEY_FOLLOWED_USERS, snapshot);
            boolean success = newEditor.commit();
            if (!success) {
                Log.e(TAG, "Failed to save follow data to SharedPreferences");
            }

            Log.d(TAG, "Saved followed users: " + snapshot.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving follow data: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查用户是否已关注 - 线程安全
     * @param userId 用户ID
     * @return 是否已关注
     */
    public boolean isUserFollowed(String userId) {
        if (userId == null) return false;

        lock.readLock().lock();
        try {
            return followedUserIds.containsKey(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 切换关注状态 - 线程安全，原子操作
     * @param userId 用户ID
     * @return 新的关注状态（true=已关注，false=未关注）
     */
    public boolean toggleFollow(String userId) {
        if (userId == null) return false;

        lock.writeLock().lock();
        try {
            // 原子性的检查-修改操作
            Boolean wasFollowed = followedUserIds.get(userId);
            boolean isCurrentlyFollowed = (wasFollowed != null && wasFollowed);

            if (isCurrentlyFollowed) {
                // 取消关注
                followedUserIds.remove(userId);
                Log.d(TAG, "Unfollowed user: " + userId);
            } else {
                // 关注
                followedUserIds.put(userId, true);
                Log.d(TAG, "Followed user: " + userId);
            }

            // 在写锁内保存数据，确保原子性
            saveDataUnsafe();
            return !isCurrentlyFollowed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 保存数据到本地存储 - 非线程安全版本，只在已获取锁的情况下调用
     */
    private void saveDataUnsafe() {
        try {
            Set<String> snapshot = new HashSet<>(followedUserIds.keySet());
            SharedPreferences.Editor newEditor = prefs.edit();
            newEditor.putStringSet(KEY_FOLLOWED_USERS, snapshot);
            boolean success = newEditor.commit();
            if (!success) {
                Log.e(TAG, "Failed to save follow data to SharedPreferences (unsafe)");
            }
            Log.d(TAG, "Saved followed users: " + snapshot.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving follow data: " + e.getMessage(), e);
        }
    }

    /**
     * 设置关注状态 - 线程安全
     * @param userId 用户ID
     * @param isFollowed 是否已关注
     */
    public void setFollowStatus(String userId, boolean isFollowed) {
        if (userId == null) return;

        lock.writeLock().lock();
        try {
            if (isFollowed) {
                followedUserIds.put(userId, true);
            } else {
                followedUserIds.remove(userId);
            }

            saveDataUnsafe();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取所有已关注的用户ID - 线程安全
     */
    public Set<String> getAllFollowedUsers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(followedUserIds.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空所有关注数据 - 线程安全
     */
    public void clearAllData() {
        lock.writeLock().lock();
        try {
            followedUserIds.clear();
            SharedPreferences.Editor newEditor = prefs.edit();
            newEditor.clear();
            boolean success = newEditor.commit();
            if (!success) {
                Log.e(TAG, "Failed to clear follow data from SharedPreferences");
            }
            Log.d(TAG, "Cleared all follow data");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取关注总数 - 线程安全
     */
    public int getFollowCount() {
        lock.readLock().lock();
        try {
            return followedUserIds.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取关注统计数据 - 线程安全
     */
    public void logStats() {
        lock.readLock().lock();
        try {
            Log.d(TAG, "Follow stats - Total followed users: " + followedUserIds.size());
        } finally {
            lock.readLock().unlock();
        }
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