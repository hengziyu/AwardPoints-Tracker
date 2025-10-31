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
import java.util.HashMap;
import java.util.Map;

/**
 * newFile 与 SQLite 管理
 */
public class NewDataManager {
    private static final Logger LOGGER = LoggerUtil.getLogger(NewDataManager.class.getName());
    private static final String[] HEADER = buildHeader();
    private final Map<Long, StudentAwardRecord> recordMap = new HashMap<>();
    private final File excelFile;
    private final File dbFile;

    public NewDataManager(String excelPath, String dbPath) {
        this.excelFile = new File(excelPath);
        this.dbFile = new File(dbPath);
        initDb(dbPath);
        if (!excelFile.exists()) saveAll();
    }

    private static String[] buildHeader() {
        String[] h = new String[6 + 50];
        h[0] = Config.COL_STUDENT_ID;
        h[1] = Config.COL_NAME;
        h[2] = Config.COL_CLASS;
        h[3] = Config.COL_CERT_TOTAL;
        h[4] = Config.COL_AWARD_TOTAL;
        h[5] = Config.COL_RECORDED_COUNT;
        for (int i = 0; i < 50; i++) h[6 + i] = Config.COL_AWARD_LABEL_PREFIX + (i + 1);
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
            for (int i = 0; i < HEADER.length; i++) headerRow.createCell(i).setCellValue(HEADER[i]);
            int rowIndex = 1;
            for (StudentAwardRecord r : recordMap.values()) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(r.getStudentId());
                row.createCell(col++).setCellValue(r.getName());
                row.createCell(col++).setCellValue(r.getClassName());
                row.createCell(col++).setCellValue(r.getCertTotalPoints());
                row.createCell(col++).setCellValue(r.getAwardTotalPoints());
                row.createCell(col++).setCellValue(r.getRecordedAwardCount());
                for (String label : r.getAwardLabels()) row.createCell(col++).setCellValue(label);
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
            st.execute("CREATE TABLE IF NOT EXISTS students (student_id INTEGER PRIMARY KEY, cert_total_points REAL DEFAULT 0.0, award_total_points REAL DEFAULT 0.0, recorded_award_count INTEGER DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS award_labels (student_id INTEGER, label_index INTEGER, label TEXT, PRIMARY KEY(student_id,label_index))");
            boolean hasColumn = false;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(students)")) {
                while (rs.next()) { if ("recorded_award_count".equalsIgnoreCase(rs.getString("name"))) { hasColumn = true; break; } }
            }
            if (!hasColumn) { try { st.execute("ALTER TABLE students ADD COLUMN recorded_award_count INTEGER DEFAULT 0"); } catch (Exception ignore) {} }
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化数据库失败");
        }
    }

    private void writeDb(StudentAwardRecord r) {
        String sql = "INSERT INTO students (student_id, cert_total_points, award_total_points, recorded_award_count) VALUES (?,?,?,?) ON CONFLICT(student_id) DO UPDATE SET cert_total_points=excluded.cert_total_points, award_total_points=excluded.award_total_points, recorded_award_count=excluded.recorded_award_count";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath()); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, r.getStudentId());
            ps.setDouble(2, r.getCertTotalPoints());
            ps.setDouble(3, r.getAwardTotalPoints());
            ps.setInt(4, r.getRecordedAwardCount());
            ps.executeUpdate();
            String labelSql = "INSERT INTO award_labels (student_id,label_index,label) VALUES (?,?,?) ON CONFLICT(student_id,label_index) DO UPDATE SET label=excluded.label";
            try (PreparedStatement lp = conn.prepareStatement(labelSql)) {
                String[] labels = r.getAwardLabels();
                for (int i = 0; i < labels.length; i++) {
                    lp.setLong(1, r.getStudentId());
                    lp.setInt(2, i);
                    lp.setString(3, labels[i] == null ? "" : labels[i]);
                    lp.addBatch();
                }
                lp.executeBatch();
            }
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
            int colId = 0, colName = 1, colClass = 2, colCert = 3, colAward = 4, colCount = 5;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cId = row.getCell(colId);
                if (cId == null) continue;
                long sid = (long) cId.getNumericCellValue();
                String name = getString(row.getCell(colName));
                String clazz = getString(row.getCell(colClass));
                double certPts = getNumeric(row.getCell(colCert));
                double awardPts = getNumeric(row.getCell(colAward));
                int recCount = (int) getNumeric(row.getCell(colCount));
                StudentAwardRecord rec = new StudentAwardRecord(sid, name, clazz);
                rec.addCertTotalPoints(certPts);
                rec.addAwardTotalPoints(awardPts);
                for (int i = 0; i < recCount; i++) rec.incrementRecordedAwardCount();
                for (int i = 0; i < 50; i++) {
                    Cell c = row.getCell(6 + i);
                    rec.setAwardLabel(i, c == null ? "" : c.getStringCellValue());
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
                int col = 0;
                row.createCell(col++).setCellValue(r.getStudentId());
                row.createCell(col++).setCellValue(r.getName());
                row.createCell(col++).setCellValue(r.getClassName());
                row.createCell(col++).setCellValue(r.getCertTotalPoints());
                row.createCell(col++).setCellValue(r.getAwardTotalPoints());
                row.createCell(col++).setCellValue(r.getRecordedAwardCount());
                String[] labels = r.getAwardLabels();
                for (int i = 0; i < labels.length; i++) {
                    Cell lc = row.getCell(col + i); if (lc == null) lc = row.createCell(col + i);
                    lc.setCellValue(labels[i] == null ? "" : labels[i]);
                }
                try (FileOutputStream fos = new FileOutputStream(excelFile)) { wb.write(fos); }
            }
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "刷新 Excel 行失败");
        }
    }
}
