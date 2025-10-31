package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 迁移自 gui_elements.py
 * 功能：构建顶部搜索与过滤控件以及中间的表格视图。
 * 一比一还原：
 *  - 顶部：标签“查找:”、文本输入、查找按钮、过滤下拉（全部/已录入完成/未录入完成）
 *  - 中间：表格（对应原 TreeView）显示 学号/姓名/班级/奖项
 */
public class GuiElements {

    private final BorderPane rootPane;
    private final HBox topFrame;
    private final VBox middleFrame;
    private final Label searchLabel;
    private final TextField searchEntry;
    private final Button searchButton;
    private final ComboBox<String> filterComboBox;
    private final TableView<StudentRow> tableView; // changed generic
    private final ScrollPane scrollPane; // 包裹表格的滚动条（TreeView 原有垂直滚动）

    /**
     * 构造函数
     * @param searchCallback 查找按钮回调 (传入当前文本框内容)
     * @param filterCallback 下拉选择改变回调 (传入选中值)
     */
    public GuiElements(java.util.function.Consumer<String> searchCallback,
                       java.util.function.Consumer<String> filterCallback) {
        rootPane = new BorderPane();

        // 顶部框
        topFrame = new HBox(8);
        topFrame.setPadding(new Insets(8));
        topFrame.setAlignment(Pos.CENTER_LEFT);

        searchLabel = new Label("查找:");
        searchEntry = new TextField();
        searchButton = new Button("查找");
        searchButton.setOnAction(e -> searchCallback.accept(searchEntry.getText()));

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("全部", "已录入完成", "未录入完成");
        filterComboBox.getSelectionModel().select(0);
        filterComboBox.setOnAction(e -> {
            String val = filterComboBox.getSelectionModel().getSelectedItem();
            filterCallback.accept(val);
        });

        topFrame.getChildren().addAll(searchLabel, searchEntry, searchButton, filterComboBox);

        // 中间框
        middleFrame = new VBox();
        middleFrame.setPadding(new Insets(8));
        middleFrame.setSpacing(8);

        tableView = new TableView<>();
        TableColumn<StudentRow, Long> colId = new TableColumn<>("学号");
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("studentId"));
        TableColumn<StudentRow, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        TableColumn<StudentRow, String> colClazz = new TableColumn<>("班级");
        colClazz.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("clazz"));
        TableColumn<StudentRow, String> colProgress = new TableColumn<>("奖项");
        colProgress.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("progress"));

        tableView.getColumns().addAll(colId, colName, colClazz, colProgress);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        middleFrame.getChildren().add(scrollPane);

        rootPane.setTop(topFrame);
        rootPane.setCenter(middleFrame);
    }

    public BorderPane getRootPane() { return rootPane; }
    public TextField getSearchEntry() { return searchEntry; }
    public ComboBox<String> getFilterComboBox() { return filterComboBox; }
    public TableView<StudentRow> getTableView() { return tableView; }
}
