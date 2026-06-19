package com.learning.a2st.api;

import com.learning.a2st.model.UnsplashPhoto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

// Java 接口
public interface UnsplashApi_call {
    // Unsplash Access Key添加一个请求头
    @Headers("Authorization: Client-ID DxELR9xUQiyFtHQU5mA3562LtybRWOtSarUeWAQkIvI")
    // Retrofit 最终会向 https://api.unsplash.com/photos/random 这个完整的 URL 发送 GET 请求。这个接口用于获取 Unsplash 上的随机图片
    @GET("photos/random")
    // Call<List<UnsplashPhoto>>（返回值类型）。 @Query("count") int count 添加一个查询参数，把 count 变量的值拼接到 URL 后面，如果count=5，说明返回5张图片。
    Call<List<UnsplashPhoto>> getRandomPhotos(@Query("count") int count);
}