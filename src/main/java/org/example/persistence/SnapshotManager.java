package org.example.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.model.StudentAwardRecord;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;
import org.example.config.Config;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

/**
 * 单文件快照导出/导入，实现“一份文件共享进度”。
 * 格式：JSON 包含 meta + records (每条含学生基础信息、积分、计数、标签数组)
 */
public final class SnapshotManager {
    private static final Logger LOGGER = LoggerUtil.getLogger(SnapshotManager.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SnapshotManager() {}

    // ================= 基础导出 =================
    public static void exportSnapshot(File target, Collection<StudentAwardRecord> records) {
        try {
            ObjectNode root = buildSnapshotRoot(records);
            byte[] data = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            try (FileOutputStream fos = new FileOutputStream(target)) { fos.write(data); }
            LOGGER.info("导出快照成功 -> " + target.getAbsolutePath());
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "导出快照失败");
        }
    }

    public static void exportSnapshotCompressed(File target, Collection<StudentAwardRecord> records) {
        File realTarget = target.getName().endsWith(".gz") ? target : new File(target.getParentFile(), target.getName() + ".gz");
        try {
            ObjectNode root = buildSnapshotRoot(records);
            byte[] data = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            try (GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(realTarget)))) {
                gos.write(data);
            }
            LOGGER.info("导出压缩快照成功 -> " + realTarget.getAbsolutePath());
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "导出压缩快照失败");
        }
    }

    // ================= 高层：导出当前全部进度 =================
    /**
     * 导出全部进度：传入 manager 直接获取其所有记录。
     */
    public static void exportAllProgress(File target, NewDataManager manager, boolean compressed) {
        Collection<StudentAwardRecord> all = manager.getAllRecords();
        if (compressed) {
            exportSnapshotCompressed(target, all);
        } else {
            exportSnapshot(target, all);
        }
    }

    // ================= 基础导入（不清空重建，仅追加/覆盖行） =================
    public static void importSnapshot(File source, NewDataManager manager, boolean overwrite) {
        Optional<List<StudentAwardRecord>> opt = readSnapshotRecords(source);
        if (opt.isEmpty()) return;
        List<StudentAwardRecord> list = opt.get();
        manager.importRecords(list, overwrite);
        LOGGER.info("导入快照完成: 总计 " + list.size());
    }

    // ================= 高层：清空并重建（提示覆盖风险） =================
    /**
     * 完整重建：删除原 Excel 与 DB，重新写入。适用于需要“单文件共享所有数据”的场景。
     * 调用方应在界面弹窗提示：此操作将覆盖并丢失现有进度。
     * @param source 快照文件（支持 .json 或 .json.gz）
     */
    public static void importAndRebuild(File source, NewDataManager manager) {
        Optional<List<StudentAwardRecord>> opt = readSnapshotRecords(source);
        if (opt.isEmpty()) return;
        List<StudentAwardRecord> list = opt.get();
        LOGGER.warn("即将覆盖所有现有数据并用快照重建 (记录数=" + list.size() + ")");

        // 清理旧文件
        deleteFileIfExists(new File(Config.RAW_SOURCE_PATH));
        deleteFileIfExists(new File(Config.AWARDS_SUMMARY_PATH));

        manager.clearAndRecreateStorage();
        manager.bulkLoadRecords(list); // 快速批量写入
        LOGGER.info("重建完成: 写入记录 " + list.size());
    }

    private static void deleteFileIfExists(File file) {
        if (file.exists()) {
            if (file.delete()) {
                LOGGER.info("已删除旧文件: " + file.getAbsolutePath());
            } else {
                LOGGER.warn("无法删除旧文件: " + file.getAbsolutePath());
            }
        }
    }

    // ================= 内部：读取快照 =================
    private static Optional<List<StudentAwardRecord>> readSnapshotRecords(File source) {
        if (!source.exists()) {
            LOGGER.warn("快照文件不存在: " + source.getAbsolutePath());
            return Optional.empty();
        }
        boolean gz = source.getName().endsWith(".gz");
        try (InputStream is = gz ? new GZIPInputStream(new BufferedInputStream(new FileInputStream(source))) : new FileInputStream(source)) {
            JsonNode root = MAPPER.readTree(is);
            JsonNode records = root.get("records");
            if (records == null || !records.isArray()) {
                LOGGER.error("快照结构无 records 数组");
                return Optional.empty();
            }
            List<StudentAwardRecord> list = new ArrayList<>();
            for (JsonNode n : records) {
                long sid = n.path("studentId").asLong();
                String name = n.path("name").asText("");
                String clazz = n.path("className").asText("");
                StudentAwardRecord r = new StudentAwardRecord(sid, name, clazz);
                r.setCertTotalPoints(n.path("certTotalPoints").asDouble(0.0));
                r.setAwardTotalPoints(n.path("awardTotalPoints").asDouble(0.0));
                r.setRecordedAwardCount(n.path("recordedAwardCount").asInt(0));
                JsonNode labels = n.path("labels");
                if (labels.isArray()) {
                    int i = 0;
                    for (JsonNode ln : labels) { if (i < 50) r.setAwardLabel(i++, ln.asText("")); }
                }
                list.add(r);
            }
            return Optional.of(list);
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "读取快照失败");
            return Optional.empty();
        }
    }

    // ================= 构建 JSON 根对象 =================
    private static ObjectNode buildSnapshotRoot(Collection<StudentAwardRecord> records) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode meta = root.putObject("meta");
        meta.put("version", 1);
        meta.put("generatedAt", System.currentTimeMillis());
        meta.put("recordCount", records.size());
        ArrayNode arr = root.putArray("records");
        for (StudentAwardRecord r : records) {
            ObjectNode node = arr.addObject();
            node.put("studentId", r.getStudentId());
            node.put("name", r.getName());
            node.put("className", r.getClassName());
            node.put("certTotalPoints", r.getCertTotalPoints());
            node.put("awardTotalPoints", r.getAwardTotalPoints());
            node.put("recordedAwardCount", r.getRecordedAwardCount());
            ArrayNode labels = node.putArray("labels");
            for (String l : r.getAwardLabels()) labels.add(l == null ? "" : l);
        }
        return root;
    }
}
