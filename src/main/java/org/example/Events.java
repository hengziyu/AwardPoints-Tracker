package org.example;

import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 重构后的 Events：适配模块化后架构，不再依赖 App。
 * 提供静态/实例方法支持：
 *  - updateTableView(tableView, students, manager)
 *  - filterBySearch(all, query)
 *  - filterByStatus(all, manager, status)
 * 保留与原 Python 语义一致的过滤逻辑；现由 UiController 直接可选调用。
 */
public class Events {
    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);

    /** 刷新表格展示 */
    public static void updateTableView(TableView<StudentRow> tv, List<Student> source, NewDataManager manager) {
        if (tv == null) return;
        tv.getItems().clear();
        for (Student s : source) {
            StudentAwardRecord rec = manager.getRecord(s.getStudentId());
            int recorded = rec != null ? rec.getRecordedAwardCount() : 0;
            String progress = recorded + "/" + s.getTotalAwards();
            tv.getItems().add(new StudentRow(s.getStudentId(), s.getName(), s.getClassName(), progress));
        }
    }

    /** 搜索过滤：姓名或学号包含（大小写不敏感） */
    public static List<Student> filterBySearch(List<Student> all, String query) {
        if (all == null) return List.of();
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        return all.stream().filter(s ->
                String.valueOf(s.getStudentId()).contains(q) ||
                        (s.getName() != null && s.getName().toLowerCase(Locale.ROOT).contains(q))
        ).collect(Collectors.toList());
    }

    /** 状态过滤：全部 / 已录入完成 / 未录入完成 */
    public static List<Student> filterByStatus(List<Student> all, NewDataManager manager, String status) {
        if (all == null) return List.of();
        if (status == null || status.equals("全部")) return all;
        List<Student> result = new ArrayList<>();
        for (Student s : all) {
            StudentAwardRecord rec = manager.getRecord(s.getStudentId());
            boolean complete = rec != null && rec.getRecordedAwardCount() == s.getTotalAwards();
            if ("已录入完成".equals(status) && complete) result.add(s);
            else if ("未录入完成".equals(status) && !complete) result.add(s);
        }
        return result;
    }

    /** 重新载入 newFile.xlsx 并返回最新记录映射影响的学生（调用后请再次 filter + updateTableView） */
    public static void reloadRecords(NewDataManager manager) {
        try { manager.reloadFromExcel(); }
        catch (Exception e) { LOGGER.error("刷新数据失败", e); }
    }
}
