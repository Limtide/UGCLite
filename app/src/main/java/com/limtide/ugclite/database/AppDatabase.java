package com.limtide.ugclite.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.limtide.ugclite.database.dao.UserDao;
import com.limtide.ugclite.database.entity.User;

/**
 * 应用数据库主类
 * 使用Room数据库管理应用数据
 */
@Database(
    entities = {User.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({})
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 获取用户数据访问对象
     */
    public abstract UserDao userDao();

    // 数据库名称
    private static final String DATABASE_NAME = "ugclite_database";

    // 单例实例
    private static volatile AppDatabase INSTANCE;

    /**
     * 获取数据库实例（单例模式）
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // 数据库创建时插入默认数据
                                    // 这里可以预埋测试账号
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 重置数据库实例（用于测试）
     */
    public static void resetInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}