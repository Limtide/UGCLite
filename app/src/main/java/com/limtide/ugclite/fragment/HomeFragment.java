package com.limtide.ugclite.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.limtide.ugclite.R;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;

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
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Log.d(TAG,"HomeFragment is onCreateView");
        initViews(view);
        setupRefreshListener();


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"HomeFragment is onViewCreated");
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);

        // 默认显示空状态页面（因为还没有数据）
        showEmptyState();
    }

    private void setupRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新被触发");
            // 模拟网络请求
            swipeRefreshLayout.postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                // 这里可以添加加载数据的逻辑
            }, 2000);
        });
    }

    private void showEmptyState() {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG,"HomeFragment is onDestroyView");
        //为什么我在转移到“我”界面的时候，没有销毁UI？

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
        //我在退出应用的时候是否应该销毁所有创建的Fragment？还是只要销毁对应attach的Activity就可以自动销毁？
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"HomeFragment is onDetach");
    }

    private void hideEmptyState() {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
}