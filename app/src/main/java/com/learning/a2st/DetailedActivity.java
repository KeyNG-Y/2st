package com.learning.a2st;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.learning.a2st.utils.ImageLoader;

public class DetailedActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed);

        // 初始化控件
        ImageView D_Image = findViewById(R.id.detailed_img);
        ImageView GetBack = findViewById(R.id.getback);
        TextView D_Desc = findViewById(R.id.detailed_desc);
        TextView D_Date = findViewById(R.id.detailed_date);
        TextView D_Size = findViewById(R.id.detailed_size);
        TextView D_Source = findViewById(R.id.detailed_source);

        // 返回按钮点击事件
        GetBack.setOnClickListener(v -> finish());

        // 接收 Intent 传递过来的数据
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        String desc = getIntent().getStringExtra("DESC");
        String date = getIntent().getStringExtra("DATE");
        String size = getIntent().getStringExtra("SIZE");
        String source = getIntent().getStringExtra("SOURCE");

        // 绑定文本数据
        D_Desc.setText(desc != null ? desc : "暂无描述");
        D_Date.setText(date != null ? "日期: " + date : "");
        D_Size.setText(size != null ? "大小: " + size : "");
        D_Source.setText(source != null ? "来源: " + source : "");

        // 直接调用手写的 ImageLoader
        // 由于在列表页已经下载过，这里 ImageLoader 会瞬间在内存 (LruCache) 或本地磁盘中找到它，绝对不会再次发起 OkHttp 网络请求
        if (imageUrl != null) {
            ImageLoader.getInstance(this).displayImage(imageUrl, D_Image);
        }
    }
}
