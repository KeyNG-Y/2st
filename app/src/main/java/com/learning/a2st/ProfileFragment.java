package com.learning.a2st;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private TextView Username;
    private TextView Signature;
    private DatabaseHelper dbHelper;

    // 把界面画出来
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate((R.layout.fragment_profile), container, false);
    }

    // 给控件绑定数据和逻辑
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 1. 初始化顶部个人信息控件 (注意：Fragment中需要使用 view.findViewById)
        Username = view.findViewById(R.id.username);
        Signature = view.findViewById(R.id.signature);
        dbHelper = new DatabaseHelper(requireContext());

        // 2. 从 SharedPreferences 跨页面读取登录账号凭证
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String username = prefs.getString("CURRENT_USER", "未知用户");
        String signature = dbHelper.getSignature(username);

        Username.setText(username);
        Signature.setText(signature);

        // 3. 初始化 6 个纵向列表的容器
        ConstraintLayout itemInformation = view.findViewById(R.id.item_information);
        ConstraintLayout itemCollection = view.findViewById(R.id.item_collection);
        ConstraintLayout itemHistory = view.findViewById(R.id.item_history);
        ConstraintLayout itemSetting = view.findViewById(R.id.item_setting);
        ConstraintLayout itemAbout = view.findViewById(R.id.item_about);
        ConstraintLayout itemAdvice = view.findViewById(R.id.item_advice);

        // 4. 创建统一的全条目点击响应监听器
        View.OnClickListener devToastListener = v ->
                Toast.makeText(requireContext(), "功能正在开发中...", Toast.LENGTH_SHORT).show();

        // 5. 将该监听器绑定到所有条目，实现全条目可点
        itemInformation.setOnClickListener(devToastListener);
        itemCollection.setOnClickListener(devToastListener);
        itemHistory.setOnClickListener(devToastListener);
        itemSetting.setOnClickListener(devToastListener);
        itemAbout.setOnClickListener(devToastListener);
        itemAdvice.setOnClickListener(devToastListener);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}

