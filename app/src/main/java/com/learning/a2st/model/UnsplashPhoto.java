package com.learning.a2st.model;

public class UnsplashPhoto {
    public String created_at;      // 创建时间
    public String alt_description; // 图片描述
    public Urls urls;              // 图片链接合集

    public static class Urls {
        public String regular;     // 常规大小，适合瀑布流
        public String small;       // 缩略图
    }
}