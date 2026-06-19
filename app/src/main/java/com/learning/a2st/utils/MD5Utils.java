package com.learning.a2st.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 把任意长度的字符串（比如一个很长的网络图片地址 URL），转换成一个固定长度（32位）的、只包含数字和字母的“唯一指纹”字符串

public class MD5Utils {
    // 接收一个字符串 key（比如图片的 URL），返回一个处理好的 cacheKey
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");   // 用 MD5 算法, 获取 MD5 实例
            mDigest.update(key.getBytes());                 // 把传入的字符串打散成字节（byte），喂给 MD5 计算工具
            cacheKey = bytesToHexString(mDigest.digest());  // 转成我们常见的 32 位十六进制字符串
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());      // Java 自带的哈希码作为备选方案
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // 字节转十六进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
