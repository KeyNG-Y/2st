package com.learning.a2st.model;

public class ImageItem {
    private String imageUrl;      // 网络的图片地址
    private String date;          // 图片日期
    private String size;          // 图片文件大小
    private String storagePath;   // 存储位置/来源描述
    private String description;   // 图片文案描述

    // 构造方法
    public ImageItem(String imageUrl, String date, String size, String storagePath, String description) {
        this.imageUrl = imageUrl;
        this.date = date;
        this.size = size;
        this.storagePath = storagePath;
        this.description = description;
    }

    // Getters and Setters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}