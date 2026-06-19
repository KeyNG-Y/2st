package com.learning.a2st

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.learning.a2st.api.UnsplashApiKt // 注意：这里需要导入你改写后的 Kotlin 版 API 接口
import com.learning.a2st.model.ImageItem
import com.learning.a2st.model.UnsplashPhoto
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val imagelist = mutableListOf<ImageItem>()

    // 标记当前是否正在请求网络数据
    private var isLoading = false

    // 本地持久化缓存的标签
    companion object {
        private const val PREFS_NAME = "UnsplashCachePrefs"
        private const val KEY_PHOTOS_JSON = "cached_photos_json"
    }

    private lateinit var unsplashApi: UnsplashApiKt

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 Retrofit，不再需要 RxJava 的 CallAdapterFactory
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        unsplashApi = retrofit.create(UnsplashApiKt::class.java)

        // 1. 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view)

        // 2. 设置创建瀑布流布局管理器：2列，垂直方向滑动
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        // 防止瀑布流滑动时 Item 左右跳动
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        recyclerView.layoutManager = layoutManager

        // 3. 绑定适配器
        adapter = ImageAdapter(requireContext(), imagelist)
        recyclerView.adapter = adapter

        // 4. 监听滑动到底部的逻辑
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // dy > 0 表示用户正在向下滑动
                if (dy > 0) {
                    val staggeredGridLayoutManager = recyclerView.layoutManager as? StaggeredGridLayoutManager
                    staggeredGridLayoutManager?.let {
                        // 获取瀑布流两列中，最后一个可见 item 的位置
                        val lastPositions = IntArray(it.spanCount)
                        it.findLastVisibleItemPositions(lastPositions)

                        // 瀑布流高度不一，取两列中最大的那个索引
                        val lastVisibleItemPosition = lastPositions.maxOrNull() ?: 0
                        val totalItemCount = it.itemCount

                        // 如果当前没有在加载，并且已经滑动到了倒数第 2 个 item，就开始提前加载下一页
                        if (!isLoading && lastVisibleItemPosition >= totalItemCount - 2) {
                            loadMorePhotos()
                        }
                    }
                }
            }
        })

        // 5. 优先加载本地保存的图片元数据
        if (loadPhotosFromLocalCache()) {
            // 如果本地有缓存的 URL 序列，通知适配器刷新，图片将完美命中 ImageLoader 的磁盘缓存，瞬间展现
            adapter.notifyDataSetChanged()
        } else {
            // 如果是第一次打开应用，本地毫无缓存，再去请求 Unsplash 线上服务
            fetchUnsplashPhotos()
        }
    }

    // 尝试从本地 SharedPreferences 中恢复上次请求成功的图片元数据
    private fun loadPhotosFromLocalCache(): Boolean {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_PHOTOS_JSON, null) ?: return false

        return try {
            val gson = Gson()
            val type: Type = object : TypeToken<List<UnsplashPhoto>>() {}.type
            val cachedList: List<UnsplashPhoto>? = gson.fromJson(cachedJson, type)

            if (!cachedList.isNullOrEmpty()) {
                imagelist.clear()
                for (photo in cachedList) {
                    val url = photo.urls?.regular ?: ""
                    val date = if (!photo.created_at.isNullOrEmpty() && photo.created_at.length >= 10) {
                        photo.created_at.substring(0, 10)
                    } else {
                        "未知时间"
                    }
                    val desc = photo.alt_description ?: "Unsplash 精彩瞬间"

                    imagelist.add(ImageItem(url, date, "", "Unsplash", desc))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 从网络异步抓取 Unsplash 随机图片
    private fun fetchUnsplashPhotos() {
        // lifecycleScope 自动绑定 Fragment 生命周期，无需在 onDestroyView 中手动销毁流
        lifecycleScope.launch {
            try {
                // 调用 suspend 函数发起请求，底层自动在 IO 线程执行并挂起当前协程，不阻塞主线程
                val networkPhotos = unsplashApi.getRandomPhotos(20)

                if (!isAdded || activity == null) return@launch

                // 持久化沉淀到本地
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val jsonString = Gson().toJson(networkPhotos)
                prefs.edit().putString(KEY_PHOTOS_JSON, jsonString).apply()

                imagelist.clear()
                for (photo in networkPhotos) {
                    val url = photo.urls?.regular ?: ""
                    val date = if (!photo.created_at.isNullOrEmpty() && photo.created_at.length >= 10) {
                        photo.created_at.substring(0, 10)
                    } else {
                        "未知时间"
                    }
                    val desc = photo.alt_description ?: "Unsplash 精彩瞬间"

                    imagelist.add(ImageItem(url, date, "", "Unsplash", desc))
                }

                // 刷新瀑布流界面
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                if (!isAdded || activity == null) return@launch
                e.printStackTrace()
                Toast.makeText(context, "获取图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 触底加载更多 Unsplash 图片（协程实现）
    private fun loadMorePhotos() {
        isLoading = true // 马上锁定状态，防止重复触发
        Toast.makeText(context, "正在加载更多...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val newPhotos = unsplashApi.getRandomPhotos(10)

                if (!isAdded || activity == null) return@launch

                isLoading = false // 数据回来了，解除锁定

                // 记录一下新数据插入前，旧列表的末尾索引是多少
                val startInsertPosition = imagelist.size

                // 解析新图片并追加到全局列表中
                for (photo in newPhotos) {
                    val url = photo.urls?.regular ?: ""
                    val date = if (!photo.created_at.isNullOrEmpty() && photo.created_at.length >= 10) {
                        photo.created_at.substring(0, 10)
                    } else {
                        "未知时间"
                    }
                    val desc = photo.alt_description ?: "Unsplash 精彩瞬间"

                    imagelist.add(ImageItem(url, date, "", "Unsplash", desc))
                }

                // 定向插入刷新，保留滑动位置并自带动画
                adapter.notifyItemRangeInserted(startInsertPosition, newPhotos.size)

            } catch (e: Exception) {
                if (!isAdded || activity == null) return@launch
                isLoading = false // 请求失败也要解除锁定
                e.printStackTrace()
                Toast.makeText(context, "网络状况不佳，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }
}