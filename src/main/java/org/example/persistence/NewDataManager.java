package org.example.persistence;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.config.Config;
import org.example.model.StudentAwardRecord;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;

/**
 * newFile 与 SQLite 管理
 */
public class NewDataManager {
    private static final Logger LOGGER = LoggerUtil.getLogger(NewDataManager.class.getName());
    private static final String[] HEADER = buildHeader();
    private final Map<Long, StudentAwardRecord> recordMap = new HashMap<>();
    private final File excelFile;
    private final File dbFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public NewDataManager(String excelPath, String dbPath) {
        this.excelFile = new File(excelPath);
        this.dbFile = new File(dbPath);
        initDb(dbPath);
        if (!excelFile.exists()) saveAll();
    }

    private static String[] buildHeader() {
        String[] h = new String[6];
        h[0] = Config.COL_STUDENT_ID;
        h[1] = Config.COL_NAME;
        h[2] = Config.COL_CLASS;
        h[3] = Config.COL_CERT_TOTAL;
        h[4] = Config.COL_AWARD_TOTAL;
        h[5] = Config.COL_RECORDED_COUNT;
        // The 6th column will be for the awards JSON blob
        h[5] = "awards_json";
        return h;
    }

    public StudentAwardRecord getOrCreateRecord(long studentId, String name, String className) {
        return recordMap.computeIfAbsent(studentId, id -> new StudentAwardRecord(id, name, className));
    }

    public StudentAwardRecord getRecord(long studentId) {
        return recordMap.get(studentId);
    }

    public void persistRecord(StudentAwardRecord record) {
        // 写数据库 + 写 Excel 行，保证界面操作立即落盘
        writeDb(record);
        flushRecordToExcel(record);
    }

    public void saveAll() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(Config.SHEET_MAIN);
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue(Config.COL_STUDENT_ID);
            headerRow.createCell(1).setCellValue(Config.COL_NAME);
            headerRow.createCell(2).setCellValue(Config.COL_CLASS);
            headerRow.createCell(3).setCellValue(Config.COL_CERT_TOTAL);
            headerRow.createCell(4).setCellValue(Config.COL_AWARD_TOTAL);
            headerRow.createCell(5).setCellValue(Config.COL_RECORDED_COUNT);
            headerRow.createCell(6).setCellValue("awards_json");

