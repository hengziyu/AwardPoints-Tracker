package org.example.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 汇总构建 (逻辑未改)
 */
public class BuildList {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildList.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public void build(String sourcePath) {
        File src = new File(sourcePath);
        if (!src.exists()) {
            LOGGER.error("源文件不存在: " + sourcePath);
            return;
        }
        try (FileInputStream fis = new FileInputStream(src); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet(Config.SHEET_MAIN);
            if (sheet == null) {
                LOGGER.error("未找到 " + Config.SHEET_MAIN + " 工作表");
                return;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.error("缺少表头行");
                return;
            }
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String val = cell.getStringCellValue();
                    headerIndex.put(val, i);
                }
            }
            Integer idxStudentId = findColumn(headerIndex, Arrays.asList("3、学号", "学号"));
            Integer idxName = findColumn(headerIndex, Arrays.asList("1、姓名", "姓名"));
            Integer idxClass = findColumn(headerIndex, Arrays.asList("2、班级", "班级"));
            if (idxStudentId == null || idxName == null || idxClass == null) {
                LOGGER.warn("基础列缺失, 退回位置 (7=姓名,8=班级,9=学号)");
                idxName = idxName == null ? 6 : idxName;
                idxClass = idxClass == null ? 7 : idxClass;
                idxStudentId = idxStudentId == null ? 8 : idxStudentId;
            }
            int startAwardIndex = 9;
            int maxColumns = headerRow.getLastCellNum();
            List<Integer> awardNameCols = new ArrayList<>();
            List<Integer> awardImageCols = new ArrayList<>();
            for (int i = startAwardIndex; i < maxColumns; i += 2) {
                awardNameCols.add(i);
                if (i + 1 < maxColumns) awardImageCols.add(i + 1);
            }
            Map<String, AggregatedStudent> aggregateMap = new LinkedHashMap<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String studentId = getCellString(row.getCell(idxStudentId));
                String name = getCellString(row.getCell(idxName));
                String clazz = getCellString(row.getCell(idxClass));
                if (studentId == null || studentId.isEmpty()) continue;
                AggregatedStudent agg = aggregateMap.computeIfAbsent(studentId, sid -> new AggregatedStudent(sid, name, clazz));
                for (int i = 0; i < awardNameCols.size(); i++) {
                    int colAward = awardNameCols.get(i);
                    int colImg = i < awardImageCols.size() ? awardImageCols.get(i) : -1;
                    String awardName = getCellString(row.getCell(colAward));
                    String imgUrl = colImg >= 0 ? getCellString(row.getCell(colImg)) : null;
                    if ((awardName == null || awardName.isEmpty()) && (imgUrl == null || imgUrl.isEmpty())) continue;
                    if (awardName == null || awardName.isEmpty()) awardName = "无名字";
                    if (imgUrl == null || imgUrl.isEmpty()) imgUrl = "无图片";
                    agg.addAward(awardName, imgUrl);
                }
            }
            writeSummary(new ArrayList<>(aggregateMap.values()));
            LOGGER.info("BuildList: 汇总完成 -> " + Config.NULL_TEMPLATE_FILE);
        } catch (Exception e) {
            LOGGER.error("生成过程中出错", e);
        }
    }

    private Integer findColumn(Map<String, Integer> headerIndex, List<String> candidates) {
        for (String c : candidates) {
            if (headerIndex.containsKey(c)) return headerIndex.get(c);
        }
        for (Map.Entry<String, Integer> e : headerIndex.entrySet()) {
            for (String c : candidates) {
                if (e.getKey().contains(c)) return e.getValue();
            }
        }
        return null;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
        }
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.BLANK) return "";
        return cell.toString().trim();
    }

    private void writeSummary(List<AggregatedStudent> students) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(Config.SHEET_MAIN);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(Config.COL_STUDENT_ID);
            header.createCell(1).setCellValue(Config.COL_NAME);
            header.createCell(2).setCellValue(Config.COL_CLASS);
            header.createCell(3).setCellValue("奖项");
            header.createCell(4).setCellValue("总奖项数");
            int r = 1;
            for (AggregatedStudent s : students) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.studentId);
                row.createCell(1).setCellValue(s.name);
                row.createCell(2).setCellValue(s.clazz);
                ArrayNode arr = mapper.createArrayNode();
                for (AwardPair ap : s.awards) {
                    ObjectNode node = mapper.createObjectNode();
                    node.put("奖项", ap.award);
                    node.put("证书图片", ap.image);
                    arr.add(node);
                }
                row.createCell(3).setCellValue(arr.toString());
                row.createCell(4).setCellValue(s.awards.size());
            }
            try (FileOutputStream fos = new FileOutputStream(Config.AWARDS_SUMMARY_PATH)) {
                wb.write(fos);
            }
        }
    }

    static class AwardPair {
        final String award;
        final String image;

        AwardPair(String a, String i) {
            award = a;
            image = i;
        }
    }

    static class AggregatedStudent {
        final String studentId;
        final String name;
        final String clazz;
        final List<AwardPair> awards = new ArrayList<>();

        AggregatedStudent(String sid, String name, String clazz) {
            this.studentId = sid;
            this.name = name;
            this.clazz = clazz;
        }

        void addAward(String a, String i) {
            awards.add(new AwardPair(a, i));
        }
    }
}
