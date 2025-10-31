package org.example;

/**
 * 配置常量，对应原 Python config.py
 */
public final class Config {
    private Config() {}

    public static final String SOURCE_PATH = "path_to_your_file.xlsx"; // 原始生成文件
    public static final String FILE_PATH = "Build_Summary.xlsx";      // 汇总文件
    public static final String NEW_FILE_PATH = "newFile.xlsx";        // 用户录入持久化文件
    public static final String DB_PATH = "student.db";                // SQLite 数据库文件

    public static final boolean USE_RANDOM_DATA = true;                // 是否使用随机数据构建
    public static final int QUANTITY = 10;                             // 随机生成人数（与 BuildNull 保持一致）
    public static final int MAX_AWARDS = 20;                           // 最大奖项数
    public static final boolean RANDOM_AWARDS_COUNT = true;            // 奖项数量随机
}