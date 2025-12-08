package com.limtide.ugclite.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.limtide.ugclite.ui.activity.PostDetailActivity;
import com.limtide.ugclite.ui.adapter.NoteCardAdapter;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.FragmentHomeBinding;
import com.limtide.ugclite.network.ApiService;
import com.limtide.ugclite.utils.VideoThumbnailUtil;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // 状态保存的Key常量
    private static final String KEY_IS_FIRST = "is_first";
    private static final String KEY_IS_LOADING = "is_loading";
    private static final String KEY_HAS_MORE_DATA = "has_more_data";
    private static final String KEY_CURRENT_CURSOR = "current_cursor";
    private static final String KEY_RECYCLER_STATE = "recycler_state";
    private static final String KEY_POSTS_DATA = "posts_data";

    private FragmentHomeBinding binding;
    private NoteCardAdapter notecardAdapter;
    private ApiService apiService;
    private boolean isFirst = true;
    private static final int PAGE_SIZE = 20; // 每页数据量

    private static final boolean ACCEPT_VIDEO = true;

    // 线程安全的状态管理
    private final ReentrantLock stateLock = new ReentrantLock();
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private final AtomicBoolean hasMoreData = new AtomicBoolean(true);
    private final AtomicInteger currentCursor = new AtomicInteger(0);

    // 保存滚动状态
    private int savedFirstVisiblePosition = 0;
    private Parcelable savedRecyclerViewState;

    // 线程安全的数据存储
    private volatile List<Post> savedPosts = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG,"HomeFragment is onAttach");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"HomeFragment is onCreate");

        // 恢复保存的状态 - 线程安全
        if (savedInstanceState != null) {
            stateLock.lock();
            try {
                isFirst = savedInstanceState.getBoolean(KEY_IS_FIRST, true);
                isLoading.set(savedInstanceState.getBoolean(KEY_IS_LOADING, false));
                hasMoreData.set(savedInstanceState.getBoolean(KEY_HAS_MORE_DATA, true));
                currentCursor.set(savedInstanceState.getInt(KEY_CURRENT_CURSOR, 0));
                savedRecyclerViewState = savedInstanceState.getParcelable(KEY_RECYCLER_STATE);

                // 恢复数据列表
                savedPosts.clear();
                ArrayList<Post> posts = (ArrayList<Post>) savedInstanceState.getSerializable(KEY_POSTS_DATA);
                if (posts != null) {
                    savedPosts.addAll(posts);
                    Log.d(TAG, "恢复保存的数据，数量: " + savedPosts.size());
                }

                Log.d(TAG, "状态恢复完成 - isFirst: " + isFirst + ", cursor: " + currentCursor.get() + ", posts: " + savedPosts.size());
            } finally {
                stateLock.unlock();
            }
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        Log.d(TAG,"HomeFragment is onCreateView");
        initViews();
        setupRefreshListener();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"HomeFragment is onViewCreated");
    }

    private void initViews() {
        // 注意：LayoutManager已在XML中配置

        // 初始化适配器
        notecardAdapter = new NoteCardAdapter(getContext());
        binding.recyclerView.setAdapter(notecardAdapter);

        // 恢复滚动状态
        if (savedRecyclerViewState != null) {
            binding.recyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerViewState);
            Log.d(TAG, "恢复RecyclerView滚动状态");
        }

        // 添加滚动监听器实现上拉加载更多 - 使用WeakReference避免内存泄漏
        binding.recyclerView.addOnScrollListener(new SafeScrollListener(this));

        Log.d(TAG, "RecyclerView setup complete, adapter: " + (notecardAdapter != null ? "not null" : "null"));

        // 设置点击事件 - 使用SafeItemClickListener避免内存泄漏
        Log.d(TAG, "Setting onItemClickListener on notecardAdapter");
        notecardAdapter.setOnItemClickListener(new SafeItemClickListener(this));

        // 初始化ApiService
        apiService = ApiService.getInstance();

        // 根据保存的状态决定是否重新加载数据
        if (!savedPosts.isEmpty()) {
            // 有保存的数据，直接恢复显示
            Log.d(TAG, "恢复保存的数据，数量: " + savedPosts.size());
            notecardAdapter.setPosts(savedPosts);
            hideEmptyState();

            // 恢复滚动状态
            if (savedRecyclerViewState != null) {
                binding.recyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerViewState);
                Log.d(TAG, "恢复RecyclerView滚动状态");
            }

            // 刷新所有可见item的点赞状态（与PostDetailActivity同步）
            refreshVisibleLikeStatus();
        } else {
            // 没有保存的数据，重新加载
            if (isFirst) {
                Log.d(TAG, "首次进入，开始加载数据");
                isFirst = false;
            }
            loadFeedData();
        }

    }

    private void setupRefreshListener() {
        binding.swipeRefreshLayout.setOnRefreshListener(new SafeRefreshListener(this));
    }

    /**
     * 检查是否需要加载更多数据 - 线程安全
     */
    private void checkLoadMore() {
        if (isLoading.get() || !hasMoreData.get()) {
            return;
        }

        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) binding.recyclerView.getLayoutManager();
        if (layoutManager != null) {
            int[] positions = layoutManager.findLastVisibleItemPositions(null);
            int lastVisiblePosition = 0;
            for (int pos : positions) {
                if (pos > lastVisiblePosition) {
                    lastVisiblePosition = pos;
                }
            }

            // 当滑动到最后3个item时开始加载更多
            int totalItemCount = layoutManager.getItemCount();
            if (totalItemCount > 0 && lastVisiblePosition >= totalItemCount - 3) {
                Log.d(TAG, "接近底部，开始加载更多数据。当前总数: " + totalItemCount + ", 最后可见位置: " + lastVisiblePosition);
                loadMoreFeedData();
            }
        }
    }

    /**
     * 加载Feed数据 - 从API获取真实数据 - 线程安全
     */
    private void loadFeedData() {
        stateLock.lock();
        try {
            if (isLoading.get()) {
                Log.d(TAG, "数据正在加载中，跳过重复请求");
                return;
            }

            // 原子性地设置加载状态
            isLoading.set(true);
            showLoadingState();

            Log.d(TAG, "开始加载Feed数据，数量: " + PAGE_SIZE + ", 支持视频: true, cursor: " + currentCursor.get());

            // 调用API获取数据（第一页，cursor=0）- 使用SafeFeedCallback避免内存泄漏
            apiService.getFeedData(PAGE_SIZE, ACCEPT_VIDEO, currentCursor.get(), new SafeFeedCallback(this, false));
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 刷新Feed数据 - 线程安全
     */
    private void refreshFeedData() {
        stateLock.lock();
        try {
            // 原子性地重置状态
            hasMoreData.set(true); // 重置为有更多数据
            currentCursor.set(0); // 重置游标到第一页
            Log.d(TAG, "重置状态进行刷新，cursor: " + currentCursor.get());
        } finally {
            stateLock.unlock();
        }

        loadFeedData();
    }

    /**
     * 加载更多Feed数据 - 线程安全
     */
    private void loadMoreFeedData() {
        stateLock.lock();
        try {
            if (isLoading.get() || !hasMoreData.get()) {
                Log.d(TAG, "正在加载或没有更多数据，跳过加载更多。isLoading: " + isLoading.get() + ", hasMoreData: " + hasMoreData.get());
                return;
            }

            // 原子性地设置加载状态
            isLoading.set(true);
            Log.d(TAG, "开始加载更多Feed数据，cursor: " + currentCursor.get() + ", 数量: " + PAGE_SIZE);

            // 调用API获取更多数据 - 使用SafeFeedCallback避免内存泄漏
            apiService.getFeedData(PAGE_SIZE, ACCEPT_VIDEO, currentCursor.get(), new SafeFeedCallback(this, true));
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 过滤帖子，只显示图片和视频类型，过滤掉纯音频（如MP3）
     */
    private List<Post> filterPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return posts;
        }

        List<Post> filteredPosts = new ArrayList<>();

        for (Post post : posts) {
            if (shouldShowPost(post)) {
                filteredPosts.add(post);
            }
        }

        return filteredPosts;
    }

    /**
     * 判断是否应该显示该帖子
     */
    private boolean shouldShowPost(Post post) {
        // 如果帖子没有clips，不显示
        if (post.clips == null || post.clips.isEmpty()) {
            Log.d(TAG, "过滤掉无clips的帖子: " + post.title);
            return false;
        }

        // 检查是否有图片或视频类型的clip
        boolean hasImageOrVideo = false;
        for (Post.Clip clip : post.clips) {
            // type 0: 图片, type 1: 视频
            if (clip.type == 0 || clip.type == 1) {
                hasImageOrVideo = true;
                break;
            }
        }

        if (!hasImageOrVideo) {
            Log.d(TAG, "过滤掉纯音频帖子的clips，帖子标题: " + post.title);
            return false;
        }

        return true;
    }

    /**
     * 显示加载状态
     */
    private void showLoadingState() {
        if (binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    }

    /**
     * 隐藏加载状态
     */
    private void hideLoadingState() {
        if (binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * 显示错误状态
     */
    private void showErrorState(String errorMessage) {
        Log.e(TAG, "显示错误状态: " + errorMessage);

        if (binding.emptyStateLayout != null) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);

            // 可以在这里更新错误提示文本
            if (binding.emptyTitle != null) {
                binding.emptyTitle.setText("请检查网络连接");
            }
            if (binding.emptyDescription != null) {
                binding.emptyDescription.setText(errorMessage);
            }
        }
    }

    private void showEmptyState() {
        if (binding.emptyStateLayout != null) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyState() {
        if (binding.emptyStateLayout != null) {
            binding.emptyStateLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 预加载视频缩略图以提升性能
     */
    private void preloadVideoThumbnails(List<Post> posts) {
        if (posts == null || posts.isEmpty() || getContext() == null) {
            return;
        }

        Log.d(TAG, "开始预加载视频缩略图，帖子数量: " + posts.size());

        // 在后台线程中预加载，避免阻塞UI
        new Thread(() -> {
            int preloadCount = 0;
            int maxPreload = Math.min(posts.size(), 6); // 限制预加载数量，避免过多网络请求

            for (int i = 0; i < maxPreload; i++) {
                Post post = posts.get(i);
                if (post.clips != null && !post.clips.isEmpty()) {
                    for (Post.Clip clip : post.clips) {
                        if (clip.type == 1 && clip.url != null && !clip.url.isEmpty()) {
                            // 找到视频clip
                            try {
                                // 检查是否已有缓存
                                if (VideoThumbnailUtil.getCachedThumbnail(getContext(), clip.url) == null) {
                                    // 没有缓存才预加载
                                    VideoThumbnailUtil.preloadThumbnail(getContext(), clip.url, null);
                                    preloadCount++;
                                    Log.d(TAG, "预加载视频缩略图: " + clip.url);
                                } else {
                                    Log.d(TAG, "视频缩略图已存在缓存，跳过: " + clip.url);
                                }
                                break; // 每个帖子只预加载第一个视频
                            } catch (Exception e) {
                                Log.w(TAG, "预加载视频缩略图失败: " + clip.url, e);
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "视频缩略图预加载完成，预加载数量: " + preloadCount);
        }).start();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG,"HomeFragment is onDestroyView");

        // 保存RecyclerView的滚动状态
        if (binding.recyclerView != null) {
            savedRecyclerViewState = binding.recyclerView.getLayoutManager().onSaveInstanceState();
            Log.d(TAG, "保存RecyclerView滚动状态");
        }

        // 清理ViewBinding以防止内存泄漏
        binding = null;


        // 取消网络请求
        if (apiService != null) {
            apiService.cancelAllRequests();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG,"HomeFragment is onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"HomeFragment is onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"HomeFragment is onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"HomeFragment is onDetach");
    }

    /**
     * 保存Fragment状态
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "保存Fragment状态");

        // 线程安全地保存状态变量
        stateLock.lock();
        try {
            outState.putBoolean(KEY_IS_FIRST, isFirst);
            outState.putBoolean(KEY_IS_LOADING, isLoading.get());
            outState.putBoolean(KEY_HAS_MORE_DATA, hasMoreData.get());
            outState.putInt(KEY_CURRENT_CURSOR, currentCursor.get());
        } finally {
            stateLock.unlock();
        }

        // 保存RecyclerView状态
        if (binding.recyclerView != null && binding.recyclerView.getLayoutManager() != null) {
            savedRecyclerViewState = binding.recyclerView.getLayoutManager().onSaveInstanceState();
            outState.putParcelable(KEY_RECYCLER_STATE, savedRecyclerViewState);
            Log.d(TAG, "保存RecyclerView状态");
        }

        // 保存数据列表
        if (notecardAdapter != null) {
            List<Post> currentPosts = new ArrayList<>();
            for (int i = 0; i < notecardAdapter.getItemCount(); i++) {
                currentPosts.add(notecardAdapter.getPost(i));
            }
            outState.putSerializable(KEY_POSTS_DATA, new ArrayList<>(currentPosts));
            Log.d(TAG, "保存数据列表，数量: " + currentPosts.size());
        }

        Log.d(TAG, "状态保存完成 - isFirst: " + isFirst + ", cursor: " + currentCursor.get());
    }

    /**
     * 刷新所有可见item的点赞状态（与PostDetailActivity同步）
     */
    private void refreshVisibleLikeStatus() {
        if (notecardAdapter == null || binding.recyclerView == null) {
            return;
        }

        // 通知适配器刷新所有可见item
        StaggeredGridLayoutManager layoutManager =
            (StaggeredGridLayoutManager) binding.recyclerView.getLayoutManager();

        if (layoutManager != null) {
            int[] firstVisiblePositions = layoutManager.findFirstVisibleItemPositions(null);
            int[] lastVisiblePositions = layoutManager.findLastVisibleItemPositions(null);

            if (firstVisiblePositions != null && lastVisiblePositions != null) {
                for (int i = 0; i < firstVisiblePositions.length; i++) {
                    int firstPos = firstVisiblePositions[i];
                    int lastPos = lastVisiblePositions[i];

                    for (int pos = firstPos; pos <= lastPos; pos++) {
                        if (pos < notecardAdapter.getItemCount()) {
                            notecardAdapter.notifyItemChanged(pos);
                        }
                    }
                }
            }
        }

        Log.d(TAG, "已刷新所有可见item的点赞状态");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "HomeFragment is onResume");

        // 从PostDetailActivity返回时，刷新点赞状态
        if (notecardAdapter != null && notecardAdapter.getItemCount() > 0) {
            refreshVisibleLikeStatus();
        }
    }

    /**
     * 安全的RecyclerView滚动监听器 - 使用WeakReference避免内存泄漏
     */
    private static class SafeScrollListener extends RecyclerView.OnScrollListener {
        private final WeakReference<HomeFragment> fragmentRef;

        SafeScrollListener(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached()) {
                return;
            }

            // 当滚动停止时检查是否需要加载更多
            if (newState == RecyclerView.SCROLL_STATE_IDLE && !fragment.isLoading.get() && fragment.hasMoreData.get()) {
                fragment.checkLoadMore();
            }
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached()) {
                return;
            }

            // 只有在向下滚动时才检查
            if (dy > 0 && !fragment.isLoading.get() && fragment.hasMoreData.get()) {
                fragment.checkLoadMore();
            }
        }
    }

    /**
     * 安全的Feed回调 - 使用WeakReference避免内存泄漏
     */
    private static class SafeFeedCallback implements ApiService.FeedCallback {
        private final WeakReference<HomeFragment> fragmentRef;
        private final boolean isLoadMore;

        SafeFeedCallback(HomeFragment fragment, boolean isLoadMore) {
            this.fragmentRef = new WeakReference<>(fragment);
            this.isLoadMore = isLoadMore;
        }

        @Override
        public void onSuccess(List<Post> posts, boolean hasMore) {
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached() || fragment.getContext() == null) {
                return;
            }

            Log.d(fragment.TAG, "API调用成功，获取到 " + (posts != null ? posts.size() : 0) + " 条数据");

            // 切换到主线程更新UI - 线程安全的状态更新
            fragment.requireActivity().runOnUiThread(() -> {
                try {
                    // 原子性地更新所有状态
                    fragment.updateLoadingStateAtomic(false, hasMore);

                    if (fragment.notecardAdapter != null) {
                        if (posts != null && !posts.isEmpty()) {
                            // 过滤掉只有音频类型（如MP3）的帖子，只保留图片和视频
                            List<Post> filteredPosts = fragment.filterPosts(posts);

                            if (isLoadMore) {
                                fragment.notecardAdapter.addPosts(filteredPosts);
                                Log.d(fragment.TAG, "加载更多过滤后数据已添加，原始数据: " + posts.size() + "，过滤后: " + filteredPosts.size());
                            } else {
                                fragment.notecardAdapter.setPosts(filteredPosts);
                                fragment.hideEmptyState();
                                Log.d(fragment.TAG, "过滤后数据已加载到瀑布流适配器，原始数据: " + posts.size() + "，过滤后: " + filteredPosts.size());
                            }

                            // 预加载视频缩略图以提升性能
                            fragment.preloadVideoThumbnails(filteredPosts);

                            // 原子性地更新cursor
                            fragment.updateCursorAtomic(filteredPosts.size());
                        } else {
                            if (!isLoadMore) {
                                fragment.showEmptyState();
                                Log.d(fragment.TAG, "没有数据，显示空状态");
                            } else {
                                Log.d(fragment.TAG, "加载更多没有新数据");
                            }
                        }
                    }

                    // 隐藏加载状态
                    fragment.hideLoadingState();
                } catch (Exception e) {
                    Log.e(fragment.TAG, "更新UI时发生异常", e);
                    // 发生异常时重置加载状态
                    fragment.isLoading.set(false);
                }
            });
        }

        @Override
        public void onError(String errorMessage) {
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached() || fragment.getContext() == null) {
                return;
            }

            Log.e(fragment.TAG, "API调用失败: " + errorMessage);

            // 切换到主线程更新UI - 线程安全的状态更新
            fragment.requireActivity().runOnUiThread(() -> {
                try {
                    // 原子性地重置加载状态
                    fragment.isLoading.set(false);

                    if (!isLoadMore) {
                        // 显示错误信息
                        fragment.showErrorState(errorMessage);
                    } else {
                        Log.e(fragment.TAG, "加载更多失败: " + errorMessage);
                    }

                    // 隐藏加载状态
                    fragment.hideLoadingState();
                } catch (Exception e) {
                    Log.e(fragment.TAG, "处理错误回调时发生异常", e);
                }
            });
        }
    }

    /**
     * 安全的点击监听器 - 使用WeakReference避免内存泄漏
     */
    private static class SafeItemClickListener implements NoteCardAdapter.OnItemClickListener {
        private final WeakReference<HomeFragment> fragmentRef;

        SafeItemClickListener(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void onItemClick(Post post, int position) {
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached() || fragment.getContext() == null) {
                return;
            }

            Log.d(fragment.TAG, "onItemClick triggered! Post: " + post.title + ", position: " + position);

            try {
                // 跳转到详情页面
                Intent intent = new Intent(fragment.requireActivity(), PostDetailActivity.class);
                intent.putExtra("post", (Serializable) post); // 明确转换为Serializable

                // 获取点击的卡片视图
                RecyclerView.ViewHolder viewHolder = fragment.binding.recyclerView.findViewHolderForAdapterPosition(position);
                View coverImage = null;
                if (viewHolder != null) {
                    // 获取封面图片视图
                    NoteCardAdapter.ViewHolder noteViewHolder = (NoteCardAdapter.ViewHolder) viewHolder;
                    coverImage = noteViewHolder.getBinding().coverImage;
                }

                // 保存当前滚动位置
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) fragment.binding.recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int[] positions = layoutManager.findFirstVisibleItemPositions(null);
                    if (positions != null && positions.length > 0) {
                        fragment.savedFirstVisiblePosition = positions[0];
                        Log.d(fragment.TAG, "保存当前位置: " + fragment.savedFirstVisiblePosition);
                    }
                }

                // 创建共享元素转场动画
                if (coverImage != null) {
                    android.app.ActivityOptions options = android.app.ActivityOptions
                            .makeSceneTransitionAnimation(fragment.requireActivity(),
                                android.util.Pair.create(coverImage, "cover_image_transition"));
                    fragment.startActivity(intent, options.toBundle());
                } else {
                    // 如果找不到封面图片，使用普通跳转
                    fragment.startActivity(intent);
                }

                Log.d(fragment.TAG, "Successfully started PostDetailActivity with transition");
            } catch (Exception e) {
                Log.e(fragment.TAG, "Error starting PostDetailActivity: " + e.getMessage(), e);
                // 发生错误时使用普通跳转
                try {
                    Intent intent = new Intent(fragment.requireActivity(), PostDetailActivity.class);
                    intent.putExtra("post", (Serializable) post);
                    fragment.startActivity(intent);
                } catch (Exception fallbackError) {
                    Log.e(fragment.TAG, "Fallback navigation failed: " + fallbackError.getMessage(), fallbackError);
                }
            }
        }
    }

    /**
     * 安全的刷新监听器 - 使用WeakReference避免内存泄漏
     */
    private static class SafeRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        private final WeakReference<HomeFragment> fragmentRef;

        SafeRefreshListener(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void onRefresh() {
            HomeFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.isDetached()) {
                return;
            }

            Log.d(fragment.TAG, "下拉刷新被触发");
            // 刷新数据
            fragment.refreshFeedData();
        }
    }

    /**
     * 原子性地更新加载状态 - 线程安全
     */
    private void updateLoadingStateAtomic(boolean loading, boolean hasMore) {
        stateLock.lock();
        try {
            isLoading.set(loading);
            hasMoreData.set(hasMore);
            Log.d(TAG, "原子性更新状态 - isLoading: " + loading + ", hasMoreData: " + hasMore + ", cursor: " + currentCursor.get());
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 原子性地更新cursor - 线程安全
     */
    private void updateCursorAtomic(int delta) {
        stateLock.lock();
        try {
            int newCursor = currentCursor.addAndGet(delta);
            Log.d(TAG, "原子性更新cursor - delta: " + delta + ", newCursor: " + newCursor);
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 获取当前状态快照 - 线程安全
     */
    private StateSnapshot getStateSnapshot() {
        stateLock.lock();
        try {
            return new StateSnapshot(
                isLoading.get(),
                hasMoreData.get(),
                currentCursor.get(),
                new ArrayList<>(savedPosts)
            );
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 状态快照类 - 用于线程安全地传递状态信息
     */
    private static class StateSnapshot {
        final boolean isLoading;
        final boolean hasMoreData;
        final int cursor;
        final List<Post> posts;

        StateSnapshot(boolean isLoading, boolean hasMoreData, int cursor, List<Post> posts) {
            this.isLoading = isLoading;
            this.hasMoreData = hasMoreData;
            this.cursor = cursor;
            this.posts = posts;
        }
    }

}