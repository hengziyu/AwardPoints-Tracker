package org.example;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责纯 UI 逻辑：构建界面、表格刷新、双击打开对话框、委托搜索过滤。
 * 原 App 中的 buildUI / loadTableData / onDataUpdated / search / filter 迁移。
 */
public class UiController implements AwardDialogListener {
    private static final Logger LOGGER = LoggerUtil.getLogger(UiController.class.getName());

    private final Stage primaryStage;
    private final NewDataManager newDataManager;
    private final List<Student> students;

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
    }

    private void setupStage() {
        Scene scene = new Scene(guiElements.getRootPane(), 1000, 600);
        primaryStage.setTitle("奖项管理系统 of 🍊🐟");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void hookTableDoubleClick() {
        tableView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                StudentRow selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Student student = students.stream()
                            .filter(s -> s.getStudentId() == selected.getStudentId())
                            .findFirst().orElse(null);
                    if (student != null) {
                        new AwardDialog(primaryStage, student, newDataManager, this).show();
                    }
                }
            }
        });
    }

    // AwardDialogListener callback
    @Override
    public void onDataUpdated() {
        Platform.runLater(this::updateTableView);
    }

    private void updateTableView() {
        tableView.getItems().clear();
        for (Student s : filteredStudents) {
            StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
            int recorded = rec != null ? rec.getRecordedAwardCount() : 0;
            String progress = recorded + "/" + s.getTotalAwards();
            tableView.getItems().add(new StudentRow(s.getStudentId(), s.getName(), s.getClassName(), progress));
        }
    }

    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredStudents = students;
        } else {
            String q = query.trim().toLowerCase();
            filteredStudents = students.stream().filter(s ->
                    String.valueOf(s.getStudentId()).contains(q) ||
                            (s.getName() != null && s.getName().toLowerCase().contains(q))
            ).toList();
        }
        updateTableView();
    }

    public void filter(String value) {
        if (value == null || value.equals("全部")) {
            filteredStudents = students;
        } else if (value.equals("已录入完成")) {
            filteredStudents = students.stream().filter(s -> {
                StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
                return rec != null && rec.getRecordedAwardCount() == s.getTotalAwards();
            }).toList();
        } else if (value.equals("未录入完成")) {
            filteredStudents = students.stream().filter(s -> {
                StudentAwardRecord rec = newDataManager.getRecord(s.getStudentId());
                return rec == null || rec.getRecordedAwardCount() < s.getTotalAwards();
            }).toList();
        }
        updateTableView();
    }

    public TableView<StudentRow> getTableView() { return tableView; }
    public GuiElements getGuiElements() { return guiElements; }
}
