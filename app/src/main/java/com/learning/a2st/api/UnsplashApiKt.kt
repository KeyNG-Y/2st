package com.learning.a2st.api

import com.learning.a2st.model.UnsplashPhoto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface UnsplashApiKt {
    @Headers("Authorization: Client-ID DxELR9xUQiyFtHQU5mA3562LtybRWOtSarUeWAQkIvI")
    @GET("photos/random")
    // 使用 suspend 挂起函数。Retrofit 底层会自动把它切换到后台线程执行。
    suspend fun getRandomPhotos(@Query("count") count: Int): List<UnsplashPhoto>
}