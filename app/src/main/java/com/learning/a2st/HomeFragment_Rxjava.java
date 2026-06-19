package com.learning.a2st;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.learning.a2st.api.UnsplashApi_Rxjava;
import com.learning.a2st.model.ImageItem;
import com.learning.a2st.model.UnsplashPhoto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// 引入 RxJava3 核心组件
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment_Rxjava extends Fragment {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<ImageItem> imagelist = new ArrayList<>();

    // 标记当前是否正在请求网络数据
    private boolean isLoading = false;

    // 本地持久化缓存的标签
    private static final String PREFS_NAME = "UnsplashCachePrefs";
    private static final String KEY_PHOTOS_JSON = "cached_photos_json";

    // 引入 CompositeDisposable 容器，用于统一管理并安全解绑所有 RxJava 异步订阅流
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private UnsplashApi_Rxjava unsplashApiRxjava;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 构建 Retrofit 实例时，注入 RxJava3CallAdapterFactory 适配器以支持 Single 接口返回
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.unsplash.com/")
                // Gson（Java库）将json转化为Java对象
                .addConverterFactory(GsonConverterFactory.create())
                // 调用Rxjava适配器
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        unsplashApiRxjava = retrofit.create(UnsplashApi_Rxjava.class);

        // 1. 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);

        // 2. 设置创建瀑布流布局管理器：2列，垂直方向滑动
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        // 防止瀑布流滑动时 Item 左右跳动
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(layoutManager);

        // 3. 绑定适配器
        adapter = new ImageAdapter(requireContext(), imagelist);
        recyclerView.setAdapter(adapter);

        // 4. 监听滑动到底部的逻辑
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // dy > 0 表示用户正在向下滑动
                if (dy > 0) {
                    StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        // 获取瀑布流两列中，最后一个可见 item 的位置
                        int[] lastPositions = layoutManager.findLastVisibleItemPositions(null);
                        // 瀑布流高度不一，取两列中最大的那个索引
                        int lastVisibleItemPosition = Math.max(lastPositions[0], lastPositions[1]);
                        int totalItemCount = layoutManager.getItemCount();

                        // 如果当前没有在加载，并且已经滑动到了倒数第 2 个 item，就开始提前加载下一页
                        if (!isLoading && lastVisibleItemPosition >= totalItemCount - 2) {
                            loadMorePhotos();
                        }
                    }
                }
            }
        });

        // 5. 优先加载本地保存的图片元数据
        if (loadPhotosFromLocalCache()) {
            // 如果本地有缓存的 URL 序列，通知适配器刷新，图片将完美命中 ImageLoader 的磁盘缓存，瞬间展现
            adapter.notifyDataSetChanged();
        } else {
            // 如果是第一次打开应用，本地毫无缓存，再去请求 Unsplash 线上服务
            fetchUnsplashPhotos();
        }
    }

    // 尝试从本地 SharedPreferences 中恢复上次请求成功的图片元数据
    private boolean loadPhotosFromLocalCache() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(KEY_PHOTOS_JSON, null);

        if (cachedJson == null) return false;

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<UnsplashPhoto>>(){}.getType();
            List<UnsplashPhoto> cachedList = gson.fromJson(cachedJson, type);

            if (cachedList != null && !cachedList.isEmpty()) {
                imagelist.clear();
                for (UnsplashPhoto photo : cachedList) {
                    String url = (photo.urls != null) ? photo.urls.regular : "";
                    String date = (photo.created_at != null && photo.created_at.length() >= 10)
                            ? photo.created_at.substring(0, 10) : "未知时间";
                    String desc = (photo.alt_description != null) ? photo.alt_description : "Unsplash 精彩瞬间";

                    imagelist.add(new ImageItem(url, date, "", "Unsplash", desc));
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 从网络异步抓取 Unsplash 随机图片（RxJava3 链式流实现）
    private void fetchUnsplashPhotos() {
        compositeDisposable.add(
                unsplashApiRxjava.getRandomPhotos(20)
                        .subscribeOn(Schedulers.io())               // 切换到子线程
                        .observeOn(AndroidSchedulers.mainThread())  // 切换回主线程来刷新 UI
                        .subscribe(networkPhotos -> {
                            // 成功回调处理
                            if (!isAdded() || getActivity() == null) return;

                            // 将这次请求回来的元数据转化为 JSON 字符串，持久化沉淀到本地
                            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            String jsonString = new Gson().toJson(networkPhotos);
                            prefs.edit().putString(KEY_PHOTOS_JSON, jsonString).apply();

                            imagelist.clear();
                            for (UnsplashPhoto photo : networkPhotos) {
                                String url = (photo.urls != null) ? photo.urls.regular : "";
                                String date = (photo.created_at != null && photo.created_at.length() >= 10)
                                        ? photo.created_at.substring(0, 10) : "未知时间";
                                String desc = (photo.alt_description != null) ? photo.alt_description : "Unsplash 精彩瞬间";

                                imagelist.add(new ImageItem(url, date, "", "Unsplash", desc));
                            }

                            // 刷新瀑布流界面
                            adapter.notifyDataSetChanged();
                        }, throwable -> {
                            // 异常捕获处理
                            if (!isAdded() || getActivity() == null) return;
                            throwable.printStackTrace();
                            Toast.makeText(getContext(), "获取图片失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        })
        );
    }

    // 触底加载更多 Unsplash 图片（RxJava3 链式流实现）
    private void loadMorePhotos() {
        isLoading = true; // 锁定状态，防重复触发
        Toast.makeText(getContext(), "正在加载更多...", Toast.LENGTH_SHORT).show();

        compositeDisposable.add(
                unsplashApiRxjava.getRandomPhotos(10)
                        .subscribeOn(Schedulers.io())               // 异步子线程发起请求
                        .observeOn(AndroidSchedulers.mainThread())  // 主线程刷新响应
                        .subscribe(newPhotos -> {
                            if (!isAdded() || getActivity() == null) return;

                            isLoading = false; // 解除状态锁定

                            // 记录新数据插入前旧列表的末尾索引位置
                            int startInsertPosition = imagelist.size();

                            // 解析新图片并追加到全局列表中
                            for (UnsplashPhoto photo : newPhotos) {
                                String url = (photo.urls != null) ? photo.urls.regular : "";
                                String date = (photo.created_at != null && photo.created_at.length() >= 10)
                                        ? photo.created_at.substring(0, 10) : "未知时间";
                                String desc = (photo.alt_description != null) ? photo.alt_description : "Unsplash 精彩瞬间";

                                imagelist.add(new ImageItem(url, date, "", "Unsplash", desc));
                            }

                            // 定向范围刷新，维持平滑滑动体验并带有默认动画效果
                            adapter.notifyItemRangeInserted(startInsertPosition, newPhotos.size());
                        }, throwable -> {
                            if (!isAdded() || getActivity() == null) return;
                            isLoading = false; // 异常发生时解除锁定防止死锁
                            throwable.printStackTrace();
                            Toast.makeText(getContext(), "网络状况不佳，请重试", Toast.LENGTH_SHORT).show();
                        })
        );
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 在视图销毁时清空容器，切断所有未完成的异步流，从根本上杜绝内存泄漏及 Fragment 销毁后的 NullPointerException 崩溃
        compositeDisposable.clear();
    }
}