            int rowIndex = 1;
            for (StudentAwardRecord r : recordMap.values()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(r.getStudentId());
                row.createCell(1).setCellValue(r.getName());
                row.createCell(2).setCellValue(r.getClassName());
                row.createCell(3).setCellValue(r.getCertTotalPoints());
                row.createCell(4).setCellValue(r.getAwardTotalPoints());
                row.createCell(5).setCellValue(r.getRecordedAwardCount());
                row.createCell(6).setCellValue(mapper.writeValueAsString(r.getAwards()));
            }
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "写入 Excel 失败");
        }
    }

    private void initDb(String dbPath) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS students (student_id INTEGER PRIMARY KEY, name TEXT, class_name TEXT, cert_total_points REAL DEFAULT 0.0, award_total_points REAL DEFAULT 0.0, recorded_award_count INTEGER DEFAULT 0, awards_json TEXT)");
            st.execute("DROP TABLE IF EXISTS award_labels"); // Drop obsolete table
            st.execute("DROP TABLE IF EXISTS student_history"); // Drop obsolete table
            st.execute("DROP VIEW IF EXISTS v_student_points"); // Drop obsolete view

            // Check and add columns if they don't exist (for migration)
            addColumnIfNotExists(st, "students", "name", "TEXT");
            addColumnIfNotExists(st, "students", "class_name", "TEXT");
            addColumnIfNotExists(st, "students", "awards_json", "TEXT");

        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化数据库失败");
        }
    }

    private void addColumnIfNotExists(Statement st, String tableName, String columnName, String columnType) {
        try {
            st.executeQuery("SELECT " + columnName + " FROM " + tableName + " LIMIT 1");
        } catch (Exception e) {
            try {
                st.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            } catch (Exception ex) {
                // Ignore
            }
        }
    }


    // 公开 writeDb 以支持批量重建使用
    public void writeDb(StudentAwardRecord r) {
        String sql = "INSERT INTO students (student_id, name, class_name, cert_total_points, award_total_points, recorded_award_count, awards_json) VALUES (?,?,?,?,?,?,?) ON CONFLICT(student_id) DO UPDATE SET name=excluded.name, class_name=excluded.class_name, cert_total_points=excluded.cert_total_points, award_total_points=excluded.award_total_points, recorded_award_count=excluded.recorded_award_count, awards_json=excluded.awards_json";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath()); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, r.getStudentId());
            ps.setString(2, r.getName());
            ps.setString(3, r.getClassName());
            ps.setDouble(4, r.getCertTotalPoints());
            ps.setDouble(5, r.getAwardTotalPoints());
            ps.setInt(6, r.getRecordedAwardCount());
            ps.setString(7, mapper.writeValueAsString(r.getAwards()));
            ps.executeUpdate();
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "写入数据库失败");
        }
    }

    public void reloadFromExcel() {
        if (!excelFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(excelFile); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet(Config.SHEET_MAIN);
            if (sheet == null) return;
            Row header = sheet.getRow(0);
            if (header == null) return;
            recordMap.clear();
            int colId = 0, colName = 1, colClass = 2, colCert = 3, colAward = 4, colCount = 5, colJson = 6;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cId = row.getCell(colId);
                if (cId == null) continue;
                long sid = (long) cId.getNumericCellValue();
                String name = getString(row.getCell(colName));
                String clazz = getString(row.getCell(colClass));
                StudentAwardRecord rec = new StudentAwardRecord(sid, name, clazz);

                rec.setCertTotalPoints(getNumeric(row.getCell(colCert)));
                rec.setAwardTotalPoints(getNumeric(row.getCell(colAward)));
                rec.setRecordedAwardCount((int) getNumeric(row.getCell(colCount)));

                String json = getString(row.getCell(colJson));
                if (json != null && !json.isEmpty()) {
                    List<Map<String, String>> awards = mapper.readValue(json, new TypeReference<>() {});
                    awards.forEach(award -> rec.addAward(award.get("name"), award.get("image"), award.get("category")));
                }
                recordMap.put(sid, rec);
            }
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "重新加载 Excel 失败");
        }
    }

    private String getString(Cell c) {
        return c == null ? "" : c.getCellType() == CellType.STRING ? c.getStringCellValue() : c.toString();
    }

    private double getNumeric(Cell c) {
        return c == null || c.getCellType() != CellType.NUMERIC ? 0.0 : c.getNumericCellValue();
    }

    private synchronized void flushRecordToExcel(StudentAwardRecord r) {
        try {
            if (!excelFile.exists()) { saveAll(); return; }
            try (FileInputStream fis = new FileInputStream(excelFile); Workbook wb = new XSSFWorkbook(fis)) {
                Sheet sheet = wb.getSheet(org.example.config.Config.SHEET_MAIN);
                if (sheet == null) { saveAll(); return; }
                int idCol = 0; int targetRowIndex = -1;
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i); if (row == null) continue;
                    Cell c = row.getCell(idCol);
                    if (c != null && c.getCellType() == CellType.NUMERIC && (long)c.getNumericCellValue() == r.getStudentId()) { targetRowIndex = i; break; }
                }
                Row row = (targetRowIndex == -1) ? sheet.createRow(sheet.getLastRowNum() + 1) : sheet.getRow(targetRowIndex);
                row.createCell(0).setCellValue(r.getStudentId());
                row.createCell(1).setCellValue(r.getName());
                row.createCell(2).setCellValue(r.getClassName());
                row.createCell(3).setCellValue(r.getCertTotalPoints());
                row.createCell(4).setCellValue(r.getAwardTotalPoints());
                row.createCell(5).setCellValue(r.getRecordedAwardCount());
                row.createCell(6).setCellValue(mapper.writeValueAsString(r.getAwards()));

                try (FileOutputStream fos = new FileOutputStream(excelFile)) { wb.write(fos); }
            }
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "刷新 Excel 行失败");
        }
    }

    public Collection<StudentAwardRecord> getAllRecords() {
        return recordMap.values();
    }

    public synchronized void importRecords(Collection<StudentAwardRecord> records, boolean overwriteExisting) {
        if (overwriteExisting) recordMap.clear();
        for (StudentAwardRecord r : records) {
            recordMap.put(r.getStudentId(), r);
            // 直接持久化（写 DB + Excel）
            persistRecord(r);
        }
    }

    // ================= 新增：为快照重建提供的辅助方法 =================

    public File getExcelFile() { return excelFile; }
    public File getDbFile() { return dbFile; }

    /**
     * 清空内存 + 删除现有文件并重新初始化数据库（不生成 Excel，调用者再写入）。
     */
    public synchronized void clearAndRecreateStorage() {
        recordMap.clear();
        if (excelFile.exists() && !excelFile.delete()) {
            LOGGER.warn("Excel 文件删除失败: " + excelFile.getAbsolutePath());
        }
        if (dbFile.exists() && !dbFile.delete()) {
            LOGGER.warn("数据库文件删除失败: " + dbFile.getAbsolutePath());
        }
        initDb(dbFile.getPath());
    }

    /**
     * 批量加载记录（仅写入 DB，最后一次性写 Excel），用于快照导入效率。现有内容应已通过 clearAndRecreateStorage 清空。
     */
    public synchronized void bulkLoadRecords(Collection<StudentAwardRecord> records) {
        for (StudentAwardRecord r : records) {
            recordMap.put(r.getStudentId(), r);
            writeDb(r);
        }
        saveAll();
    }
}
