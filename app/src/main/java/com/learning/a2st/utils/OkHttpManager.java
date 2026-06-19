package com.learning.a2st.utils;

import java.util.concurrent.TimeUnit;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.io.InputStream;
import okhttp3.Response;
import java.io.IOException;

public class OkHttpManager {

    private static volatile OkHttpManager instance; // 用于保存该类的唯一实例
    private final OkHttpClient client;  // OkHttp 的核心执行者，final 保证它在初始化后不会被修改

    // ------------------------------------------------------------------------------------------------------
    // 私有化构造器
    private OkHttpManager() {
        // 配置 OkHttpClient 最佳实践：设置连接和读取的超时时间15s, 防止网络请求无限期卡死
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------
    // 双重校验锁单例模式 (Double-Checked Locking)，保证多线程环境下的绝对安全
    // 全局访问方法：提供一个公开的静态方法 getInstance()，让外部能获取到唯一的 OkHttpManager 实例
    public static OkHttpManager getInstance() {
        if (instance == null) { // 双重校验锁（DCL） 第一次：为了提高效率，如果实例已经存在，就直接返回，避免每次都去加锁。
            synchronized (OkHttpManager.class) {    // synchronized给类对象加锁，保证同一时间只有一个线程能进入代码块创建实例。
                if (instance == null) { // 第二次：防止多个线程同时通过了第一次判断，在锁外等待，导致重复创建实例。
                    instance = new OkHttpManager();
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------------------------------------------
    public void enqueueGet(String url, Callback callback) { // 接收两个参数: 请求的网址 url 和请求结果的回调 callback
        Request request = new Request.Builder().url(url).get().build(); // 构建一个标准的 HTTP GET 请求对象
        // 发起异步请求，不会阻塞主线程
        // 将任务加入 OkHttp 内部的调度队列（Dispatcher），由线程池在后台执行。这意味着该请求不会阻塞主线程（UI线程），
        // 适合在 Android 等移动端开发中获取网络数据或图片。请求成功或失败的结果会通过 callback 接口返回
        client.newCall(request).enqueue(callback);
    }

    // ------------------------------------------------------------------------------------------------------
    // 在自定义的工作线程里发起网络请求，需要 OkHttp 提供一个同步获取流的方法。
    // 同步的网络请求方法: 向指定的网址发起请求，如果请求成功，就把服务器返回的数据以“数据流（InputStream）”的形式交给你。
    public InputStream getStreamSync(String url) {
        // 利用 OkHttp 的建造者模式，把传入的 url 包装成一个标准的 HTTP GET 请求对象
        Request request = new Request.Builder().url(url).get().build();
        try {
            // execute() 是同步方法，会阻塞当前线程直到响应返回
            // 发起同步请求
            Response response = client.newCall(request).execute();
            // 判断服务器返回的状态码是不是在 200 到 300 之间，代表请求在 HTTP 层面是成功的。确保服务器确实返回了实体内容
            if (response.isSuccessful() && response.body() != null) {
                return response.body().byteStream(); // 返回字节流
            }
        } catch (IOException e) {
            // 如果在请求过程中发生了网络中断等 IOException，程序会跳到 catch 块里，把错误的堆栈信息打印到控制台（e.printStackTrace()），方便你调试查错
            e.printStackTrace();
        }
        return null;
    }
    // ------------------------------------------------------------------------------------------------------
}