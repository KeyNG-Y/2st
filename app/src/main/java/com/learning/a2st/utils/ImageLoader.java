package com.learning.a2st.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static volatile ImageLoader instance;   //  单例变量：保证全局只有一个图片加载器
    private Context context;

    // L1: 内存缓存 (Android 原生提供的 Lru 算法缓存)
    private LruCache<String, Bitmap> memoryCache;

    // L2: 磁盘缓存目录
    private File diskCacheDir;

    // 多线程管理: 定长线程池 (根据 CPU 核心数动态设置)
    private ExecutorService threadPool;

    // 并发锁: 记录正在下载的 URL，防止 RecyclerView 快速滑动时重复创建同一张图片的下载任务
    private ConcurrentHashMap<String, Boolean> downloadTasks;

    // 线程通信: 用于将子线程下载好的 Bitmap 切回主线程进行 UI 渲染
    private Handler mainHandler;

    // 私有化构造器：外部只能通过 getInstance(Context) 获取。
    private ImageLoader(Context context) {
        this.context = context.getApplicationContext(); // 防止内存泄漏

        // 1. 初始化内存缓存，配置 LruCache：获取当前应用最大可用内存，拿出其中的 1/8 作为图片缓存空间
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 重写此方法来衡量每张图片的大小（以 KB 为单位）
                return bitmap.getByteCount() / 1024;
            }
        };

        // 2. 初始化磁盘缓存目录 (/data/data/<package>/cache/images)，专门用来存放下载下来的图片文件
        diskCacheDir = new File(this.context.getCacheDir(), "images");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }

        // 3. 初始化线程池和相关锁工具：
        // 获取手机的 CPU 核心数，创建对应数量的固定线程池。这样既能充分利用多核性能，又不会因为开启过多线程把手机卡死。
        int cpuCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(cpuCount);
        downloadTasks = new ConcurrentHashMap<>();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // 单例模式获取：标准的双重校验锁（DCL）单例模式，确保在多线程环境下，ImageLoader 只会被初始化一次。
    public static ImageLoader getInstance(Context context) {
        // 第一次检查（无锁）：如果实例已经存在，直接返回，避免加锁带来的性能开销
        if (instance == null) {
            // 同步代码块（加锁）：只有当实例为 null 时，才进入加锁状态
            synchronized (ImageLoader.class) {
                // 第二次检查（有锁）：防止多个线程同时通过了第一次检查，排队进入后重复创建
                if (instance == null) {
                    instance = new ImageLoader(context);
                }
            }
        }
        return instance;
    }


    // 核心加载方法 (供 UI 层直接调用)
    public void displayImage(final String url, final ImageView imageView) {
        // 防错乱1：给 ImageView 绑定当前要加载的 URL
        imageView.setTag(url);

        // 防错乱1. 尝试从内存中获取 (LruCache)
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        // 并发控制：如果当前 URL 已经在下载中，直接拦截，拒绝重复投递任务
        if (downloadTasks.containsKey(url)) {
            return;
        }

        // 加锁：标记当前 URL 正在处理中
        downloadTasks.put(url, true);

        // 提交到子线程处理 (磁盘读取 + 网络请求)
        threadPool.submit(() -> loadFromDiskOrNetwork(url, imageView));
    }

    // 子线程执行：磁盘读取与网络下载
    private void loadFromDiskOrNetwork(String url, ImageView imageView) {
        // 并配合 MD5Utils 实现统一的磁盘无冲突命名
        String fileName = MD5Utils.hashKeyForDisk(url);
        File imageFile = new File(diskCacheDir, fileName);

        // 2. 尝试从本地磁盘获取
        if (imageFile.exists()) {
            Bitmap diskBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (diskBitmap != null) {
                // 读到了，存入内存，并更新 UI
                memoryCache.put(url, diskBitmap);
                updateUI(url, imageView, diskBitmap);
                // 解锁
                downloadTasks.remove(url);
                return;
            }
        }

        // 3. 磁盘也没有，发起网络请求下载 (调用 OkHttpManager 的同步方法)
        InputStream is = OkHttpManager.getInstance().getStreamSync(url);
        if (is != null) {
            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

                // 从刚保存的文件中解析 Bitmap
                Bitmap networkBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (networkBitmap != null) {
                    // 存入内存，并更新 UI
                    memoryCache.put(url, networkBitmap);
                    updateUI(url, imageView, networkBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 无论成功失败，必须解锁，以便下次还能重试
                downloadTasks.remove(url);
            }
        } else {
            // 下载失败也需解锁
            downloadTasks.remove(url);
        }
    }


    // 切回主线程更新 UI
    private void updateUI(final String url, final ImageView imageView, final Bitmap bitmap) {
        mainHandler.post(() -> {
            // 防错乱2：回到主线程真正要上墙时，校验 ImageView 的 Tag 是否还是刚才的 URL
            // （因为 RecyclerView 滑动过快时，ImageView 可能已经被回收去展示别的 URL 了）
            if (imageView.getTag() != null && imageView.getTag().equals(url)) {
                imageView.setImageBitmap(bitmap);
            }
        });
    }
}