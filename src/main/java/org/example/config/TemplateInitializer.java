package org.example.config;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 模板初始化
 */
public final class TemplateInitializer {
    private static final Logger LOGGER = LoggerUtil.getLogger(TemplateInitializer.class.getName());

    private TemplateInitializer() {
    }

    public static void initializeTemplate() {
        ensureNewFile();
        ensureSummaryFile();
    }

    private static void ensureNewFile() {
        File f = new File(Config.STUDENT_AWARDS_PATH);
        if (f.exists()) return;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(Config.SHEET_MAIN);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(Config.COL_STUDENT_ID);
            header.createCell(1).setCellValue(Config.COL_NAME);
            header.createCell(2).setCellValue(Config.COL_CLASS);
            header.createCell(3).setCellValue(Config.COL_CERT_TOTAL);
            header.createCell(4).setCellValue(Config.COL_AWARD_TOTAL);
            header.createCell(5).setCellValue(Config.COL_RECORDED_COUNT);
            for (int i = 0; i < 50; i++) header.createCell(6 + i).setCellValue(Config.COL_AWARD_LABEL_PREFIX + (i + 1));
            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }
            LOGGER.info("初始化模板成功");
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化模板失败");
        }
    }

    private static void ensureSummaryFile() {
        File f = new File(Config.AWARDS_SUMMARY_PATH);
        if (f.exists()) return;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(Config.SHEET_MAIN);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(Config.COL_STUDENT_ID);
            header.createCell(1).setCellValue(Config.COL_NAME);
            header.createCell(2).setCellValue(Config.COL_CLASS);
            header.createCell(3).setCellValue("奖项");
            header.createCell(4).setCellValue("总奖项数");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }
            LOGGER.info("初始化模板成功");
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化模板失败");
        }
    }
}
