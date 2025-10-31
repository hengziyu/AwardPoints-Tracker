package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 迁移自 Python build_list.py
 * 功能：
 *  - 读取源 Excel (path_to_your_file.xlsx)
 *  - 抽取学号/姓名/班级及奖项与图片列
 *  - 合并每行奖项为 JSON 数组字符串列 "奖项"
 *  - 按学号合并（奖项累加）
 *  - 计算总奖项数列 "总奖项数"
 *  - 写出 Build_Summary.xlsx
 * 一比一复原逻辑，无 TODO。
 */
public class BuildList {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildList.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /** 构建汇总文件 */
    public void build(String sourcePath) {
        File src = new File(sourcePath);
        if (!src.exists()) {
            LOGGER.error("源文件不存在: " + sourcePath);
            return;
        }
        try (FileInputStream fis = new FileInputStream(src); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) {
                LOGGER.error("未找到 Sheet1 工作表");
                return;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.error("缺少表头行");
                return;
            }

            // 收集列索引
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String val = cell.getStringCellValue();
                    headerIndex.put(val, i);
                }
            }

            // 识别基础列 (允许不同命名前缀) - 优先精确匹配再模糊
            Integer idxStudentId = findColumn(headerIndex, Arrays.asList("3、学号", "学号"));
            Integer idxName = findColumn(headerIndex, Arrays.asList("1、姓名", "姓名"));
            Integer idxClass = findColumn(headerIndex, Arrays.asList("2、班级", "班级"));

            if (idxStudentId == null || idxName == null || idxClass == null) {
                LOGGER.warn("基础列缺失，尝试按位置回退 (7=姓名,8=班级,9=学号)");
                // 与 BuildNull 的写入位置对应
                idxName = idxName == null ? 6 : idxName;    // 1-based 第7列 -> index 6
                idxClass = idxClass == null ? 7 : idxClass; // 第8列 -> index 7
                idxStudentId = idxStudentId == null ? 8 : idxStudentId; // 第9列 -> index 8
            }

            // 找出奖项与图片列 (按模板：奖项N 与 证书图片N 或 "请上传证书图片：")
            // 假设顺序为: 奖项1, 图片1, 奖项2, 图片2 ... 从第10列开始 (index 9)
            int startAwardIndex = 9; // index 基于 BuildNull 奖项起始列
            int maxColumns = headerRow.getLastCellNum();
            List<Integer> awardNameCols = new ArrayList<>();
            List<Integer> awardImageCols = new ArrayList<>();
            for (int i = startAwardIndex; i < maxColumns; i += 2) {
                awardNameCols.add(i);
                if (i + 1 < maxColumns) {
                    awardImageCols.add(i + 1);
                }
            }

            // 合并行数据
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
                    if ((awardName == null || awardName.isEmpty()) && (imgUrl == null || imgUrl.isEmpty())) {
                        continue;
                    }
                    if (awardName == null || awardName.isEmpty()) awardName = "无名字";
                    if (imgUrl == null || imgUrl.isEmpty()) imgUrl = "无图片";
                    agg.addAward(awardName, imgUrl);
                }
            }

            // 写出汇总文件
            writeSummary(new ArrayList<>(aggregateMap.values()));
            LOGGER.info("BuildList: 汇总完成 -> " + Config.FILE_PATH);
        } catch (Exception e) {
            LOGGER.error("生成过程中出错", e);
        }
    }

    private Integer findColumn(Map<String, Integer> headerIndex, List<String> candidates) {
        for (String c : candidates) {
            if (headerIndex.containsKey(c)) return headerIndex.get(c);
        }
        // 模糊匹配
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
            if (d == (long) d) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.BLANK) return "";
        return cell.toString().trim();
    }

    private void writeSummary(List<AggregatedStudent> students) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("班级");
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

            try (FileOutputStream fos = new FileOutputStream(Config.FILE_PATH)) {
                wb.write(fos);
            }
        }
    }

    // 辅助内部结构（包级访问）
    static class AwardPair {
        final String award;
        final String image;
        AwardPair(String a, String i) { this.award = a; this.image = i; }
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
        void addAward(String a, String i) { awards.add(new AwardPair(a, i)); }
    }
}


