package org.example;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.LoggerUtil;
import org.slf4j.Logger;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BuildNull: 迁移自 Python buid_null.py
 * 完整功能：
 * - 若目标文件不存在，则使用内嵌十六进制数据生成临时 Excel (null.xlsx)
 * - 读取临时 Excel，并按照 4 个步骤写入随机姓名、班级、学号与奖项列表（奖项与图片成对写入）
 * - 保存为 path_to_your_file.xlsx
 * - 删除临时文件
 * 一比一复原，无任何 TODO。
 */
public class BuildNull {

    private static final Logger LOGGER = LoggerUtil.getLogger(BuildNull.class.getName());

    // 对应 config 常量
    private static final int QUANTITY = 10; // 随机生成的人数
    private static final int MAX_AWARDS = 20; // 最大随机奖项数
    private static final boolean RANDOM_AWARDS_COUNT = true; // 是否在 [1, MAX_AWARDS] 范围内随机
    private int dataRowOffset = 0; // 根据表头检测调整
    // 文件路径（与 Python 保持一致语义）
    private static final String NULL_FILE = "null.xlsx";
    private static final String PATH_FILE = "path_to_your_file.xlsx";

    private Workbook workbook;
    private Sheet sheet;
    private final Random random = new Random();

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
        } else {
            dataRowOffset = 0;
        }
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


    /**
     * 主构建流程（支持嵌入数据为空时的回退）
     */
    public void build() {
        try {
            File target = new File(PATH_FILE);
            if (!target.exists()) {
                copyTemplateFresh();
            } else {
                if (!canOpenWorkbook(target)) {
                    LOGGER.warn("目标文件已损坏，删除并重新复制模板: " + PATH_FILE);
                    if (!target.delete()) {
                        LOGGER.warn("无法删除损坏文件，尝试覆盖写入。");
                    }
                    copyTemplateFresh();
                }
            } // 现在尝试正式打开
            try (FileInputStream fis = new FileInputStream(PATH_FILE)) {
                workbook = WorkbookFactory.create(fis);
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet("Sheet1");
            } catch (Exception ex) {
                LoggerUtil.logException(LOGGER, ex, "复制后仍无法打开，回退创建新工作簿");
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Sheet1");
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
            LOGGER.warn("模板文件缺失，创建空工作簿替代。");
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Sheet1");
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

    /**
     * 随机中文姓名
     */
    private String generateRandomChineseName() {
        String[] firstNames = {"王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴"};
        String[] lastNames = {"伟", "芳", "娜", "敏", "静", "秀英", "丽", "强", "磊", "军"};
        return firstNames[random.nextInt(firstNames.length)] + lastNames[random.nextInt(lastNames.length)];
    }

    /**
     * 随机中文班级
     */
    private String generateRandomChineseClass() {
        String[] classNames = {"网络", "物联网", "信安", "移动互联"};
        return classNames[random.nextInt(classNames.length)] + (1000 + random.nextInt(9000));
    }

    /**
     * 随机学号（10~11位之间）
     */
    private String generateRandomChineseNumber() {
        long min = (long) Math.pow(10, 9);
        long max = (long) Math.pow(10, 11) - 1;
        long val = min + (Math.abs(random.nextLong()) % (max - min + 1));
        return String.valueOf(val);
    }

    /**
     * 随机奖项名称
     */
    private String generateRandomChineseAward() {
        String[] awardList = {"码蹄杯", "蓝桥杯", "西门子杯", "网络安全", "全国职业技能大赛"};
        String[] gradeList = {"国奖", "省奖", "校奖", "市奖", "院奖"};
        return awardList[random.nextInt(awardList.length)] + gradeList[random.nextInt(gradeList.length)];
    }

    /**
     * 随机奖项图片 URL
     */
    private String generateRandomChineseAwardImg() {
        String[] imgList = {
                "https://pics0.baidu.com/feed/0e2442a7d933c8959b2c26032f0464fe82020019.jpeg",
                "https://pics4.baidu.com/feed/a2cc7cd98d1001e91284fb12d96e6ee255e797e5.jpeg",
                "https://pics6.baidu.com/feed/18d8bc3eb13533fa9cf6bda9c9b3e81140345b66.jpeg",
                "https://pics0.baidu.com/feed/0824ab18972bd4077a0bfd7819e98b5f0db309d0.jpeg"
        };
        return imgList[random.nextInt(imgList.length)];
    }

    /**
     * 生成一个人的奖项列表：[[奖项, 图片], ...]
     */
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

    /**
     * 在第 index 行第 G 列写入 value (1-based)
     */
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

    /**
     * 从 param 列开始，按 [奖项, 图片] 两列依次写入 value 列表
     */
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

    /**
     * 执行步骤
     */
    private void run(int step) {
        if (step == 1) {
            for (int i = 1; i <= QUANTITY; i++) {
                coordinateWriting(i, 7, generateRandomChineseName());
            }
        } else if (step == 2) {
            for (int i = 1; i <= QUANTITY; i++) {
                coordinateWriting(i, 8, generateRandomChineseClass());
            }
        } else if (step == 3) {
            for (int i = 1; i <= QUANTITY; i++) {
                coordinateWriting(i, 9, generateRandomChineseNumber());
            }
        } else if (step == 4) {
            for (int i = 1; i <= QUANTITY; i++) {
                awardWriting(i, 10, getAwardList());
            }
        }
    }

    /**
     * 保存当前工作簿到 PATH_FILE
     */
    private void saveWorkbook() {
        try (FileOutputStream fos = new FileOutputStream(PATH_FILE)) {
            workbook.write(fos);
        } catch (IOException e) {
            LoggerUtil.logException(LOGGER, e, "保存工作簿失败");
        }
    }


    /**
     * 入口，可独立运行
     */
    public static void main(String[] args) {
        new BuildNull().build();
    }
}
