package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责数据源选择后的初始化：模板初始化 + 学生列表加载 + NewDataManager 创建 + 记录加载 + 启动检查。
 * 原 App 中的 initializeDataPipelinePostChoice 与 loadStudentsFromSummary 合并。
 */
public class StartupService {
    private static final Logger LOGGER = LoggerUtil.getLogger(StartupService.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    public StartupResult initialize(DataLoader.Choice choice) {
        if (choice == DataLoader.Choice.RANDOM) {
            new BuildList().build(Config.SOURCE_PATH);
        } else if (choice == DataLoader.Choice.NEW) {
            // 已在 DataLoader 生成汇总
        } else if (choice == DataLoader.Choice.LAST) {
            // 使用现有 Build_Summary.xlsx
        }
        TemplateInitializer.initializeTemplate();
        List<Student> students = loadStudentsFromSummary();
        NewDataManager manager = new NewDataManager(Config.NEW_FILE_PATH, Config.DB_PATH);
        manager.reloadFromExcel();
        StartupIntegrityChecker.runAll(manager, students);
        return new StartupResult(students, manager);
    }

    private List<Student> loadStudentsFromSummary() {
        List<Student> list = new ArrayList<>();
        File f = new File(Config.FILE_PATH);
        if (!f.exists()) {
            LOGGER.warn("汇总文件不存在: " + Config.FILE_PATH);
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
            if (colStudentId < 0 || colName < 0 || colClazz < 0 || colAwards < 0) {
                LOGGER.error("列索引识别失败");
                return list;
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String sidStr = getCellString(row.getCell(colStudentId));
                String name = getCellString(row.getCell(colName));
                String clazz = getCellString(row.getCell(colClazz));
                String awardsJson = getCellString(row.getCell(colAwards));
                if (sidStr == null || sidStr.isEmpty()) continue;
                long sid;
                try { sid = Long.parseLong(sidStr); } catch (NumberFormatException ex) { continue; }
                List<Award> awardList = parseAwards(awardsJson);
                list.add(new Student(sid, name == null ? "" : name, clazz == null ? "" : clazz, awardList));
            }
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "加载学生失败");
        }
        return list;
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
            LoggerUtil.logException(LOGGER, e, "解析奖项 JSON 失败: " + json);
        }
        return list;
    }

    public static class StartupResult {
        public final List<Student> students;
        public final NewDataManager manager;
        public StartupResult(List<Student> students, NewDataManager manager) {
            this.students = students;
            this.manager = manager;
        }
    }
}

