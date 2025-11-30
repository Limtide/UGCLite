package com.limtide.ugclite.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Feed API 响应数据模型
 * 对应 API 返回的 JSON 结构
 */
public class FeedResponse {

    @SerializedName("status_code")
    public int statusCode; // 状态码（0：成功，其它：失败）

    @SerializedName("has_more")
    public int hasMore; // 是否还有更多作品（取值：0或1）

    @SerializedName("post_list")
    public List<Post> postList; // 作品列表

    /**
     * 判断API请求是否成功
     */
    public boolean isSuccess() {
        return statusCode == 0;
    }

    /**
     * 判断是否还有更多数据
     */
    public boolean hasMoreData() {
        return hasMore == 1;
    }

    @Override
    public String toString() {
        return "FeedResponse{" +
                "statusCode=" + statusCode +
                ", hasMore=" + hasMore +
                ", postList.size()=" + (postList != null ? postList.size() : 0) +
                '}';
    }
}