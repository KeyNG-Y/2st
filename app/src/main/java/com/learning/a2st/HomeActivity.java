package com.learning.a2st;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class HomeActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private HomeFragment_Rxjava homeFragment;
    private ProfileFragment profileFragment;
    private WeatherFragment weatherFragment;
    private LinearLayout tabWeather;
    private Fragment currentFragment;

    private LinearLayout tabHome;
    private LinearLayout tabProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        tabHome = findViewById(R.id.tab_home);
        tabProfile = findViewById(R.id.tab_profile);
        tabWeather = findViewById(R.id.tab_weather);
        fragmentManager = getSupportFragmentManager();

        // 首次启动，初始化并添加 Fragment
        // 无此判断：每次屏幕旋转或 Activity 重建，代码都会重新 new 两个 Fragment 并再次 add 进去，导致界面出现 Fragment 重叠的 Bug
        // 加上这个判断后，能保证这些 Fragment 只在 Activity 第一次被创建时添加一次。
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment_Rxjava();
            profileFragment = new ProfileFragment();
            weatherFragment = new WeatherFragment();

            // 使用 add 方式全部装载，默认隐藏个人中心，只显示首页
            fragmentManager.beginTransaction()  // 在 Android 中，对 Fragment 的添加、移除、替换等操作都需要包裹在一个事务中
                    .add(R.id.fragment_container, homeFragment, "home")  // 将 homeFragment 添加到当前 Activity 布局中 ID 为 fragment_container 的容器里，并给它打上一个名为 "home" 的标签
                    .add(R.id.fragment_container, weatherFragment, "weather") // weatherFragment 也添加到同一个容器里，并打上 "weather" 标签
                    .add(R.id.fragment_container, profileFragment, "profile")   // profileFragment 也添加到同一个容器里，并打上 "profile" 标签
                    .hide(weatherFragment)  // 隐藏天气
                    .hide(profileFragment)  // 隐藏个人主页
                    .commit();  // 调用了 commit() 提交这个事务, 前面设置的所有操作才会真正生效并执行
            currentFragment = homeFragment; // 用一个全局变量 currentFragment 记录当前正在显示的 Fragment 是哪一个

            // 设置底部 Tab 初始颜色状态 (首页文字变黑激活，个人中心文字灰色)
            setTabUI(tabHome, tabWeather, tabProfile);
        } else {
            // 防止内存重启导致状态错乱: 通过标签找回系统保留的 Fragment 实例
            homeFragment = (HomeFragment_Rxjava) fragmentManager.findFragmentByTag("home");
            weatherFragment = (WeatherFragment) fragmentManager.findFragmentByTag("weather");
            profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag("profile");

            // 动态判断系统恢复时，到底哪个页面没有被隐藏，谁没被隐藏谁就是 currentFragment
            if (homeFragment != null && !homeFragment.isHidden()) {
                currentFragment = homeFragment;
            } else if (weatherFragment != null && !weatherFragment.isHidden()) {
                currentFragment = weatherFragment;
            } else {
                currentFragment = profileFragment;
            }
        }

        // 绑定底部导航点击跳转切换
        tabHome.setOnClickListener(v -> switchFragment(homeFragment, tabHome, tabWeather, tabProfile));
        tabWeather.setOnClickListener(v -> switchFragment(weatherFragment, tabWeather, tabHome, tabProfile));
        tabProfile.setOnClickListener(v -> switchFragment(profileFragment, tabProfile, tabHome, tabWeather));
    }


     // 使用 show/hide 事务切换 Fragment，完美保留页面状态
    private void switchFragment(Fragment targetFragment, LinearLayout selectedTab, LinearLayout... unselectedTabs) {
        if (currentFragment == targetFragment) return;  // 防重复点击判断
        fragmentManager.beginTransaction()
                .hide(currentFragment)  // 将当前正在显示的页面隐藏,不销毁
                .show(targetFragment)   // 将目标页面显示出来
                .commit();
        currentFragment = targetFragment;
        setTabUI(selectedTab, unselectedTabs);   // 调用下方的 UI 刷新方法，同步更新底部导航栏的文字颜色。
    }

    // 动态刷新底部 View 的文字高亮状态
    private void setTabUI(LinearLayout selectedTab, LinearLayout... unselectedTabs) {
        // 从传入的“选中 Tab 布局”中，通过 getChildAt(0) 获取它的第一个子控件（默认是显示文字的 TextView），并强转为 TextView
        TextView selectedText = (TextView) selectedTab.getChildAt(0);
        // 从未选中 Tab 布局”中获取它的第一个子控件（TextView）
        selectedText.setTextColor(ContextCompat.getColor(selectedTab.getContext(), android.R.color.black));

        // 2. 遍历所有未选中的 Tab，把它们的文字全部变灰
        for (LinearLayout tab : unselectedTabs) {
            TextView unselectedText = (TextView) tab.getChildAt(0);
            unselectedText.setTextColor(ContextCompat.getColor(tab.getContext(), android.R.color.darker_gray));
        }
    }
}
