package com.learning.a2st;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// 数据文件存储在应用的私有目录：/data/data/<package_name>/databases/下


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "UsersDB.db";    // 数据库文件名
    private static final int DATABASE_VERSION = 1;   // 数据库版本号
    public static final String TABLE_USERS = "users";   // 表名

    private static final String COL_ID = "id";   // 列名
    private static final String COL_USERNAME = "username";     // 列名
    private static final String COL_PASSWORD = "password";  // 列名
    private static final String COL_SIGNATURE = "signature";

    // 创建表的 SQL 语句
    private static final String TABLE_CREATE =
        "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_SIGNATURE + " TEXT)";

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库版本升级时调用
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        // 简单的升级策略：删除旧表，创建新表（会丢失数据）
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);

        // 在实际项目中，应实现更复杂的数据迁移逻辑
        // 例如，当从版本1升级到版本2时，可能需要添加新列：
        // if (oldVersion < 2) {
        //     db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN priority INTEGER DEFAULT 0");
        // }
    }

    // 插入用户（注册/预埋）
    public long insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();     // 准备写入通道
        ContentValues values = new ContentValues();         // 打包待存入的数据
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);    // 执行插入操作，返回的long是插入成功后，自动生成的行号
    }

    // 验证登录
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();     // 准备读取通道：只读
        Cursor cursor = db.query(TABLE_USERS,               // 要在 users 这张表里找，结果装在cursor对象返回
                new String[]{COL_ID},                       // 如果找到了，只需要把那行数据的 ID 拿回来就行
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", // 同时满足‘邮箱等于某个值’ 并且 ‘密码也等于某个值’
                new String[]{username, password},           // 把上面两个 ? 替换成用户实际输入的 email 和 password
                null, null, null);

        boolean exists = cursor.getCount() > 0;             // 搜索结果集”里一共有多少条数据
        cursor.close();
        return exists;
    }

    // 检查数据库是否已有用户（用于判断首次运行）
    public boolean hasUsers() {
        SQLiteDatabase db = this.getReadableDatabase();     // 打开数据库的只读模式
        // 执行了一条rawQuery (原始查询)：原生的 SQL 语句：SELECT COUNT(*) FROM user:数一下表里总共有多少行数据
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        boolean has = false;                            // 准备一个变量 has，默认值为 false（假设没有用户）
        if (cursor.moveToFirst()) {                     // 游标移动到查询结果的第一行
            has = cursor.getInt(0) > 0;     // 从当前行的第 0 列（也就是刚才统计出来的总数）提取出一个整数
        }
        cursor.close();
        return has;
    }

    // 获取签名
    public String getSignature(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_SIGNATURE},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        String signature = "Hello!"; // 默认签名
        if (cursor.moveToFirst()) {
            // 如果数据库里的签名不是空的，就取出来
            if (!cursor.isNull(0)) {
                signature = cursor.getString(0);
            }
        }
        cursor.close();
        return signature;
    }

    // 更新签名
    public void updateSignature(String username, String newSignature) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SIGNATURE, newSignature);
        // 执行更新：UPDATE users SET signature = ? WHERE username = ?
        db.update(TABLE_USERS, values, COL_USERNAME + "=?", new String[]{username});
    }
}
