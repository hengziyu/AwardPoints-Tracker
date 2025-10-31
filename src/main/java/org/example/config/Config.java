package org.example.config;

/**
 * 配置常量 (原 Config.java 保持逻辑)
 */
public final class Config {
    private Config() {}
    public static final String SOURCE_PATH = "Raw_Source.xlsx"; // 原始随机生成源文件
    public static final String FILE_PATH = "Awards_Summary.xlsx";      // 汇总后的奖项文件
    public static final String NEW_FILE_PATH = "Student_Awards.xlsx";  // 学生奖项及积分持久化文件
    public static final String DB_PATH = "student.db";                // SQLite 数据库文件
    public static final boolean USE_RANDOM_DATA = true;
    public static final int QUANTITY = 10;
    public static final int MAX_AWARDS = 20;
    public static final boolean RANDOM_AWARDS_COUNT = true;
}
