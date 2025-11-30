package com.limtide.ugclite.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import androidx.lifecycle.LiveData;

import com.limtide.ugclite.database.entity.User;

import java.util.List;

/**
 * 用户数据访问对象
 * 提供用户数据的CRUD操作
 */
@Dao
public interface UserDao {

    /**
     * 插入新用户（如果用户名已存在则替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(User user);

    /**
     * 插入多个用户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertUsers(List<User> users);

    /**
     * 根据用户名查询用户
     */
    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    /**
     * 根据用户名查询用户（返回LiveData）
     */
    @Query("SELECT * FROM users WHERE username = :username")
    LiveData<User> getUserByUsernameLiveData(String username);

    /**
     * 根据用户名和密码验证用户登录
     */
    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND isActive = 1")
    User loginUser(String username, String password);

    /**
     * 检查用户名是否已存在
     */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int checkUsernameExists(String username);

    /**
     * 获取所有用户
     */
    @Query("SELECT * FROM users ORDER BY createTime DESC")
    List<User> getAllUsers();

    /**
     * 获取所有活跃用户
     */
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY lastLoginTime DESC")
    LiveData<List<User>> getAllActiveUsers();

    /**
     * 更新用户信息
     */
    @Update
    int updateUser(User user);

    /**
     * 更新用户密码
     */
    @Query("UPDATE users SET password = :newPassword WHERE username = :username")
    int updateUserPassword(String username, String newPassword);

    /**
     * 更新用户昵称
     */
    @Query("UPDATE users SET nickname = :nickname WHERE username = :username")
    int updateUserNickname(String username, String nickname);

    /**
     * 更新用户签名
     */
    @Query("UPDATE users SET signature = :signature WHERE username = :username")
    int updateUserSignature(String username, String signature);

    /**
     * 更新用户头像
     */
    @Query("UPDATE users SET avatarUrl = :avatarUrl WHERE username = :username")
    int updateUserAvatar(String username, String avatarUrl);

    /**
     * 更新最后登录时间
     */
    @Query("UPDATE users SET lastLoginTime = :loginTime WHERE username = :username")
    int updateLastLoginTime(String username, long loginTime);

    /**
     * 激活用户账号
     */
    @Query("UPDATE users SET isActive = 1 WHERE username = :username")
    int activateUser(String username);

    /**
     * 禁用用户账号
     */
    @Query("UPDATE users SET isActive = 0 WHERE username = :username")
    int deactivateUser(String username);

    /**
     * 删除用户
     */
    @Delete
    int deleteUser(User user);

    /**
     * 根据用户名删除用户
     */
    @Query("DELETE FROM users WHERE username = :username")
    int deleteUserByUsername(String username);

    /**
     * 获取用户总数
     */
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    /**
     * 获取活跃用户总数
     */
    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    int getActiveUserCount();

    /**
     * 根据昵称搜索用户
     */
    @Query("SELECT * FROM users WHERE nickname LIKE '%' || :nickname || '%' AND isActive = 1")
    List<User> searchUsersByNickname(String nickname);

    /**
     * 清空所有用户数据
     */
    @Query("DELETE FROM users")
    void deleteAllUsers();
}