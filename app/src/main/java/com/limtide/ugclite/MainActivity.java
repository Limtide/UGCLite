package com.limtide.ugclite;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.limtide.ugclite.activity.ProfileActivity;
import com.limtide.ugclite.databinding.ActivityMainBinding;
import com.limtide.ugclite.fragment.HomeFragment;
import com.limtide.ugclite.fragment.ProfileFragment;
import com.limtide.ugclite.utils.PreferenceManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity创建");

        // 检查并处理首次启动
        // handleFirstLaunch(); // 暂时注释掉，方法未定义

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

        // 初始化Fragment成员变量
        initFragments();

        // 设置点击事件监听器
        setupClickListeners();

        // 默认选中社区Tab
        binding.tabLayout.getTabAt(3).select();

        // 默认显示首页
        showFragment(TabState.HOME);
    }


    //声明成员变量
    private void initFragments() {
        fragmentManager = getSupportFragmentManager();
        homeFragment = new HomeFragment();
        profileFragment = new ProfileFragment();
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

        // 使用 replace 方式切换 Fragment
        // 使用 replace 更换后，Fragment走完了生命周期，无法保存已经浏览的内容。
        if (targetFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, targetFragment);
            transaction.commit();

            currentFragment = targetFragment;
            currentTab = tabState;
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
}