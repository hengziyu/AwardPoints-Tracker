package org.example;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * 迁移自 data_loader.py
 * 功能：程序启动时弹出数据源选择对话框：
 *  - 从新文件加载 (new)
 *  - 从上次文件加载 (last)
 *  - 使用随机数据测试 (random)
 * 选择后执行对应构建逻辑：
 *  - new: 选择任意 Excel 文件并调用 BuildList.build(path)
 *  - last: 调用 BuildList.build(Config.FILE_PATH) （与原 Python 行为保持一致）
 *  - random: 若 USE_RANDOM_DATA 为真，调用 BuildNull.build()，否则回退 last
 * 返回布尔值表示是否初始化成功。
 * 一比一复原，无 TODO。
 */
public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private Choice finalChoice; // 记录最后选择
    private String newSourcePath; // 当选择 NEW 时的源文件路径

    public Choice getFinalChoice() { return finalChoice; }
    public String getNewSourcePath() { return newSourcePath; }

    /**
     * 显示启动选择并构建数据
     * @param ownerStage 主舞台
     * @return true 如果成功初始化，否则 false
     */
    public boolean initializeData(Stage ownerStage) {
        Choice result = showStartupDialog(ownerStage);
        finalChoice = result;
        if (result == null) {
            showErrorAndExit(ownerStage, "未选择加载方式，程序退出。");
            return false;
        }
        try {
            switch (result) {
                case NEW: { // 直接构建新的汇总文件
                    String path = chooseFile(ownerStage);
                    if (path == null) {
                        showErrorAndExit(ownerStage, "未选择文件，程序退出。");
                        return false;
                    }
                    newSourcePath = path;
                    LOGGER.debug("从新文件加载并生成汇总: " + path);
                    new BuildList().build(path);
                    break; }
                case LAST: { // 不重建，直接使用现有 Build_Summary.xlsx
                    LOGGER.debug("从上次文件加载（不重建）: " + Config.FILE_PATH);
                    break; }
                case RANDOM: { // 仅生成原始源文件，后续由 App 触发 buildList
                    if (Config.USE_RANDOM_DATA) {
                        LOGGER.debug("使用随机数据构建源文件");
                        new BuildNull().build();
                    } else {
                        LOGGER.info("随机数据被禁用，使用现有汇总文件");
                        finalChoice = Choice.LAST; // 回退标记
                    }
                    break; }
                default: {
                    showErrorAndExit(ownerStage, "未知选择，程序退出。");
                    return false; }
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("数据初始化失败", ex);
            showErrorAndExit(ownerStage, "数据初始化失败: " + ex.getMessage());
            return false;
        }
    }

    /** 显示文件选择 */
    private String chooseFile(Stage ownerStage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("请选择新的 Excel 文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx", "*.xls"));
        File f = chooser.showOpenDialog(ownerStage);
        return f != null ? f.getAbsolutePath() : null;
    }

    /** 弹出启动对话框 */
    private Choice showStartupDialog(Stage ownerStage) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(ownerStage);
        alert.setTitle("请选择数据源");
        alert.setHeaderText("请选择数据加载方式：");

        ButtonType btnNew = new ButtonType("从新文件加载");
        ButtonType btnLast = new ButtonType("从上次文件加载");
        ButtonType btnRandom = new ButtonType("使用随机数据测试");
        alert.getButtonTypes().setAll(btnNew, btnLast, btnRandom);

        // 禁止直接关闭
        Stage dialogStage = (Stage) alert.getDialogPane().getScene().getWindow();
        dialogStage.setOnCloseRequest(event -> event.consume());

        styleDialog(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) return null;
        if (result.get() == btnNew) return Choice.NEW;
        if (result.get() == btnLast) return Choice.LAST;
        if (result.get() == btnRandom) return Choice.RANDOM;
        return null;
    }

    /** 简单样式调整 */
    private void styleDialog(DialogPane pane) {
        pane.setStyle("-fx-font-size: 14px;");
        for (ButtonType bt : pane.getButtonTypes()) {
            Button b = (Button) pane.lookupButton(bt);
            b.setDefaultButton(false);
        }
    }

    /** 显示错误并退出 */
    private void showErrorAndExit(Stage ownerStage, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.initOwner(ownerStage);
        alert.showAndWait();
        ownerStage.close();
    }

    /** 选择枚举 */
    public enum Choice { NEW, LAST, RANDOM }
}
