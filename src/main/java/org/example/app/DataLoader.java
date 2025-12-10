package org.example.app;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.build.BuildList;
import org.example.build.BuildNull;
import org.example.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * 数据源选择 (逻辑未改)
 */
public class DataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private Choice finalChoice;
    private String newSourcePath;

    public Choice getFinalChoice() {
        return finalChoice;
    }

    public String getNewSourcePath() {
        return newSourcePath;
    }

    public boolean initializeData(Stage ownerStage) {
        Choice result = showStartupDialog(ownerStage);
        finalChoice = result;
        if (result == null) {
            showErrorAndExit(ownerStage, "未选择加载方式, 程序退出。");
            return false;
        }
        try {
            switch (result) {
                case NEW -> {
                    String path = chooseFile(ownerStage);
                    if (path == null) {
                        showErrorAndExit(ownerStage, "未选择文件, 程序退出。");
                        return false;
                    }
                    newSourcePath = path;
                    LOGGER.debug("从新文件加载并生成汇总: " + path);
                    new BuildList().build(path);
                }
                case LAST -> LOGGER.debug("从上次文件加载(不重建): " + Config.AWARDS_SUMMARY_PATH);
                case RANDOM -> {
                    if (Config.USE_RANDOM_DATA) {
                        LOGGER.debug("使用随机数据构建源文件");
                        new BuildNull().build();
                    } else {
                        LOGGER.info("随机数据被禁用, 使用现有汇总文件");
                        finalChoice = Choice.LAST;
                    }
                }
                default -> {
                    showErrorAndExit(ownerStage, "未知选择, 程序退出。");
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("数据初始化失败", ex);
            showErrorAndExit(ownerStage, "数据初始化失败: " + ex.getMessage());
            return false;
        }
    }

    private String chooseFile(Stage ownerStage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("请选择新的 Excel 文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx", "*.xls"));
        File f = chooser.showOpenDialog(ownerStage);
        return f != null ? f.getAbsolutePath() : null;
    }

    private Choice showStartupDialog(Stage ownerStage) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(ownerStage);
        alert.setTitle("请选择数据源");
        alert.setHeaderText("请选择数据加载方式:");
        ButtonType btnNew = new ButtonType("从新文件加载");
        ButtonType btnLast = new ButtonType("从上次文件加载");
        ButtonType btnRandom = new ButtonType("使用随机数据测试");
        alert.getButtonTypes().setAll(btnNew, btnLast, btnRandom);
        Stage dialogStage = (Stage) alert.getDialogPane().getScene().getWindow();
        dialogStage.setOnCloseRequest(e -> e.consume());
        styleDialog(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) return null;
        if (result.get() == btnNew) return Choice.NEW;
        if (result.get() == btnLast) return Choice.LAST;
        if (result.get() == btnRandom) return Choice.RANDOM;
        return null;
    }

    private void styleDialog(DialogPane pane) {
        pane.setStyle("-fx-font-size:14px;");
        for (ButtonType bt : pane.getButtonTypes()) {
            Button b = (Button) pane.lookupButton(bt);
            b.setDefaultButton(false);
        }
    }

    private void showErrorAndExit(Stage ownerStage, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.initOwner(ownerStage);
        alert.showAndWait();
        ownerStage.close();
    }

    public enum Choice {NEW, LAST, RANDOM}
}

