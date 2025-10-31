package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * 迁移自 db_operation.py
 * 功能：
 *  - initDb(): 初始化数据库表 students
 *  - writeDb(id, label, index, val1, val2): 打印基本信息并 UPSERT 学号对应的分数
 * 一比一复原，不留 TODO。
 */
public class DbOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbOperation.class);

    public static void initDb(String dbPath) {
        LOGGER.debug("初始化数据库");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "student_id INTEGER PRIMARY KEY," +
                    "cert_total_points REAL DEFAULT 0.0," +
                    "award_total_points REAL DEFAULT 0.0) ");
        } catch (Exception e) {
            LOGGER.error("数据库初始化失败", e);
        }
    }

    public static void writeDb(long id, String label, int index, double certTotal, double awardTotal) {
        System.out.println("学号" + id);
        System.out.println("类型" + label);
        System.out.println("索引" + index);
        System.out.println("证书总分" + certTotal);
        System.out.println("奖项总分" + awardTotal);

        String sql = "INSERT INTO students (student_id, cert_total_points, award_total_points) VALUES (?,?,?) " +
                "ON CONFLICT(student_id) DO UPDATE SET cert_total_points=excluded.cert_total_points, award_total_points=excluded.award_total_points";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Config.DB_PATH);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LOGGER.debug("数据库链接成功");
            ps.setLong(1, id);
            ps.setDouble(2, certTotal);
            ps.setDouble(3, awardTotal);
            ps.executeUpdate();
            LOGGER.debug("数据存入成功");
        } catch (Exception e) {
            LOGGER.error("写入数据库失败", e);
        }
    }

    // 初始化一次（与 Python 顶层调用一致）
    static {
        initDb(Config.DB_PATH);
    }
}

