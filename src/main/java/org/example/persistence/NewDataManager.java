package org.example.persistence;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.StudentAwardRecord;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
        h[0] = "学号";
        h[1] = "姓名";
        h[2] = "班级";
        h[3] = "证书总分";
        h[4] = "奖项总分";
        h[5] = "已录入奖项数";
        for (int i = 0; i < 50; i++) h[6 + i] = "奖项" + (i + 1);
        return h;
    }

    public StudentAwardRecord getOrCreateRecord(long studentId, String name, String className) {
        return recordMap.computeIfAbsent(studentId, id -> new StudentAwardRecord(id, name, className));
    }

    public StudentAwardRecord getRecord(long studentId) {
        return recordMap.get(studentId);
    }

    public void persistRecord(StudentAwardRecord record) {
        writeDb(record);
    }

    public void saveAll() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
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
            st.execute("CREATE TABLE IF NOT EXISTS students (student_id INTEGER PRIMARY KEY, cert_total_points REAL DEFAULT 0.0, award_total_points REAL DEFAULT 0.0) ");
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化数据库失败");
        }
    }

    private void writeDb(StudentAwardRecord r) {
        String sql = "INSERT INTO students (student_id, cert_total_points, award_total_points) VALUES (?,?,?) ON CONFLICT(student_id) DO UPDATE SET cert_total_points=excluded.cert_total_points, award_total_points=excluded.award_total_points";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath()); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, r.getStudentId());
            ps.setDouble(2, r.getCertTotalPoints());
            ps.setDouble(3, r.getAwardTotalPoints());
            ps.executeUpdate();
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "写入数据库失败");
        }
    }

    public void reloadFromExcel() {
        if (!excelFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(excelFile); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
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
}

