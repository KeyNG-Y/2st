package com.learning.a2st.api;

import com.learning.a2st.model.UnsplashPhoto;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface UnsplashApi_Rxjava {
    // Unsplash Access Key
    @Headers("Authorization: Client-ID DxELR9xUQiyFtHQU5mA3562LtybRWOtSarUeWAQkIvI")
    @GET("photos/random")
    // RxJava3
    Observable<List<UnsplashPhoto>> getRandomPhotos(@Query("count") int count);
}