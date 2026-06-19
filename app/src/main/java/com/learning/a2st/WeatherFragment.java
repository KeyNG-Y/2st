package com.learning.a2st;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.learning.a2st.api.AMapWeatherApi;
import com.learning.a2st.model.Cast;
import com.learning.a2st.model.Forecast;
import com.learning.a2st.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class WeatherFragment extends Fragment {
    private TextView tvCityName, tvCurrentTemp, tvHighLow, tvDayDetails, tvNightDetails;
    private static final String API_KEY = "909a92a86064aa4ec8523c90ab3b70d0";

    private AMapWeatherApi weatherApi;
    @Nullable       // 表示该值可以为 null, 代表这个 Fragment 没有界面，只在后台运行。@NonNull：表示该值绝对不能为 null
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 1. 绑定 UI
        tvCityName = view.findViewById(R.id.tv_city_name);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvHighLow = view.findViewById(R.id.tv_high_low);
        tvDayDetails = view.findViewById(R.id.tv_day_details);
        tvNightDetails = view.findViewById(R.id.tv_night_details);
        Button btnBeijing = view.findViewById(R.id.btn_beijing);
        Button btnShanghai = view.findViewById(R.id.btn_shanghai);
        Button btnHangzhou = view.findViewById(R.id.btn_hangzhou);
        Button btnGuangzhou = view.findViewById(R.id.btn_guangzhou);

        // 2. 初始化 Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://restapi.amap.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherApi = retrofit.create(AMapWeatherApi.class);

        // 3. 设置点击事件 (北京的adcode:110000, 上海：310000, 杭州：330100, 广州:440100，)
        btnBeijing.setOnClickListener(v -> fetchWeatherWithRetrofit("110000"));
        btnShanghai.setOnClickListener(v -> fetchWeatherWithRetrofit("310000"));
        btnHangzhou.setOnClickListener(v -> fetchWeatherWithRetrofit("330100"));
        btnGuangzhou.setOnClickListener(v -> fetchWeatherWithRetrofit("440100"));

        // 4. 打开页面默认请求北京天气
        fetchWeatherWithRetrofit("110000");

    }

    private void fetchWeatherWithRetrofit(String cityCode) {
        Call<WeatherResponse> call = weatherApi.getWeather(cityCode, "all", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                // 如果 Fragment 已经不在屏幕上了，直接终止，防崩溃！
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();
                    // 先看服务器返回的状态码是不是 "1"（高德 API 中 1 代表成功）
                    if ("1".equals(data.status) && data.forecasts != null && !data.forecasts.isEmpty()) {
                        Forecast cityForecast = data.forecasts.get(0);
                        if (cityForecast.casts != null && !cityForecast.casts.isEmpty()) {
                            Cast today = cityForecast.casts.get(0);
                            updateUI(cityForecast.city, today);
                        }
                    } else {
                        Toast.makeText(getContext(), "数据异常: " + data.info, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e("Weather", "网络请求失败", t);
                Toast.makeText(getContext(), "网络加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(String cityName, Cast today) {
        tvCityName.setText(cityName);
        tvCurrentTemp.setText(today.daytemp + "°");
        tvHighLow.setText("最高：" + today.daytemp + "° 最低：" + today.nighttemp + "°");

        String dayInfo = "天气：\t\t\t" + today.dayweather + "\n" +
                "温度：\t\t\t" + today.daytemp + "°\n" +
                "风力：\t\t\t" + today.daywind + "风 " + today.daypower + "级";
        tvDayDetails.setText(dayInfo);

        String nightInfo = "天气：\t\t\t" + today.nightweather + "\n" +
                "温度：\t\t\t" + today.nighttemp + "°\n" +
                "风力：\t\t\t" + today.nightwind + "风 " + today.nightpower + "级";
        tvNightDetails.setText(nightInfo);
    }
}

