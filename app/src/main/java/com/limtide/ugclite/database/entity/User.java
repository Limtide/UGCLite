package com.limtide.ugclite.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

/**
 * 用户实体类
 * 使用Room数据库存储用户信息
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    private String username; // 用户名作为主键

    private String password; // 密码
    private String nickname; // 昵称
    private String signature; // 个人签名
    private String avatarUrl; // 头像URL
    private long createTime; // 创建时间
    private long lastLoginTime; // 最后登录时间
    private boolean isActive; // 账号是否激活

    // 默认构造函数
    public User() {
        this.createTime = System.currentTimeMillis();
        this.lastLoginTime = 0;
        this.isActive = true;
    }

    // 带参数的构造函数 - Room忽略
    @Ignore
    public User(@NonNull String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // 完整参数构造函数 - Room忽略
    @Ignore
    public User(@NonNull String username, String password, String nickname, String signature, String avatarUrl) {
        this(username, password);
        this.nickname = nickname;
        this.signature = signature;
        this.avatarUrl = avatarUrl;
    }

    // Getter 和 Setter 方法
    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // 更新最后登录时间
    public void updateLastLoginTime() {
        this.lastLoginTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", signature='" + signature + '\'' +
                ", createTime=" + createTime +
                ", lastLoginTime=" + lastLoginTime +
                ", isActive=" + isActive +
                '}';
    }

    // 检查用户名和密码是否匹配
    public boolean checkPassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }

    // 获取显示名称（优先使用昵称，否则使用用户名）
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }
}