package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 迁移自 data_processing.py
 * 功能：
 *  - loadData(): 读取 Build_Summary.xlsx，解析 "奖项" JSON 字符串列为 List<Award>，计算 已录入奖项数
 *  - loadNewData(): 读取 newFile.xlsx，确保 奖项1..奖项50 列存在（不存在则补空字符串），返回记录 Map
 * 一比一复原语义，无 TODO。
 */
public class DataProcessing {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessing.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Student> loadData() {
        List<Student> list = new ArrayList<>();
        File f = new File(Config.FILE_PATH);
        if (!f.exists()) {
            LOGGER.warn("文件不存在: " + Config.FILE_PATH);
            return list;
        }
        try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) return list;
            Row header = sheet.getRow(0);
            if (header == null) return list;
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
                list.add(new Student(sid, name == null ? "" : name, clazz == null ? "" : clazz, awards));
            }
        } catch (Exception e) {
            LOGGER.error("读取数据失败", e);
        }
        return list;
    }

    public Map<Long, String[]> loadNewData() {
        Map<Long, String[]> map = new HashMap<>();
        File f = new File(Config.NEW_FILE_PATH);
        if (!f.exists()) {
            // 初始化空文件结构
            initNewFile();
        }
        try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) return map;
            Row header = sheet.getRow(0);
            if (header == null) return map;
            int colStudentId = findColumnIndex(header, "学号");
            // 奖项列 1..50
            int[] awardCols = new int[50];
            for (int i = 0; i < 50; i++) {
                awardCols[i] = findColumnIndex(header, "奖项" + (i + 1));
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String sidStr = getCellString(row.getCell(colStudentId));
                if (sidStr == null || sidStr.isEmpty()) continue;
                long sid;
                try { sid = Long.parseLong(sidStr); } catch (NumberFormatException ex) { continue; }
                String[] labels = new String[50];
                for (int i = 0; i < 50; i++) {
                    if (awardCols[i] >= 0) {
                        labels[i] = getCellString(row.getCell(awardCols[i]));
                        if (labels[i] == null) labels[i] = "";
                    } else {
                        labels[i] = "";
                    }
                }
                map.put(sid, labels);
            }
        } catch (Exception e) {
            LOGGER.error("读取 newFile 失败", e);
        }
        return map;
    }

    private void initNewFile() {
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
            try (FileOutputStream fos = new FileOutputStream(Config.NEW_FILE_PATH)) {
                wb.write(fos);
            }
        } catch (Exception e) {
            LOGGER.error("初始化 newFile 失败", e);
        }
    }

    private int findColumnIndex(Row header, String name) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell != null && name.equals(cell.getStringCellValue())) return i;
        }
        return -1;
    }

    private String getCellString(Cell cell) {
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

    private List<Award> parseAwards(String json) {
        List<Award> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JsonNode arr = mapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String name = node.has("奖项") ? node.get("奖项").asText() : "";
                    String img = node.has("证书图片") ? node.get("证书图片").asText() : "";
                    list.add(new Award(name, img));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("解析奖项失败: " + json, e);
        }
        return list;
    }
}

