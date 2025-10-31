package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * 迁移自 file_operations.py
 * 提供与原 Python 等价的三个函数：
 *  - loadData(filePath): 读取汇总文件，解析“奖项”列 JSON 字符串为 List<Award>，同时计算已录入奖项数
 *  - loadNewData(filePath): 读取 newFile.xlsx，确保奖项1..50列存在并以字符串回填，返回行集合
 *  - saveNewData(rows, filePath): 将 newFile 数据写回 Excel
 * 一比一复原，无 TODO。
 */
public class FileOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileOperations.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 表示 newFile 中的一行 */
    public static class NewDataRow {
        public long studentId;
        public String name;
        public String clazz;
        public double certTotalPoints;
        public double awardTotalPoints;
        public int recordedAwardCount;
        public String[] awardLabels = new String[50];
    }

    /** 读取汇总文件（Build_Summary.xlsx） */
    public static List<Student> loadData(String filePath) {
        List<Student> students = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) {
            LOGGER.warn("文件不存在: " + filePath);
            return students;
        }
        try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) return students;
            Row header = sheet.getRow(0);
            if (header == null) return students;
            int colStudentId = findColumnIndex(header, "学号");
            int colName = findColumnIndex(header, "姓名");
            int colClazz = findColumnIndex(header, "班级");
            int colAwards = findColumnIndex(header, "奖项");
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String sidStr = getCellString(row.getCell(colStudentId));
                if (sidStr == null || sidStr.isEmpty()) continue;
                long sid;
                try { sid = Long.parseLong(sidStr); } catch (NumberFormatException ex) { continue; }
                String name = getCellString(row.getCell(colName));
                String clazz = getCellString(row.getCell(colClazz));
                String awardsJson = getCellString(row.getCell(colAwards));
                List<Award> awards = parseAwards(awardsJson);
                students.add(new Student(sid, name == null ? "" : name, clazz == null ? "" : clazz, awards));
            }
        } catch (Exception e) {
            LOGGER.error("读取汇总文件失败", e);
        }
        return students;
    }

    /** 读取 newFile.xlsx 并确保奖项列存在 */
    public static List<NewDataRow> loadNewData(String filePath) {
        List<NewDataRow> rows = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) {
            LOGGER.warn("newFile 不存在: " + filePath);
            return rows;
        }
        try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) return rows;
            Row header = sheet.getRow(0);
            if (header == null) return rows;
            // 预期列：学号, 姓名, 班级, 证书总分, 奖项总分, 已录入奖项数, 奖项1..奖项50
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cId = row.getCell(0);
                if (cId == null) continue;
                NewDataRow nd = new NewDataRow();
                if (cId.getCellType() == CellType.NUMERIC) {
                    nd.studentId = (long) cId.getNumericCellValue();
                } else {
                    String idStr = getCellString(cId);
                    try { nd.studentId = Long.parseLong(idStr); } catch (Exception ex) { continue; }
                }
                nd.name = getCellString(row.getCell(1));
                nd.clazz = getCellString(row.getCell(2));
                nd.certTotalPoints = getNumeric(row.getCell(3));
                nd.awardTotalPoints = getNumeric(row.getCell(4));
                nd.recordedAwardCount = (int) getNumeric(row.getCell(5));
                for (int i = 0; i < 50; i++) {
                    Cell c = row.getCell(6 + i);
                    nd.awardLabels[i] = c == null ? "" : getCellString(c);
                }
                rows.add(nd);
            }
        } catch (Exception e) {
            LOGGER.error("读取 newFile 失败", e);
        }
        return rows;
    }

    /** 写回 newFile.xlsx */
    public static void saveNewData(List<NewDataRow> rows, String filePath) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("班级");
            header.createCell(3).setCellValue("证书总分");
            header.createCell(4).setCellValue("奖项总分");
            header.createCell(5).setCellValue("已录入奖项数");
            for (int i = 0; i < 50; i++) {
                header.createCell(6 + i).setCellValue("奖项" + (i + 1));
            }
            int r = 1;
            for (NewDataRow nd : rows) {
                Row row = sheet.createRow(r++);
                int col = 0;
                row.createCell(col++).setCellValue(nd.studentId);
                row.createCell(col++).setCellValue(nd.name == null ? "" : nd.name);
                row.createCell(col++).setCellValue(nd.clazz == null ? "" : nd.clazz);
                row.createCell(col++).setCellValue(nd.certTotalPoints);
                row.createCell(col++).setCellValue(nd.awardTotalPoints);
                row.createCell(col++).setCellValue(nd.recordedAwardCount);
                for (String label : nd.awardLabels) {
                    row.createCell(col++).setCellValue(label == null ? "" : label);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        } catch (Exception e) {
            LOGGER.error("写入 newFile 失败", e);
        }
    }

    private static int findColumnIndex(Row header, String name) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell != null && name.equals(cell.getStringCellValue())) return i;
        }
        return -1;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == (long) d) return String.valueOf((long) d);
                return String.valueOf(d);
            case BLANK: return "";
            default: return cell.toString();
        }
    }

    private static double getNumeric(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String s = getCellString(cell);
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private static List<Award> parseAwards(String json) {
        List<Award> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JsonNode arr = MAPPER.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String name = node.has("奖项") ? node.get("奖项").asText() : "";
                    String img = node.has("证书图片") ? node.get("证书图片").asText() : "";
                    list.add(new Award(name, img));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("解析奖项列失败: " + json, e);
        }
        return list;
    }
}

