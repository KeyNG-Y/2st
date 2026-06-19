package com.learning.a2st;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------------------------------------
    // 判断 App 是否是第一次运行
    private static final String PREFS_NAME = "AppPrefs";        // 定义了用来存取数据的文件名
    private static final String KEY_FIRST_RUN = "first_run";    // 定义了记录“是否首次运行”这个状态的专属标签（键名）

    // -------------------------------------------------------------------------------------------------------
    // 测试账号
    private static final String PRESET_USER = "taylor";
    private static final String PRESET_PASSWORD = "tttttt";

    // -------------------------------------------------------------------------------------------------------
    private EditText userEditText;  //用户名输入栏
    private EditText passwordEditText;  //密码输入栏
    private ImageView eye;  // 眼睛图标
    private boolean isPasswordVisible = false; // 标记密码当前是否可见
    private TextView forget;  // 忘记密码控件变量
    private Button loginbutton; //登录
    private TextView registertext;  //注册
    private LinearLayout wechatlogin;   //微信登录
    private LinearLayout applelogin;    //苹果登录

    private DatabaseHelper dbHelper;    //数据库管理辅助类

    // -------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 布局文件名

        // ---------------------------------------------------------------------------------------------------
        // 初始化控件
        userEditText = findViewById(R.id.user);
        passwordEditText = findViewById(R.id.password);
        eye = findViewById(R.id.eye);
        forget = findViewById(R.id.forget);
        loginbutton = findViewById(R.id.loginbutton);
        registertext = findViewById(R.id.registertext);
        wechatlogin = findViewById(R.id.wechatlogin);
        applelogin = findViewById(R.id.applelogin);
        dbHelper = new DatabaseHelper(this);    // 初始化数据库

        // ---------------------------------------------------------------------------------------------------
        // ========== 首次运行：预埋账号到SQLite ==========
        // 打开 AppPrefs ，MODE_PRIVATE 表示这本“记事本”是私有的，只有自己的 App 能读写
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true); // 拿着标签 KEY_FIRST_RUN，找对应的值

        if (isFirstRun) {
            // 插入预埋账号
            long result = dbHelper.insertUser(PRESET_USER, PRESET_PASSWORD);    // 返回插入新行的 ID
            if (result != -1) {
                // 标记已执行过首次初始化
                prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();  // 把 KEY_FIRST_RUN 改成 false，异步保存
                Toast.makeText(this, "已预埋测试账号: " + PRESET_USER, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "预埋账号失败", Toast.LENGTH_SHORT).show();
            }
        }

        // ---------------------------------------------------------------------------------------------------
        // ========== 登录验证（查询SQLite） ==========
        loginbutton.setOnClickListener(v -> {
            // 抓取并清理用户输入的账号：trim()自动去掉用户多打的空格
            String inputUser = userEditText.getText().toString().trim();
            String inputPassword = passwordEditText.getText().toString().trim();

            if (inputUser.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show();
                return;
            }

            // 查询数据库验证
            boolean isValid = dbHelper.checkUser(inputUser, inputPassword);
            if (isValid) {
                Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();
                // 把当前登录成功的账号存到 SharedPreferences 里，作为通行证
                // Todo:自动登录
                prefs.edit().putString("CURRENT_USER", inputUser).apply();
                // 跳转
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                // 关闭登录页，防止用户在个人中心点击系统返回键又退回登录页
                finish();
            } else {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------------------------------------------------------------------------------------------------
        // 密码显示/隐藏逻辑
        // 刚打开 App 时，密码是默认隐藏状态
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        eye.setImageResource(R.drawable.eyeclose);
        isPasswordVisible = false;
        eye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // 密码当前是可见的，需要隐藏
                    passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    eye.setImageResource(R.drawable.eyeclose); // 切换为闭眼图标
                } else {
                    // 密码当前是隐藏的，需要显示
                    passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    eye.setImageResource(R.drawable.eyeopen); // 切换为睁眼图标
                }
                isPasswordVisible = !isPasswordVisible;
                // 将光标移动到文本末尾
                passwordEditText.setSelection(passwordEditText.getText().length());
            }
        });

        // ---------------------------------------------------------------------------------------------------
        // 忘记密码？
        forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 方案 A：弹出一个简单的提示（适合演示）
                Toast.makeText(MainActivity.this, "功能开发中...", Toast.LENGTH_LONG).show();

                // 方案 B：跳转到另一个 Activity（如果有 ResetPasswordActivity）
                // Intent intent = new Intent(MainActivity.this, ResetPasswordActivity.class);
                // startActivity(intent);

                // 方案 C：直接打开浏览器跳转到网页（如果有网页版找回）
                // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.baidu.com"));
                // startActivity(intent);
            }
        });

        // ---------------------------------------------------------------------------------------------------
        // 立即注册！
        registertext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"功能开发中...",Toast.LENGTH_LONG).show();
            }
        });

        // ---------------------------------------------------------------------------------------------------
        // 微信登录
        wechatlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"功能开发中...",Toast.LENGTH_LONG).show();
            }
        });

        // ---------------------------------------------------------------------------------------------------
        //苹果登录
        applelogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"功能开发中...",Toast.LENGTH_LONG).show();
            }
        });

        // ---------------------------------------------------------------------------------------------------

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

}