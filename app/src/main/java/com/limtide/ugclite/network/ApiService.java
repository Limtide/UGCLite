package com.limtide.ugclite.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.limtide.ugclite.data.model.FeedResponse;
import com.limtide.ugclite.data.model.Post;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API服务类 - 处理网络请求
 * 使用OkHttp调用ByteDance College Training Camp API
 */
public class ApiService {

    private static final String TAG = "ApiService";
    private static final String BASE_URL = "https://college-training-camp.bytedance.com/feed/";
    private static final int CONNECT_TIMEOUT = 15; // 连接超时15秒
    private static final int READ_TIMEOUT = 30;    // 读取超时30秒

    private OkHttpClient okHttpClient;
    private Gson gson;
    private static volatile ApiService instance;


    /**
     * 获取ApiService实例 - 双重检查锁定单例模式
     * 确保多线程环境下的线程安全
     */
    public static ApiService getInstance() {
        if (instance == null) {
            synchronized (ApiService.class) {
                if (instance == null) {
                    instance = new ApiService();
                }
            }
        }
        return instance;
    }

    /**
     * 私有构造函数
     */
    private ApiService() {
        // 初始化OkHttpClient
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)//连接超时时间
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)//读取超时时间
                .retryOnConnectionFailure(true)//失败重试
                .build();

        // 初始化Gson
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }


    /**
     * 获取Feed数据 - GET请求方式
     * @param count 请求作品数量
     * @param acceptVideoClip 是否支持视频片段
     * @param callback 回调接口
     */
    public void getFeedData(int count, boolean acceptVideoClip, FeedCallback callback) {
        getFeedData(count, acceptVideoClip, 0, callback);
    }

    /**
     * 获取Feed数据 - GET请求方式（支持分页）
     * @param count 请求作品数量
     * @param acceptVideoClip 是否支持视频片段
     * @param cursor 分页游标（0表示第一页）
     * @param callback 回调接口
     */
    public void getFeedData(int count, boolean acceptVideoClip, int cursor, FeedCallback callback) {
        Log.d(TAG, "开始获取Feed数据 - GET方式，count: " + count + ", acceptVideoClip: " + acceptVideoClip + ", cursor: " + cursor);
        Log.d(TAG, "开始获取Feed数据 - GET方式，count: " + count + ", acceptVideoClip: " + acceptVideoClip);

        // 构建URL和Query参数
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("count", String.valueOf(count))
                .addQueryParameter("accept_video_clip", acceptVideoClip ? "true" : "false");

        // 添加分页参数（只有非第一页时才添加cursor）
        if (cursor > 0) {
            urlBuilder.addQueryParameter("cursor", String.valueOf(cursor));
        }

        HttpUrl url = urlBuilder.build();

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "UGCLite-Android/1.0")
                .build();

        // 异步发送请求 (enqueue)
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络层面的失败（如无网、DNS解析失败、超时）
                Log.e(TAG, "网络请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response, callback);
            }
        });
    }



    /**
     * 获取Feed数据 - POST请求方式（支持分页）
     * @param count 请求作品数量
     * @param acceptVideoClip 是否支持视频片段
     * @param cursor 分页游标（0表示第一页）
     * @param callback 回调接口
     */
    public void getFeedDataPost(int count, boolean acceptVideoClip, int cursor, FeedCallback callback) {
        Log.d(TAG, "开始获取Feed数据 - POST方式，count: " + count + ", acceptVideoClip: " + acceptVideoClip + ", cursor: " + cursor);

        // 构建请求体
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("count", String.valueOf(count))
                .add("accept_video_clip", acceptVideoClip ? "true" : "false");

        // 添加分页参数（只有非第一页时才添加cursor）
        if (cursor > 0) {
            formBuilder.add("cursor", String.valueOf(cursor));
        }

        RequestBody formBody = formBuilder.build();

        // 构建请求
        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("User-Agent", "UGCLite-Android/1.0")
                .build();

        // 发送请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "网络请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response, callback);
            }
        });
    }

    /**
     * 处理网络响应
     */
    private void handleResponse(Response response, FeedCallback callback) {
        try {
            //检查 HTTP 状态码 (是否是 200-299)
            if (!response.isSuccessful()) {
                //处理 404/500 等错误
                String errorMsg = "请求失败，状态码: " + response.code();
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
                return;
            }
            //获取响应体字符串
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "API响应内容: " + responseBody);

            //Gson 解析 JSON -> Java 对象
            FeedResponse feedResponse = gson.fromJson(responseBody, FeedResponse.class);
            //业务逻辑判断
            if (feedResponse != null) {
                if (feedResponse.isSuccess()) {
                    List<Post> posts = feedResponse.postList;
                    boolean hasMore = feedResponse.hasMoreData();
                    Log.d(TAG, "数据解析成功，获取到 " + (posts != null ? posts.size() : 0) + " 条数据，hasMore: " + hasMore);

                    if (callback != null) {
                        callback.onSuccess(posts, hasMore);
                    }
                } else {
                    String errorMsg = "API返回错误，状态码: " + feedResponse.statusCode;
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            } else {
                String errorMsg = "响应数据解析失败";
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "处理响应时发生异常", e);
            if (callback != null) {
                callback.onError("数据处理异常: " + e.getMessage());
            }
        } finally {
            //关闭流，防止内存泄漏
            if (response.body() != null) {
                response.body().close();
            }
        }
    }

    /**
     * 取消所有网络请求
     */
    public void cancelAllRequests() {
        Log.d(TAG, "取消所有网络请求");
        if (okHttpClient != null) {
            okHttpClient.dispatcher().cancelAll();
        }
    }



    /**
     * Feed数据回调接口
     * 异步调用，渲染UI与获取数据同步进行
     */
    public interface FeedCallback {
        /**
         * 数据获取成功
         * @param posts 作品列表
         * @param hasMore 是否还有更多数据
         */
        void onSuccess(List<Post> posts, boolean hasMore);

        /**
         * 数据获取失败
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);
    }
}