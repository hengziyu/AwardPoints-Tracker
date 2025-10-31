package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.app.DataLoader;
import org.example.app.StartupService;
import org.example.ui.UiController;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

/**
 * 主程序入口：迁移自 Python main.py 逻辑。
 * 功能：
 * - 若启用随机数据且源文件不存在，调用 BuildNull 构建源 Excel
 * - 调用 BuildList 构建汇总文件 Build_Summary.xlsx
 * - 加载汇总文件为 Student 列表
 * - 展示 JavaFX 主界面（表格显示学号/姓名/班级/奖项进度）
 * - 双击行打开 AwardDialog
 */
public class App extends Application {

    private static final Logger LOGGER = LoggerUtil.getLogger(App.class.getName());
    private DataLoader dataLoader;
    private UiController uiController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // 修复: 若主舞台尚无 Scene，先绑定一个占位 Scene，避免对话框 initOwner 访问 null Scene 导致 NPE
            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(new StackPane(), 1, 1));
            }
            dataLoader = new DataLoader();
            boolean ok = dataLoader.initializeData(primaryStage);
            if (!ok) return;
            DataLoader.Choice choice = dataLoader.getFinalChoice();
            StartupService.StartupResult result = new StartupService().initialize(choice);
            uiController = new UiController(primaryStage, result.students, result.manager);
        } catch (Exception e) {
            LoggerUtil.logException(LOGGER, e, "应用启动失败");
        }
    }
}
