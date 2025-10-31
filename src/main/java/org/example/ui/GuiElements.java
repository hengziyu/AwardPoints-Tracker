package org.example.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.example.model.StudentRow;

/**
 * GUI 元素构建
 */
public class GuiElements {
    private final BorderPane rootPane;
    private final HBox topFrame;
    private final VBox middleFrame;
    private final Label searchLabel;
    private final TextField searchEntry;
    private final Button searchButton;
    private final ComboBox<String> filterComboBox;
    private final TableView<StudentRow> tableView;
    private final ScrollPane scrollPane;

    public GuiElements(java.util.function.Consumer<String> searchCallback, java.util.function.Consumer<String> filterCallback) {
        rootPane = new BorderPane();
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
        middleFrame = new VBox();
        middleFrame.setPadding(new Insets(8));
        middleFrame.setSpacing(8);
        tableView = new TableView<>();
        TableColumn<StudentRow, Long> colId = new TableColumn<>("学号");
        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        TableColumn<StudentRow, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<StudentRow, String> colClazz = new TableColumn<>("班级");
        colClazz.setCellValueFactory(new PropertyValueFactory<>("clazz"));
        TableColumn<StudentRow, String> colProgress = new TableColumn<>("奖项");
        colProgress.setCellValueFactory(new PropertyValueFactory<>("progress"));
        tableView.getColumns().addAll(colId, colName, colClazz, colProgress);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        middleFrame.getChildren().add(scrollPane);
        rootPane.setTop(topFrame);
        rootPane.setCenter(middleFrame);
    }

    public BorderPane getRootPane() {
        return rootPane;
    }

    public TextField getSearchEntry() {
        return searchEntry;
    }

    public ComboBox<String> getFilterComboBox() {
        return filterComboBox;
    }

    public TableView<StudentRow> getTableView() {
        return tableView;
    }
}

