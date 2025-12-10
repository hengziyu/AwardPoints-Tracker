package org.example.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Student;
import org.example.model.StudentAwardRecord;
import org.example.model.StudentRow;
import org.example.persistence.NewDataManager;
import org.example.persistence.SnapshotManager;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UI 控制器
 */
public class UiController implements AwardDialogListener {
    private static final Logger LOGGER = LoggerUtil.getLogger(UiController.class.getName());
    private final Stage primaryStage;
    private final NewDataManager newDataManager;
    private List<Student> students;
    private final TableView<StudentRow> tableView;
    private final GuiElements guiElements;
    private List<Student> filteredStudents;

    public UiController(Stage stage, List<Student> students, NewDataManager manager) {
        this.primaryStage = stage;
        this.students = students;
        this.newDataManager = manager;
        this.filteredStudents = new ArrayList<>(students);
        this.guiElements = new GuiElements(this::search, this::filter);
        this.tableView = guiElements.getTableView();
        hookTableDoubleClick();
        setupStage();
        updateTableView();
        guiElements.getExportButton().setOnAction(e -> exportSnapshot());
        guiElements.getImportButton().setOnAction(e -> importSnapshot());
    }

    private void setupStage() {
        Scene scene = new Scene(guiElements.getRootPane(), 1000, 600);
        primaryStage.setTitle("奖项管理系统");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void hookTableDoubleClick() {
        tableView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                StudentRow selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Student student = students.stream().filter(s -> s.getStudentId() == selected.getStudentId()).findFirst().orElse(null);
                    if (student != null) {
                        new AwardDialog(primaryStage, student, newDataManager, this).show();
                    }
                }
            }
        });
    }

    @Override
    public void onDataUpdated() {
        Platform.runLater(this::updateTableView);
    }

    private void updateTableView() {
        tableView.getItems().clear();
        for (Student s : filteredStudents) {
            StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
            int recorded = rec != null ? rec.getRecordedAwardCount() : 0;
            int total = s.getTotalAwards();
            // 当从快照导入时，totalAwards可能为0，此时使用recorded作为total
            if (total == 0 && recorded > 0) {
                total = recorded;
            }
            String progress = recorded + "/" + total;
            tableView.getItems().add(new StudentRow(s.getStudentId(), s.getName(), s.getClassName(), progress));
        }
    }

    public void search(String query) {
        if (query == null || query.trim().isEmpty()) filteredStudents = students;
        else {
            String q = query.trim().toLowerCase();
            filteredStudents = students.stream().filter(s -> String.valueOf(s.getStudentId()).contains(q) || (s.getName() != null && s.getName().toLowerCase().contains(q))).toList();
        }
        updateTableView();
    }

    public void filter(String value) {
        List<Student> source = students; // 使用当前的学生列表进行过滤
        if (value == null || value.equals("全部")) {
            filteredStudents = new ArrayList<>(source);
        } else if (value.equals("已录入完成")) {
            filteredStudents = source.stream().filter(s -> {
                StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
                int total = s.getTotalAwards() > 0 ? s.getTotalAwards() : (rec != null ? rec.getRecordedAwardCount() : 0);
                return rec != null && rec.getRecordedAwardCount() == total && total > 0;
            }).collect(Collectors.toList());
        } else if (value.equals("未录入完成")) {
            filteredStudents = source.stream().filter(s -> {
                StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
                int total = s.getTotalAwards() > 0 ? s.getTotalAwards() : (rec != null ? rec.getRecordedAwardCount() : 0);
                return rec == null || rec.getRecordedAwardCount() < total;
            }).collect(Collectors.toList());
        }
        updateTableView();
    }

    private void exportSnapshot() {
        try {
            // 确保所有学生都在 NewDataManager 中有记录
            for (Student student : students) {
                newDataManager.getOrCreateRecord(student.getStudentId(), student.getName(), student.getClassName());
            }
            Collection<StudentAwardRecord> allRecords = newDataManager.getAllRecords();


            ChoiceDialog<String> dialog = new ChoiceDialog<>("普通 JSON", "普通 JSON", "压缩 JSON(GZIP)");
            dialog.setTitle("导出进度");
            dialog.setHeaderText("选择导出格式");
            dialog.setContentText("格式:");
            String choice = dialog.showAndWait().orElse(null);
            if (choice == null) return;
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            if ("压缩 JSON(GZIP)".equals(choice)) {
                File out = new File("snapshot_" + ts + ".json.gz");
                SnapshotManager.exportSnapshotCompressed(out, allRecords);
            } else {
                File out = new File("snapshot_" + ts + ".json");
                SnapshotManager.exportSnapshot(out, allRecords);
            }
        } catch (Exception ex) {
            LoggerUtil.logException(LOGGER, ex, "导出快照按钮执行失败");
        }
    }

    private void importSnapshot() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择快照文件导入");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Snapshot", "*.json", "*.json.gz"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("确认导入");
            confirmationDialog.setHeaderText("警告：此操作将覆盖所有现有进度！");
            confirmationDialog.setContentText("您确定要从 " + selectedFile.getName() + " 导入并覆盖所有当前数据吗？");

            Optional<ButtonType> result = confirmationDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    // 使用 SnapshotManager 重建数据
                    SnapshotManager.importAndRebuild(selectedFile, newDataManager);

                    // 从重建后的数据中加载新的学生列表
                    List<Student> newStudents = new ArrayList<>();
                    for (StudentAwardRecord record : newDataManager.getAllRecords()) {
                        // 创建一个临时的 Student 对象用于UI显示
                        // totalAwards 设置为 recordedAwardCount，因为快照中没有原始的总奖项数
                        Student student = new Student(record.getStudentId(), record.getName(), record.getClassName(), new ArrayList<>());
                        student.setTotalAwards(record.getRecordedAwardCount());
                        newStudents.add(student);
                    }

                    // 更新 UiController 的内部状态
                    this.students.clear();
                    this.students.addAll(newStudents);
                    this.filteredStudents = new ArrayList<>(this.students);

                    // 刷新UI
                    guiElements.getSearchEntry().clear();
                    guiElements.getFilterComboBox().getSelectionModel().selectFirst();
                    updateTableView();

                    Alert infoDialog = new Alert(Alert.AlertType.INFORMATION);
                    infoDialog.setTitle("导入成功");
                    infoDialog.setHeaderText(null);
                    infoDialog.setContentText("已成功从快照文件导入数据。");
                    infoDialog.showAndWait();

                } catch (Exception ex) {
                    LoggerUtil.logException(LOGGER, ex, "导入快照失败");
                    Alert errorDialog = new Alert(Alert.AlertType.ERROR);
                    errorDialog.setTitle("导入失败");
                    errorDialog.setHeaderText(null);
                    errorDialog.setContentText("导入快照时发生错误: " + ex.getMessage());
                    errorDialog.showAndWait();
                }
            }
        }
    }

    public TableView<StudentRow> getTableView() {
        return tableView;
    }

    public GuiElements getGuiElements() {
        return guiElements;
    }
}
