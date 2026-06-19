package com.learning.a2st;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.learning.a2st.model.ImageItem;
import com.learning.a2st.utils.ImageLoader;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private Context context;
    private List<ImageItem> dataList;

    public ImageAdapter(Context context, List<ImageItem> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // position：代表当前列表项在列表中的位置
        ImageItem item = dataList.get(position);    // 根据这个位置，从总数据列表里取出对应的 ImageItem 对象。

        // 1. 绑定文本数据
        holder.Description.setText(item.getDescription());

        // ================== 【动态获取本地文件大小】 ==================
        // 这里的路径必须与你的 ImageLoader 内部的磁盘缓存路径完全一致
        java.io.File diskCacheDir = new java.io.File(context.getCacheDir(), "images");
        String fileName = com.learning.a2st.utils.MD5Utils.hashKeyForDisk(item.getImageUrl());
        java.io.File imageFile = new java.io.File(diskCacheDir, fileName);

        String sizeText = "网络图片";
        if (imageFile.exists()) {
            long bytes = imageFile.length();
            if (bytes < 1024) {
                sizeText = bytes + " B";
            } else if (bytes < 1024 * 1024) {
                sizeText = (bytes / 1024) + " KB";
            } else {
                sizeText = String.format(java.util.Locale.getDefault(), "%.1f MB", (float) bytes / (1024 * 1024));
            }
        }

        // ex：日期是 "2026-05-27"，大小是 "2.5MB"，拼完后 metaText 就变成了 "2026-05-27 · 2.5MB"
        String metaText = item.getDate() + " · " + sizeText;
        holder.MetaInformation.setText(metaText);   // 最后把拼接好的这整段文字，一次性在“元信息文本框”里展示出来。

        // 2. 清理复用残留：为了防止滑动时闪烁，先将图片置空或设置默认底色
        holder.Cover.setImageBitmap(null);

        // 3. 加载 ImageLoader 加载图片！
        // 内部已经包含了防错乱的 setTag 逻辑和三级缓存处理
        ImageLoader.getInstance(context).displayImage(item.getImageUrl(), holder.Cover);

        // 4. 点击事件跳转详情页，并携带该图文的所有元数据
        final String finalSizeText = sizeText;
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, DetailedActivity.class);
            intent.putExtra("IMAGE_URL", item.getImageUrl());
            intent.putExtra("DESC", item.getDescription());
            intent.putExtra("DATE", item.getDate());
            intent.putExtra("SIZE", finalSizeText); // 传递动态计算的大小到详情页
            intent.putExtra("STORAGE_PATH", item.getStoragePath());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();  // 列表里一共有多少条数据
    }

    // ViewHolder 内部类：findViewById 找出来存好 view ，列表快速滑动时，从而保证了滑动的极致流畅。
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView Cover;    // 封面图（Cover）
        TextView Description;   // 描述（Description）
        TextView MetaInformation;  // 元数据信息（meta_information）

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            Cover = itemView.findViewById(R.id.cover);
            Description = itemView.findViewById(R.id.description);
            MetaInformation = itemView.findViewById(R.id.meta_information);
        }
    }
}
