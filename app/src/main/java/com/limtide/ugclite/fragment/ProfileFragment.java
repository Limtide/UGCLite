package com.limtide.ugclite.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.limtide.ugclite.databinding.FragmentProfileBinding;
import com.limtide.ugclite.data.repository.UserRepository;
import androidx.lifecycle.Observer;
import com.limtide.ugclite.database.entity.User;


public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private static final String TAG = "ProfileFragment";

    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //View view = inflater.inflate(R.layout.fragment_profile, container, false);
        binding =FragmentProfileBinding.inflate(inflater,container,false);

        // 初始化UserRepository
        userRepository = new UserRepository(requireActivity().getApplication());

        // 加载当前用户信息
        loadCurrentUser();

        setupClickListeners(binding.getRoot());

        return binding.getRoot();
    }



    /**
     * 加载当前登录用户信息
     */
    private void loadCurrentUser() {
        // 从SharedPreferences获取当前用户名
        String currentUsername = requireActivity()
                .getSharedPreferences("user_prefs", 0)
                .getString("current_username", null);

        if (currentUsername != null && !currentUsername.trim().isEmpty()) {
            Log.d(TAG, "Loading user info for: " + currentUsername);

            // 从数据库获取用户信息
            userRepository.getUserByUsername(currentUsername).observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    if (user != null) {
                        // 更新UI显示用户信息
                        updateUserInfo(user);
                    } else {
                        Log.d(TAG, "User not found in database: " + currentUsername);
                        // 如果数据库中没有找到用户，显示默认信息
                        showDefaultUserInfo();
                    }
                }
            });
        } else {
            Log.d(TAG, "No current user found");
            // 没有当前用户，显示默认信息
            showDefaultUserInfo();
        }
    }

    /**
     * 更新用户信息显示
     */
    private void updateUserInfo(User user) {
        if (binding != null) {
            // 显示用户昵称（如果昵称为空则显示用户名）
            String displayName = user.getDisplayName();
            binding.profileTitle.setText(displayName);

            // 显示用户签名（如果签名为空则显示默认签名）
            String signature = user.getSignature();
            if (signature != null && !signature.trim().isEmpty()) {
                binding.profileSubtitle.setText(signature);
            } else {
                binding.profileSubtitle.setText("默认签名：简洁生活，从现在开始");
            }

            Log.d(TAG, "Updated user info - Name: " + displayName + ", Signature: " + signature);
        }
    }

    /**
     * 显示默认用户信息
     */
    private void showDefaultUserInfo() {
        if (binding != null) {
            binding.profileTitle.setText("未登录用户");
            binding.profileSubtitle.setText("简洁生活，从现在开始");
        }
    }

    private void setupClickListeners(View view) {

        binding.optionAbout.setOnClickListener(v -> {
            Log.d(TAG,  binding.optionAbout+ "被点击");
            Toast.makeText(requireContext(),"optionAbout",Toast.LENGTH_SHORT).show();
        });
        binding.optionHelp.setOnClickListener(v -> {
            Log.d(TAG,  binding.optionHelp+ "被点击");
            Toast.makeText(requireContext(),"optionHelp",Toast.LENGTH_SHORT).show();
        });
        binding.optionLogout.setOnClickListener(v -> {
            Log.d(TAG,  binding.optionLogout+ "被点击");
            // 清除当前登录用户信息
            logoutCurrentUser();
            Toast.makeText(requireContext(),"已退出登录",Toast.LENGTH_SHORT).show();
        });
        binding.optionPrivacy.setOnClickListener(v -> {
            Log.d(TAG,  binding.optionPrivacy+ "被点击");
            Toast.makeText(requireContext(),"optionPrivacy",Toast.LENGTH_SHORT).show();
        });
        binding.optionSettings.setOnClickListener(v -> {
            Log.d(TAG,  binding.optionSettings+ "被点击");
            Toast.makeText(requireContext(),"optionSettings",Toast.LENGTH_SHORT).show();
        });
        binding.profileSubtitle.setOnClickListener(v -> {
            Log.d(TAG,  binding.profileSubtitle+ "被点击");
            Toast.makeText(requireContext(),"profileSubtitle",Toast.LENGTH_SHORT).show();
        });

    }

    /**
     * 退出登录，清除当前用户信息
     */
    private void logoutCurrentUser() {
        // 清除SharedPreferences中的当前用户信息
        requireActivity().getSharedPreferences("user_prefs", 0)
                .edit()
                .remove("current_username")
                .remove("login_time")
                .apply();

        // 更新UI显示默认信息
        showDefaultUserInfo();

        Log.d(TAG, "User logged out successfully");

        // 可以选择跳转到登录页面
        // Intent intent = new Intent(requireContext(), LoginActivity.class);
        // startActivity(intent);
        // requireActivity().finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;

        // 清理UserRepository资源
        if (userRepository != null) {
            userRepository.cleanup();
        }
    }
}