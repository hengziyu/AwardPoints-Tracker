package org.example.build;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.config.Config;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机数据构建 (逻辑未改)
 */
public class BuildNull {
    private static final Logger LOGGER = LoggerUtil.getLogger(BuildNull.class.getName());
    private static final int QUANTITY = Config.QUANTITY;
    private static final int MAX_AWARDS = Config.MAX_AWARDS;
    private static final boolean RANDOM_AWARDS_COUNT = Config.RANDOM_AWARDS_COUNT;
    private static final String NULL_FILE = Config.NULL_TEMPLATE_FILE;
    private static final String PATH_FILE = Config.RAW_SOURCE_PATH;
    private final Random random = new Random();
    private int dataRowOffset = 0;
    private Workbook workbook;
    private Sheet sheet;

    public static void main(String[] args) {
        new BuildNull().build();
    }

    private void detectHeaderOffset() {
        Row r0 = sheet.getRow(0);
        if (r0 == null) {
            dataRowOffset = 0;
            return;
        }
        String c7 = getCellString(r0.getCell(6));
        String c8 = getCellString(r0.getCell(7));
        String c9 = getCellString(r0.getCell(8));
        if ((c7 != null && c7.contains("姓名")) || (c8 != null && c8.contains("班")) || (c9 != null && c9.contains("学号"))) {
            dataRowOffset = 1;
        } else dataRowOffset = 0;
        LOGGER.debug("dataRowOffset=" + dataRowOffset);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
            default:
                return cell.toString();
        }
    }

    public void build() {
        try {
            File target = new File(PATH_FILE);
            if (!target.exists()) {
                copyTemplateFresh();
            } else {
                if (!canOpenWorkbook(target)) {
                    LOGGER.warn("目标文件已损坏, 删除并重新复制模板: " + PATH_FILE);
                    if (!target.delete()) LOGGER.warn("无法删除损坏文件, 尝试覆盖写入");
                    copyTemplateFresh();
                }
            }
            try (FileInputStream fis = new FileInputStream(PATH_FILE)) {
                workbook = WorkbookFactory.create(fis);
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet(Config.SHEET_MAIN);
            } catch (Exception ex) {
                LoggerUtil.logException(LOGGER, ex, "复制后仍无法打开, 回退创建新工作簿");
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(Config.SHEET_MAIN);
                ensureHeader();
            }
            ensureHeader();
            detectHeaderOffset();
            for (int step = 1; step <= 4; step++) {
                run(step);
            }
            try (FileOutputStream fos = new FileOutputStream(PATH_FILE)) {
                workbook.write(fos);
            }
            workbook.close();
            LOGGER.info("构建完成: " + PATH_FILE);
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "构建流程失败");
        }
    }

    private void copyTemplateFresh() throws IOException {
        File template = new File(NULL_FILE);
        if (!template.exists()) {
            LOGGER.warn("模板文件缺失, 创建空工作簿替代。");
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet(Config.SHEET_MAIN);
            ensureHeader();
            try (FileOutputStream fos = new FileOutputStream(PATH_FILE)) {
                workbook.write(fos);
            }
            workbook.close();
            return;
        }
        Files.copy(Path.of(NULL_FILE), Path.of(PATH_FILE), StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("模板复制完成: " + PATH_FILE);
    }

    private boolean canOpenWorkbook(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            Workbook wb = WorkbookFactory.create(fis);
            wb.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void ensureHeader() {
        if (sheet == null) return;
        Row r0 = sheet.getRow(0);
        if (r0 != null) return;
        r0 = sheet.createRow(0);
        r0.createCell(6).setCellValue("姓名");
        r0.createCell(7).setCellValue("班级");
        r0.createCell(8).setCellValue("学号");
        int base = 9;
        for (int i = 1; i <= 40; i++) {
            r0.createCell(base).setCellValue("奖项" + i);
            r0.createCell(base + 1).setCellValue("证书图片" + i);
            base += 2;
        }
    }

    private String generateRandomChineseName() {
        String[] first = Config.RANDOM_SURNAME_POOL;
        String[] last = Config.RANDOM_GIVEN_NAME_POOL;
        return first[random.nextInt(first.length)] + last[random.nextInt(last.length)];
    }

    private String generateRandomChineseClass() {
        String[] prefixes = Config.RANDOM_CLASS_PREFIX_POOL;
        return prefixes[random.nextInt(prefixes.length)] + (Config.CLASS_SUFFIX_MIN + random.nextInt(Config.CLASS_SUFFIX_MAX - Config.CLASS_SUFFIX_MIN + 1));
    }

    private String generateRandomChineseNumber() {
        long span = Config.STUDENT_ID_MAX - Config.STUDENT_ID_MIN + 1;
        long val = Config.STUDENT_ID_MIN + (Math.abs(random.nextLong()) % span);
        return String.valueOf(val);
    }

    private String generateRandomChineseAward() {
        String[] awardList = Config.RANDOM_AWARD_NAME_POOL;
        String[] gradeList = Config.RANDOM_AWARD_GRADE_POOL;
        return awardList[random.nextInt(awardList.length)] + gradeList[random.nextInt(gradeList.length)];
    }

    private String generateRandomChineseAwardImg() {
        String[] img = Config.RANDOM_IMAGE_POOL;
        return img[random.nextInt(img.length)];
    }

    private List<List<String>> getAwardList() {
        int n = RANDOM_AWARDS_COUNT ? (1 + random.nextInt(MAX_AWARDS)) : MAX_AWARDS;
        List<List<String>> awards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<String> pair = new ArrayList<>(2);
            pair.add(generateRandomChineseAward());
            pair.add(generateRandomChineseAwardImg());
            awards.add(pair);
        }
        return awards;
    }

    private void coordinateWriting(int index, int G, String value) {
        if (sheet == null) return;
        int rowIdx = (index - 1) + dataRowOffset;
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        int colIdx = G - 1;
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value);
    }

    private void awardWriting(int index, int param, List<List<String>> value) {
        if (sheet == null) return;
        int rowIdx = (index - 1) + dataRowOffset;
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        int col = param - 1;
        for (List<String> pair : value) {
            Cell awardCell = row.getCell(col);
            if (awardCell == null) awardCell = row.createCell(col);
            awardCell.setCellValue(pair.get(0));
            Cell imgCell = row.getCell(col + 1);
            if (imgCell == null) imgCell = row.createCell(col + 1);
            imgCell.setCellValue(pair.get(1));
            col += 2;
        }
    }

    private void run(int step) {
        if (step == 1) {
            for (int i = 1; i <= QUANTITY; i++) coordinateWriting(i, 7, generateRandomChineseName());
        } else if (step == 2) {
            for (int i = 1; i <= QUANTITY; i++) coordinateWriting(i, 8, generateRandomChineseClass());
        } else if (step == 3) {
            for (int i = 1; i <= QUANTITY; i++) coordinateWriting(i, 9, generateRandomChineseNumber());
        } else if (step == 4) {
            for (int i = 1; i <= QUANTITY; i++) awardWriting(i, 10, getAwardList());
        }
    }

    private void saveWorkbook() {
        try (FileOutputStream fos = new FileOutputStream(PATH_FILE)) {
            workbook.write(fos);
        } catch (IOException e) {
            LoggerUtil.logException(LOGGER, e, "保存工作簿失败");
        }
    }
}
