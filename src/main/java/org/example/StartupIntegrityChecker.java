package org.example;

import org.slf4j.Logger;
import org.example.LoggerUtil;

import java.io.File;
import java.util.List;

/**
 * 启动完整性检查：不改变业务数据，仅输出日志，帮助确认所有模块协同正常。
 * 还原后不影响原逻辑，可在 App 初始化时调用。
 */
public final class StartupIntegrityChecker {
    private static final Logger LOGGER = LoggerUtil.getLogger(StartupIntegrityChecker.class.getName());
    private StartupIntegrityChecker() {}

    public static void runAll(NewDataManager newDataManager, List<Student> students) {
        try {
            checkFiles();
            checkStudents(students);
            checkRecordMap(newDataManager, students);
            LOGGER.info("启动完整性检查完成");
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "启动完整性检查异常");
        }
    }

    private static void checkFiles() {
        File summary = new File(Config.FILE_PATH);
        File newFile = new File(Config.NEW_FILE_PATH);
        File db = new File(Config.DB_PATH);
        LOGGER.debug("文件存在性: summary={} newFile={} db={}"
                , summary.exists(), newFile.exists(), db.exists());
    }

    private static void checkStudents(List<Student> students) {
        if (students == null || students.isEmpty()) {
            LOGGER.warn("学生列表为空或未加载");
            return;
        }
        int totalAwards = students.stream().mapToInt(Student::getTotalAwards).sum();
        LOGGER.debug("已加载学生数量={} 累计奖项数量={}", students.size(), totalAwards);
    }

    private static void checkRecordMap(NewDataManager dm, List<Student> students) {
        if (dm == null) {
            LOGGER.warn("NewDataManager 未初始化");
            return;
        }
        int records = 0;
        int withFull = 0;
        for (Student s : students) {
            StudentAwardRecord r = dm.getRecord(s.getStudentId());
            if (r != null) {
                records++;
                if (r.getRecordedAwardCount() == s.getTotalAwards()) withFull++;
            }
        }
        LOGGER.debug("已存在记录学生数={} 其中已全部录入={} (recordMap size可能 > 统计数)", records, withFull);
    }
}

