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
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.limtide.ugclite.activity.PostDetailActivity;
import com.limtide.ugclite.adapter.NoteCardAdapater;
import com.limtide.ugclite.data.model.Post;
import com.limtide.ugclite.databinding.FragmentHomeBinding;
import com.limtide.ugclite.network.ApiService;

import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NoteCardAdapater notecardAdapater;
    private ApiService apiService;
    private boolean isLoading = false; // 是否正在加载数据
    private boolean hasMoreData = true; // 是否还有更多数据
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

        Log.d(TAG, "RecyclerView setup complete, adapter: " + (notecardAdapater != null ? "not null" : "null"));

        // 设置点击事件
        Log.d(TAG, "Setting onItemClickListener on notecardAdapater");
        notecardAdapater.setOnItemClickListener(new NoteCardAdapater.OnItemClickListener() {
            @Override
            public void onItemClick(Post post, int position) {
                Log.d(TAG, "onItemClick triggered! Post: " + post.title + ", position: " + position);

                try {
                    // 跳转到详情页面
                    Intent intent = new Intent(getContext(), PostDetailActivity.class);
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
        loadFeedData();
    }

    private void setupRefreshListener() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新被触发");
            // 刷新数据
            refreshFeedData();
        });
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

        // 调用API获取数据
        apiService.getFeedData(PAGE_SIZE, false, new ApiService.FeedCallback() {
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
                                notecardAdapater.setPosts(posts);
                                hideEmptyState();
                                Log.d(TAG, "数据已加载到瀑布流适配器");
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
        loadFeedData();
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
                binding.emptyTitle.setText("加载失败");
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