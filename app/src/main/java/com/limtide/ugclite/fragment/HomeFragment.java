package com.limtide.ugclite.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.limtide.ugclite.activity.PostDetailActivity;
import com.limtide.ugclite.adapter.NoteCardAdapater;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.FragmentHomeBinding;
import com.limtide.ugclite.network.ApiService;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NoteCardAdapater notecardAdapater;
    private ApiService apiService;
    private boolean isFirst = true;
    private boolean isLoading = false; // 是否正在加载数据
    private boolean hasMoreData = true; // 是否还有更多数据
    private int currentCursor = 0; // 当前分页游标
    private static final int PAGE_SIZE = 20; // 每页数据量

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG,"HomeFragment is onAttach");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"HomeFragment is onCreate");
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
        // 设置瀑布流布局 - 双列
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(layoutManager);

        // 初始化适配器
        notecardAdapater = new NoteCardAdapater(getContext());
        binding.recyclerView.setAdapter(notecardAdapater);

        // 添加滚动监听器实现上拉加载更多
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 当滚动停止时检查是否需要加载更多
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isLoading && hasMoreData) {
                    checkLoadMore();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 只有在向下滚动时才检查
                if (dy > 0 && !isLoading && hasMoreData) {
                    checkLoadMore();
                }
            }
        });

        Log.d(TAG, "RecyclerView setup complete, adapter: " + (notecardAdapater != null ? "not null" : "null"));

        // 设置点击事件
        Log.d(TAG, "Setting onItemClickListener on notecardAdapater");
        notecardAdapater.setOnItemClickListener(new NoteCardAdapater.OnItemClickListener() {
            @Override
            public void onItemClick(Post post, int position) {
                Log.d(TAG, "onItemClick triggered! Post: " + post.title + ", position: " + position);

                try {
                    // 跳转到详情页面
                    Intent intent = new Intent(requireActivity(), PostDetailActivity.class);
                    intent.putExtra("post", post); // 传递整个Post对象
                    startActivity(intent);
                    Log.d(TAG, "Successfully started PostDetailActivity");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting PostDetailActivity: " + e.getMessage(), e);
                }
            }
        });

        // 初始化ApiService
        apiService = ApiService.getInstance();

        // 初始加载数据
        if (isFirst) {
            showEmptyState();
            isFirst = false;
        } else {
            loadFeedData();
        }

    }

    private void setupRefreshListener() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新被触发");
            // 刷新数据
            refreshFeedData();
        });
    }

    /**
     * 检查是否需要加载更多数据
     */
    private void checkLoadMore() {
        if (isLoading || !hasMoreData) {
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
     * 加载Feed数据 - 从API获取真实数据
     */
    private void loadFeedData() {
        if (isLoading) {
            Log.d(TAG, "数据正在加载中，跳过重复请求");
            return;
        }

        isLoading = true;
        showLoadingState();

        Log.d(TAG, "开始加载Feed数据，数量: " + PAGE_SIZE + ", 支持视频: false");

        // 调用API获取数据（第一页，cursor=0）
        apiService.getFeedData(PAGE_SIZE, false, currentCursor, new ApiService.FeedCallback() {
            @Override
            public void onSuccess(List<Post> posts, boolean hasMore) {
                Log.d(TAG, "API调用成功，获取到 " + (posts != null ? posts.size() : 0) + " 条数据");

                // 切换到主线程更新UI
                if (getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        isLoading = false;
                        hasMoreData = hasMore;

                        if (notecardAdapater != null) {
                            if (posts != null && !posts.isEmpty()) {
                                // 过滤掉只有音频类型（如MP3）的帖子，只保留图片和视频
                                List<Post> filteredPosts = filterPosts(posts);
                                notecardAdapater.setPosts(filteredPosts);
                                hideEmptyState();
                                Log.d(TAG, "过滤后数据已加载到瀑布流适配器，原始数据: " + posts.size() + "，过滤后: " + filteredPosts.size());

                                // 更新cursor为下一页的起始位置
                                currentCursor += filteredPosts.size();
                            } else {
                                showEmptyState();
                                Log.d(TAG, "没有数据，显示空状态");
                            }
                        }

                        // 隐藏加载状态
                        hideLoadingState();
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "API调用失败: " + errorMessage);

                // 切换到主线程更新UI
                if (getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        isLoading = false;

                        // 显示错误信息
                        showErrorState(errorMessage);

                        // 隐藏加载状态
                        hideLoadingState();
                    });
                }
            }
        });
    }

    /**
     * 刷新Feed数据
     */
    private void refreshFeedData() {
        hasMoreData = true; // 重置为有更多数据
        currentCursor = 0; // 重置游标到第一页
        loadFeedData();
    }

    /**
     * 加载更多Feed数据
     */
    private void loadMoreFeedData() {
        if (isLoading || !hasMoreData) {
            Log.d(TAG, "正在加载或没有更多数据，跳过加载更多");
            return;
        }

        isLoading = true;
        Log.d(TAG, "开始加载更多Feed数据，cursor: " + currentCursor + ", 数量: " + PAGE_SIZE);

        // 调用API获取更多数据
        apiService.getFeedData(PAGE_SIZE, false, currentCursor, new ApiService.FeedCallback() {
            @Override
            public void onSuccess(List<Post> posts, boolean hasMore) {
                Log.d(TAG, "加载更多API调用成功，获取到 " + (posts != null ? posts.size() : 0) + " 条数据");

                // 切换到主线程更新UI
                if (getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        isLoading = false;
                        hasMoreData = hasMore;

                        if (notecardAdapater != null) {
                            if (posts != null && !posts.isEmpty()) {
                                // 过滤掉只有音频类型（如MP3）的帖子，只保留图片和视频
                                List<Post> filteredPosts = filterPosts(posts);
                                notecardAdapater.addPosts(filteredPosts);
                                Log.d(TAG, "加载更多过滤后数据已添加，原始数据: " + posts.size() + "，过滤后: " + filteredPosts.size());

                                // 更新cursor为下一页的起始位置
                                currentCursor += filteredPosts.size();
                            } else {
                                Log.d(TAG, "加载更多没有新数据");
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "加载更多API调用失败: " + errorMessage);

                // 切换到主线程更新UI
                if (getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        isLoading = false;
                        Log.e(TAG, "加载更多失败: " + errorMessage);
                    });
                }
            }
        });
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG,"HomeFragment is onDestroyView");
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

}