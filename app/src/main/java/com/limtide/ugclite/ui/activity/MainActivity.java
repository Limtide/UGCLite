package com.limtide.ugclite.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.limtide.ugclite.R;
import com.limtide.ugclite.databinding.ActivityMainBinding;
import com.limtide.ugclite.ui.fragment.HomeFragment;
import com.limtide.ugclite.ui.fragment.ProfileFragment;
import com.limtide.ugclite.utils.PreferenceManager;
import com.limtide.ugclite.utils.CacheManager;
import com.limtide.ugclite.UGCApplication;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;

    //声明成员变量 声明底部导航栏的Fragment来方便切换
    //实际开发中应该声明成员变量，还是为了减少内存，声明成局部变量？
    private FragmentManager fragmentManager;
    private HomeFragment homeFragment;
    private ProfileFragment profileFragment;
    private Fragment currentFragment;

    private SwipeRefreshLayout swipeRefreshLayout;

    // 底部导航栏标签状态
    private enum TabState {
        HOME, FRIENDS, MESSAGE, PROFILE
    }
    private TabState currentTab ;// = TabState.HOME

    // 缓存管理器
    private CacheManager cacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity创建");


//        Intent intent = getIntent();
//        boolean isOffline = intent.getBooleanExtra("is_offline_mode", false);
//        if (isOffline) {
//            // 断网模式逻辑：比如初始化本地数据库
//            Toast.makeText(this, "当前是：断网模式", Toast.LENGTH_SHORT).show();
//        } else {
//            // 联网模式逻辑：比如初始化网络请求库
//            Toast.makeText(this, "当前是：联网模式", Toast.LENGTH_SHORT).show();
//        }

        // 使用ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化PreferenceManager
        preferenceManager = PreferenceManager.getInstance(this);
        // loadUserPreferences(); // 暂时注释掉，方法未定义

        // 初始化缓存管理器
        initCacheManager();

        // 初始化Fragment成员变量
        initFragments();

        // 设置点击事件监听器
        setupClickListeners();

        // 默认选中社区Tab
        binding.tabLayout.getTabAt(3).select();

        // 默认显示首页
        showFragment(TabState.HOME);
    }


      /**
     * 初始化缓存管理器
     */
    private void initCacheManager() {
        try {
            cacheManager = CacheManager.getInstance(this);
            Log.d(TAG, "缓存管理器初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化缓存管理器失败", e);
        }
    }

    //声明成员变量
    private void initFragments() {
        fragmentManager = getSupportFragmentManager();
        homeFragment = new HomeFragment();
        profileFragment = new ProfileFragment();

        // 初始化时添加所有Fragment，但不显示
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, homeFragment, "home");
        transaction.add(R.id.fragment_container, profileFragment, "profile");
        transaction.hide(profileFragment); // 先隐藏非HomeFragment
        transaction.commit();

        currentFragment = homeFragment;
        currentTab = TabState.HOME;
    }

    private void setupClickListeners() {
        // 菜单按钮点击事件
        binding.menuButton.setOnClickListener(v -> {
            Log.d(TAG, "菜单按钮被点击");
        });

        // 搜索按钮点击事件
        binding.searchButton.setOnClickListener(v -> {
            Log.d(TAG, "搜索按钮被点击");
        });

        // 拍摄按钮点击事件
        binding.captureButton.setOnClickListener(v -> {
            Log.d(TAG, "拍摄按钮被点击");
        });

        // 底部导航栏点击事件
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // 首页标签点击
        binding.homeTab.setOnClickListener(v -> {
            Log.d(TAG, "首页标签被点击");
            showFragment(TabState.HOME);
        });

        // 朋友标签点击
        binding.friendsTab.setOnClickListener(v -> {
            Log.d(TAG, "朋友标签被点击");
            showFragment(TabState.FRIENDS);
        });

        // 消息标签点击
        binding.messageTab.setOnClickListener(v -> {
            Log.d(TAG, "消息标签被点击");
            showFragment(TabState.MESSAGE);
        });

        // 我标签点击
        binding.profileTab.setOnClickListener(v -> {
            Log.d(TAG, "我标签被点击");
//            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
//            startActivity(intent);
//            finish();
            showFragment(TabState.PROFILE);
        });
    }

    private void showFragment(TabState tabState) {
        Log.d(TAG, "tabState:" + String.valueOf(tabState));
        Log.d(TAG, "currentTab:" + String.valueOf(currentTab));

        if (currentTab == tabState) {
            Toast.makeText(this, "已经是当前页面，不需要切换", Toast.LENGTH_SHORT).show();
            return; // 已经是当前页面，不需要切换
        }

        Fragment targetFragment = null;

        switch (tabState) {
            case HOME:
                targetFragment = homeFragment;
                updateTabStyle(binding.homeTab, true);
                updateTabStyle(binding.friendsTab, false);
                updateTabStyle(binding.messageTab, false);
                updateTabStyle(binding.profileTab, false);
                break;
            case FRIENDS:
                updateTabStyle(binding.homeTab, false);
                updateTabStyle(binding.friendsTab, true);
                updateTabStyle(binding.messageTab, false);
                updateTabStyle(binding.profileTab, false);
                Toast.makeText(this, "朋友功能开发中...", Toast.LENGTH_SHORT).show();
                currentTab = tabState;//开发页面后删除
                return;
            case MESSAGE:
                updateTabStyle(binding.homeTab, false);
                updateTabStyle(binding.friendsTab, false);
                updateTabStyle(binding.messageTab, true);
                updateTabStyle(binding.profileTab, false);
                Toast.makeText(this, "消息功能开发中...", Toast.LENGTH_SHORT).show();
                currentTab = tabState;//开发页面后删除
                return;
            case PROFILE:
                targetFragment = profileFragment;
                updateTabStyle(binding.homeTab, false);
                updateTabStyle(binding.friendsTab, false);
                updateTabStyle(binding.messageTab, false);
                updateTabStyle(binding.profileTab, true);
                break;
        }

        // 使用 show/hide 方式切换 Fragment，保持Fragment不被销毁
        if (targetFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // 隐藏当前Fragment，显示目标Fragment
            if (currentFragment != null && currentFragment != targetFragment) {
                transaction.hide(currentFragment);
            }
            transaction.show(targetFragment);
            transaction.commit();

            currentFragment = targetFragment;
            currentTab = tabState;

            Log.d(TAG, "Fragment切换完成: " + targetFragment.getClass().getSimpleName());
        }
    }

  
    private void updateTabStyle(View tabView, boolean isActive) {
        if (tabView instanceof TextView) {
            TextView textView = (TextView) tabView;
            if (isActive) {
                textView.setTextAppearance(this, R.style.BottomNavActiveTabStyle);
            } else {
                textView.setTextAppearance(this, R.style.BottomNavTabStyle);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume");

        // 检查是否需要清理缓存
        checkAndCleanupCacheIfNeeded();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy");

        // 清理资源
        if (binding != null) {
            binding = null;
        }

        // 清理Fragment引用
        currentFragment = null;
        homeFragment = null;
        profileFragment = null;
        fragmentManager = null;

        // 缓存管理器不需要在Activity级别清理，由Application管理
    }

    /**
     * 检查并清理缓存
     */
    private void checkAndCleanupCacheIfNeeded() {
        if (cacheManager == null) return;

        try {
            // 检查是否需要执行缓存清理
            if (cacheManager.shouldCleanup()) {
                Log.d(TAG, "MainActivity触发缓存清理");

                cacheManager.performCleanup(new CacheManager.CleanupCallback() {
                    @Override
                    public void onSuccess(CacheManager.CleanupResult result) {
                        Log.d(TAG, "MainActivity缓存清理完成: " + result.toString());

                        // 可以在这里添加Toast提示用户
                        runOnUiThread(() -> {
                            // Toast.makeText(MainActivity.this,
                            //     "缓存清理完成，释放了 " + formatFileSize(result.totalCleanedSize),
                            //     Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "MainActivity缓存清理失败: " + error);
                    }
                });
            } else {
                Log.d(TAG, "MainActivity当前无需清理缓存");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查缓存清理时出错", e);
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 手动触发缓存清理（可由外部调用）
     */
    public void manualCleanupCache() {
        if (cacheManager == null) return;

        Log.d(TAG, "手动触发缓存清理");
        cacheManager.performCleanup();
    }

    /**
     * 获取缓存统计信息
     */
    public void getCacheStats(CacheManager.CacheStatsCallback callback) {
        if (cacheManager != null) {
            cacheManager.getCacheStats(callback);
        }
    }

    /**
     * 紧急清理内存中的Post数据（针对9GB问题）
     */
    public void emergencyCleanupMemoryData() {
        Log.w(TAG, "执行紧急内存数据清理");

        try {
            // 清理HomeFragment中的内存数据
            if (homeFragment != null) {
                homeFragment.emergencyCleanupMemory();
            }

            // 清理ProfileFragment中的内存数据（如果有的话）
            if (profileFragment != null) {
                // profileFragment.emergencyCleanupMemory(); // 如果有这个方法的话
            }

            // 强制垃圾回收
            System.gc();

            Log.w(TAG, "紧急内存数据清理完成");

        } catch (Exception e) {
            Log.e(TAG, "紧急内存清理失败", e);
        }
    }
}