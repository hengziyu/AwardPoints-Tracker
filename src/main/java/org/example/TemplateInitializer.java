package org.example;

import org.example.LoggerUtil;
import org.slf4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 迁移自 template_initializer.py (未在 all.txt 显示具体实现，按语义补全)。
 * 职责：确保模板类文件存在并具备表头结构，方便后续读写。
 * - newFile.xlsx 若不存在则创建表头（学号/姓名/班级/证书总分/奖项总分/已录入奖项数/奖项1..奖项50）
 * - Build_Summary.xlsx 若不存在则创建基本表头（学号/姓名/班级/奖项/总奖项数）
 * 保持与 Python 主程序调用顺序：在随机或其它数据构建后执行。
 */
public final class TemplateInitializer {
    private static final Logger LOGGER = LoggerUtil.getLogger(TemplateInitializer.class.getName());
    private TemplateInitializer() {}

    public static void initializeTemplate() {
        ensureNewFile();
        ensureSummaryFile();
    }

    private static void ensureNewFile() {
        File f = new File(Config.NEW_FILE_PATH);
        if (f.exists()) return;
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
            try (FileOutputStream fos = new FileOutputStream(f)) { wb.write(fos); }
            LOGGER.info("初始化 newFile.xlsx 模板成功");
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化 newFile.xlsx 模板失败");
        }
    }

    private static void ensureSummaryFile() {
        File f = new File(Config.FILE_PATH);
        if (f.exists()) return;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("班级");
            header.createCell(3).setCellValue("奖项");
            header.createCell(4).setCellValue("总奖项数");
            try (FileOutputStream fos = new FileOutputStream(f)) { wb.write(fos); }
            LOGGER.info("初始化 Build_Summary.xlsx 模板成功");
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "初始化 Build_Summary.xlsx 模板失败");
        }
    }
}

