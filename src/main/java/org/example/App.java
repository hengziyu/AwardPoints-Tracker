package org.example;

import org.example.app.StartupService;
import org.example.build.BuildList;
import org.example.build.BuildNull;
import org.example.config.Config;
import org.example.model.Award;
import org.example.model.Student;
import org.example.model.StudentAwardRecord;
import org.example.persistence.NewDataManager;
import org.example.persistence.SnapshotManager;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;
import org.fusesource.jansi.AnsiConsole;
import static org.example.util.AnsiColors.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class App {

    private static final Logger LOGGER = LoggerUtil.getLogger(App.class.getName());
    private static NewDataManager dataManager;
    private static List<Student> studentList;
    private static final Scanner scanner = new Scanner(System.in);
    private static final Map<String, Double> SCORE_MAP = Map.of(
            Config.CATEGORY_CERT, Config.SCORE_CERT,
            Config.CATEGORY_NATIONAL, Config.SCORE_NATIONAL,
            Config.CATEGORY_PROVINCE_CITY, Config.SCORE_PROVINCE_CITY,
            Config.CATEGORY_SCHOOL, Config.SCORE_SCHOOL,
            Config.CATEGORY_COLLEGE, Config.SCORE_COLLEGE,
            Config.CATEGORY_NONE, Config.SCORE_NONE
    );
    private static final List<String> CATEGORY_KEYS = List.of(
            Config.CATEGORY_CERT,
            Config.CATEGORY_NATIONAL,
            Config.CATEGORY_PROVINCE_CITY,
            Config.CATEGORY_SCHOOL,
            Config.CATEGORY_COLLEGE,
            Config.CATEGORY_NONE
    );

    public static void main(String[] args) {
        AnsiConsole.systemInstall(); // Enable ANSI support
        System.out.println(cyan("欢迎使用学生奖项评分系统（命令行版）"));

        if (!startup()) {
            System.out.println(red("启动失败，程序退出。"));
            AnsiConsole.systemUninstall();
            return;
        }

        mainLoop();

        System.out.println(cyan("感谢使用，程序已退出。"));
        AnsiConsole.systemUninstall();
    }

    private static boolean startup() {
        System.out.println("\n请选择数据源:");
        System.out.println("1. 从新文件加载");
        System.out.println("2. 从上次文件加载");
        System.out.println("3. 使用随机数据测试");
        System.out.print(yellow("请输入选项 (1-3): "));

        String choice = scanner.nextLine();
        StartupService startupService = new StartupService();
        StartupService.StartupResult result = null;

        try {
            switch (choice) {
                case "1":
                    System.out.print(yellow("请输入源 Excel 文件路径: "));
                    String path = scanner.nextLine();
                    new BuildList().build(path);
                    result = startupService.initialize();
                    break;
                case "2":
                     result = startupService.initialize();
                    break;
                case "3":
                    if (Config.USE_RANDOM_DATA) {
                        System.out.println(green("使用随机数据构建源文件..."));
                        new BuildNull().build();
                        new BuildList().build(Config.RAW_SOURCE_PATH);
                    } else {
                        System.out.println("随机数据被禁用, 使用现有汇总文件");
                    }
                    result = startupService.initialize();
                    break;
                default:
                    System.out.println(red("无效选项，程序退出。"));
                    return false;
            }

            if (result == null || result.students.isEmpty()) {
                System.out.println(red("未能加载任何学生数据。"));
                return false;
            }

            studentList = result.students;
            dataManager = result.manager;
            System.out.println(green("数据加载成功，共 " + studentList.size() + " 名学生。"));
            return true;

        } catch (Exception e) {
            LOGGER.error("启动过程中发生错误", e);
            return false;
        }
    }

    private static void mainLoop() {
        while (true) {
            System.out.println(cyan("\n--- 主菜单 ---"));
            System.out.println("1. 列出所有学生及其进度");
            System.out.println("2. 为指定学生评分");
            System.out.println("3. 导出进度");
            System.out.println("4. 导入进度");
            System.out.println("5. 退出");
            System.out.print(yellow("请输入选项 (1-5): "));

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    listStudentsMenu();
                    break;
                case "2":
                    scoreStudentMenu();
                    break;
                case "3":
                    exportProgress();
                    break;
                case "4":
                    importProgress();
                    break;
                case "5":
                    return;
                default:
                    System.out.println(red("无效选项，请重新输入。"));
                    break;
            }
        }
    }

    private static void listStudentsMenu() {
        System.out.println(cyan("\n--- 筛选学生 ---"));
        System.out.println("1. 显示全部");
        System.out.println("2. 显示已完成");
        System.out.println("3. 显示未完成");
        System.out.print(yellow("请输入选项 (1-3): "));
        String choice = scanner.nextLine();

        List<StudentAwardRecord> recordsToList = switch (choice) {
            case "2" -> dataManager.getAllRecords().stream()
                    .filter(r -> r.getRecordedAwardCount() > 0 && r.getRecordedAwardCount() == r.getAwards().size())
                    .collect(Collectors.toList());
            case "3" -> dataManager.getAllRecords().stream()
                    .filter(r -> r.getRecordedAwardCount() < r.getAwards().size())
                    .collect(Collectors.toList());
            default -> new java.util.ArrayList<>(dataManager.getAllRecords());
        };

        listAllStudents(recordsToList);
    }


    private static void listAllStudents(List<StudentAwardRecord> records) {
        System.out.println(cyan("\n--- 学生列表 ---"));
        System.out.printf("%-15s %-10s %-15s %-10s\n", "学号", "姓名", "班级", "进度");
        System.out.println("-".repeat(55));
        records.sort((r1, r2) -> Long.compare(r1.getStudentId(), r2.getStudentId()));
        for (StudentAwardRecord record : records) {
            System.out.printf("%-15d %-10s %-15s %-10s\n",
                    record.getStudentId(),
                    record.getName(),
                    record.getClassName(),
                    record.getRecordedAwardCount() + "/" + record.getAwards().size());
        }
    }

    private static void scoreStudentMenu() {
        System.out.print(yellow("\n请输入要评分的学生的学号: "));
        String studentIdStr = scanner.nextLine();
        long studentId;
        try {
            studentId = Long.parseLong(studentIdStr);
        } catch (NumberFormatException e) {
            System.out.println(red("无效的学号格式。"));
            return;
        }

        StudentAwardRecord record = dataManager.getRecord(studentId);

        if (record == null) {
            System.out.println(red("未找到学号为 " + studentId + " 的学生记录。"));
            return;
        }

        System.out.println(cyan("\n--- 评分模式 ---"));
        System.out.println("1. 只看未评分的奖项");
        System.out.println("2. 查看并可修改所有奖项");
        System.out.print(yellow("请输入选项 (1-2): "));
        String choice = scanner.nextLine();
        boolean showAll = "2".equals(choice);

        scoreStudent(record, showAll);
    }


    private static void scoreStudent(StudentAwardRecord record, boolean showAll) {
        System.out.println("开始为学生 " + green(record.getName()) + " (" + record.getStudentId() + ") 评分。输入 'q' 可以随时退出评分。");

        List<Map<String, String>> awards = record.getAwards();

        for (int i = 0; i < awards.size(); i++) {
            Map<String, String> award = awards.get(i);
            String currentLabel = award.get("category");

            if (showAll || currentLabel == null || currentLabel.isEmpty()) {
                System.out.println("\n" + "-".repeat(20));
                System.out.printf("正在评分第 %d / %d 个奖项:\n", i + 1, awards.size());
                System.out.println("奖项名称: " + award.get("name"));
                System.out.println("证书图片链接: " + award.get("image"));
                System.out.println("当前分类: " + (currentLabel == null || currentLabel.isEmpty() ? yellow("未评分") : green(currentLabel)));
                System.out.println("请选择一个新分类:");

                for (int j = 0; j < CATEGORY_KEYS.size(); j++) {
                    String key = CATEGORY_KEYS.get(j);
                    System.out.printf("%d. %s (%.2f分)\n", j + 1, key, SCORE_MAP.get(key));
                }
                System.out.print(yellow("请输入选项编号 (1-" + CATEGORY_KEYS.size() + ", 或 'q' 退出, 直接回车跳过): "));

                String input = scanner.nextLine();
                if ("q".equalsIgnoreCase(input)) {
                    System.out.println("已退出对 " + record.getName() + " 的评分。");
                    return;
                }
                if (input.isEmpty()) {
                    continue; // Skip to next award
                }

                try {
                    int selectedIndex = Integer.parseInt(input) - 1;
                    if (selectedIndex >= 0 && selectedIndex < CATEGORY_KEYS.size()) {
                        String selectedCategory = CATEGORY_KEYS.get(selectedIndex);
                        updateAwardPoints(record, i, selectedCategory);
                        System.out.println(green("评分成功！"));
                    } else {
                        System.out.println(red("无效选项，请重新为该奖项评分。"));
                        i--; // 重新处理当前奖项
                    }
                } catch (NumberFormatException e) {
                    System.out.println(red("输入无效，请重新为该奖项评分。"));
                    i--; // 重新处理当前奖项
                }
            }
        }
        System.out.println(green("\n学生 " + record.getName() + " 的所有奖项均已处理完毕！"));
    }

    private static void updateAwardPoints(StudentAwardRecord record, int awardIndex, String newLabel) {
        String previousLabel = record.getAwardLabel(awardIndex);

        // 如果之前有标签，先减去旧分数
        if (previousLabel != null && !previousLabel.isEmpty()) {
            double oldScore = SCORE_MAP.getOrDefault(previousLabel, 0.0);
            if (Config.CATEGORY_CERT.equals(previousLabel)) {
                record.addCertTotalPoints(-oldScore);
            } else {
                record.addAwardTotalPoints(-oldScore);
            }
        } else {
            // 如果是第一次评分，增加计数
            record.incrementRecordedAwardCount();
        }

        // 添加新分数
        double newScore = SCORE_MAP.getOrDefault(newLabel, 0.0);
        if (Config.CATEGORY_CERT.equals(newLabel)) {
            record.addCertTotalPoints(newScore);
        } else {
            record.addAwardTotalPoints(newScore);
        }

        record.setAwardLabel(awardIndex, newLabel);
        dataManager.persistRecord(record);
    }

    private static void exportProgress() {
        System.out.println(cyan("\n选择导出格式:"));
        System.out.println("1. 普通 JSON");
        System.out.println("2. 压缩 JSON (GZIP)");
        System.out.print(yellow("请输入选项 (1-2): "));
        String choice = scanner.nextLine();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        boolean compressed = "2".equals(choice);
        String extension = compressed ? ".json.gz" : ".json";
        File out = new File("snapshot_" + ts + extension);

        // 确保所有学生记录都存在
        studentList.forEach(s -> dataManager.getOrCreateRecord(s.getStudentId(), s.getName(), s.getClassName()));

        SnapshotManager.exportAllProgress(out, dataManager, compressed);
        System.out.println(green("进度已成功导出到 ") + out.getAbsolutePath());
    }

    private static void importProgress() {
        System.out.print(yellow("\n请输入要导入的快照文件名: "));
        String filename = scanner.nextLine();
        File file = new File(filename);

        if (!file.exists()) {
            System.out.println(red("文件不存在: " + filename));
            return;
        }

        System.out.println(red("警告：导入操作将覆盖所有当前数据（包括Excel和数据库）！"));
        System.out.print(yellow("确定要继续吗? (y/n): "));
        String confirmation = scanner.nextLine();

        if ("y".equalsIgnoreCase(confirmation)) {
            try {
                SnapshotManager.importAndRebuild(file, dataManager);
                System.out.println(green("导入成功！正在重新加载数据..."));

                // 从重建后的数据中加载新的学生列表
                studentList.clear();
                // Re-initialize the state from the newly imported data
                StartupService.StartupResult reloadedResult = new StartupService().initialize();
                if (reloadedResult != null) {
                    studentList.addAll(reloadedResult.students);
                    dataManager = reloadedResult.manager;
                    System.out.println(green("数据重新加载完毕。"));
                } else {
                    System.out.println(red("错误：重新加载数据失败。"));
                }

            } catch (Exception e) {
                LOGGER.error("导入快照失败", e);
            }
        } else {
            System.out.println("导入操作已取消。");
        }
    }
}

