package com.limtide.ugclite.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.limtide.ugclite.R;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private TextView profileTitle;
    private TextView profileSubtitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupClickListeners(view);

        return view;
    }

    private void initViews(View view) {
        profileTitle = view.findViewById(R.id.profile_title);
        profileSubtitle = view.findViewById(R.id.profile_subtitle);
    }

    private void setupClickListeners(View view) {
        // 设置项点击事件
        int[] optionIds = {
            R.id.option_settings,
            R.id.option_privacy,
            R.id.option_help,
            R.id.option_about,
            R.id.option_logout
        };

        String[] optionNames = {
            "设置", "隐私政策", "帮助中心", "关于我们", "退出登录"
        };

        for (int i = 0; i < optionIds.length; i++) {
            final int index = i;
            view.findViewById(optionIds[i]).setOnClickListener(v -> {
                Log.d(TAG, optionNames[index] + "被点击");
                // 这里可以添加具体的跳转逻辑
            });
        }
    }
}