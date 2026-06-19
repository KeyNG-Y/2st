package com.learning.a2st.api;

import com.learning.a2st.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AMapWeatherApi {
    // 获取数据：Retrofit 会向高德 API 的基础 URL（https://restapi.amap.com/）拼接上 v3/weather/weatherInfo，也就是去请求高德天气的实况
    @GET("v3/weather/weatherInfo")
    // Call<WeatherResponse> 表示这个请求会返回一个 Call 对象，它里面封装了高德返回的数据，并且 Retrofit 会自动把 JSON 解析成你之前定义的 WeatherResponse 类
    Call<WeatherResponse> getWeather(
            @Query("city") String cityCode,     // 告诉高德你要查哪个城市的天气
            @Query("extensions") String extensions, // 告诉高德你要什么级别的天气数据
            @Query("key") String apiKey // 在高德开放平台申请的那串 API Key
    );
}