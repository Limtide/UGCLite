package com.limtide.ugclite.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 作品实体类
 * 对应 JSON 中的 "post_list" 数组里的每一项
 */
public class Post implements Parcelable, Serializable {

    // --- 核心字段 ---

    public Post() {
        // 初始化默认值
        this.postId = "";
        this.title = "";
        this.content = "";
        this.createTime = System.currentTimeMillis() / 1000;
        this.author = null;
        this.hashtags = new ArrayList<>();
        this.clips = new ArrayList<>();
        this.music = null;
    }
    @SerializedName("post_id")
    public String postId;

    // 无参构造函数，用于PostDetailActivity中的备用创建

    @SerializedName("title")
    public String title;

    @SerializedName("content")
    public String content;

    /**
     * 注意：时间戳通常较大，建议使用 long 类型防止溢出
     */
    @SerializedName("create_time")
    public long createTime;

    // --- 嵌套对象 ---

    @SerializedName("author")
    public Author author;

    @SerializedName("hashtag")
    public List<Hashtag> hashtags;

    // 添加兼容性字段，当hashtag是单个对象时使用
    public void setHashtagFromObject(Hashtag singleHashtag) {
        if (hashtags == null) {
            hashtags = new ArrayList<>();
        }
        hashtags.clear();
        hashtags.add(singleHashtag);
    }

    /**
     * 这是一个列表，包含图片或视频片段
     */
    @SerializedName("clips")
    public List<Clip> clips;

    @SerializedName("music")
    public Music music;

    // ==========================================
    // Parcelable 接口实现
    // ==========================================

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // 安全写入String字段，null值会自动处理
        dest.writeString(postId != null ? postId : "");
        dest.writeString(title != null ? title : "");
        dest.writeString(content != null ? content : "");
        dest.writeLong(createTime);

        // 安全写入Parcelable字段
        dest.writeParcelable(author, flags);

        // 安全写入List字段 - writeTypedList会处理null情况
        dest.writeTypedList(hashtags);
        dest.writeTypedList(clips);

        dest.writeParcelable(music, flags);
    }

    protected Post(Parcel in) {
        postId = in.readString();
        title = in.readString();
        content = in.readString();
        createTime = in.readLong();
        author = in.readParcelable(Author.class.getClassLoader());

        // 安全读取List字段 - createTypedArrayList会处理null情况
        hashtags = in.createTypedArrayList(Hashtag.CREATOR);
        clips = in.createTypedArrayList(Clip.CREATOR);

        music = in.readParcelable(Music.class.getClassLoader());
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    // 为内部类也实现Parcelable接口
    public static class Author implements Parcelable, Serializable {
        @SerializedName("user_id")
        public String userId;

        @SerializedName("nickname")
        public String nickname;

        @SerializedName("avatar")
        public String avatarUrl;

        // 无参构造函数
        public Author() {
            this.userId = "";
            this.nickname = "";
            this.avatarUrl = "";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(userId);
            dest.writeString(nickname);
            dest.writeString(avatarUrl);
        }

        protected Author(Parcel in) {
            userId = in.readString();
            nickname = in.readString();
            avatarUrl = in.readString();
        }

        public static final Creator<Author> CREATOR = new Creator<Author>() {
            @Override
            public Author createFromParcel(Parcel in) {
                return new Author(in);
            }

            @Override
            public Author[] newArray(int size) {
                return new Author[size];
            }
        };
    }

    public static class Hashtag implements Parcelable, Serializable {
        @SerializedName("start")
        public int start; // 高亮起始位置

        @SerializedName("end")
        public int end;   // 高亮结束位置

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(start);
            dest.writeInt(end);
        }

        protected Hashtag(Parcel in) {
            start = in.readInt();
            end = in.readInt();
        }

        public static final Creator<Hashtag> CREATOR = new Creator<Hashtag>() {
            @Override
            public Hashtag createFromParcel(Parcel in) {
                return new Hashtag(in);
            }

            @Override
            public Hashtag[] newArray(int size) {
                return new Hashtag[size];
            }
        };
    }

    public static class Clip implements Parcelable, Serializable {
        @SerializedName("type")
        public int type; // 0：图片，1：视频，2：音频(MP3)

        @SerializedName("width")
        public int width;

        @SerializedName("height")
        public int height;

        @SerializedName("url")
        public String url;

        /**
         * 这是一个辅助方法（非 JSON 字段），用于瀑布流计算
         * 防止除以 0 导致崩溃
         */
        public float getAspectRatio() {
            if (height == 0) return 1.0f;
            return (float) width / height;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(type);
            dest.writeInt(width);
            dest.writeInt(height);
            dest.writeString(url);
        }

        protected Clip(Parcel in) {
            type = in.readInt();
            width = in.readInt();
            height = in.readInt();
            url = in.readString();
        }

        public static final Creator<Clip> CREATOR = new Creator<Clip>() {
            @Override
            public Clip createFromParcel(Parcel in) {
                return new Clip(in);
            }

            @Override
            public Clip[] newArray(int size) {
                return new Clip[size];
            }
        };
    }

    public static class Music implements Parcelable, Serializable {
        @SerializedName("volume")
        public int volume;

        @SerializedName("seek_time")
        public int seekTime; // 起始播放位置 (ms)

        @SerializedName("url")
        public String url;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(volume);
            dest.writeInt(seekTime);
            dest.writeString(url);
        }

        protected Music(Parcel in) {
            volume = in.readInt();
            seekTime = in.readInt();
            url = in.readString();
        }

        public static final Creator<Music> CREATOR = new Creator<Music>() {
            @Override
            public Music createFromParcel(Parcel in) {
                return new Music(in);
            }

            @Override
            public Music[] newArray(int size) {
                return new Music[size];
            }
        };
    }
}