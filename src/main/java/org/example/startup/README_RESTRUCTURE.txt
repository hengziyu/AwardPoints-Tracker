重构说明: 已将原 org.example 根目录下所有业务类按领域分层。
app/        应用入口与启动服务
build/      随机与汇总构建
config/     配置与模板初始化
image/      图像加载与变换
model/      领域数据模型
persistence/数据持久化 (Excel + SQLite)
processing/ 数据加载/文件操作/事件
startup/    启动完整性检查
ui/         JavaFX UI 与对话框
util/       通用工具 (Logger)
package org.example.app;

import javafx.application.Application; import javafx.scene.Scene; import javafx.stage.Stage; import javafx.scene.layout.StackPane; import org.example.util.LoggerUtil; import org.slf4j.Logger; import org.example.app.DataLoader; import org.example.app.StartupService; import org.example.ui.UiController; import org.example.persistence.NewDataManager; import org.example.model.Student; import java.util.List;
/** 程序入口 (仅包调整) */
public class App extends Application { private static final Logger LOGGER= LoggerUtil.getLogger(App.class.getName()); private DataLoader dataLoader; private UiController uiController; @Override public void start(Stage primaryStage){ try{ if(primaryStage.getScene()==null) primaryStage.setScene(new Scene(new StackPane(),1,1)); dataLoader=new DataLoader(); boolean ok=dataLoader.initializeData(primaryStage); if(!ok) return; DataLoader.Choice choice=dataLoader.getFinalChoice(); StartupService.StartupResult result=new StartupService().initialize(choice); List<Student> students=result.students; NewDataManager manager=result.manager; uiController=new UiController(primaryStage, students, manager); } catch(Exception e){ LoggerUtil.logException(LOGGER,e,"应用启动失败"); } } public static void main(String[] args){ launch(args); }}
package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Logger 工具 (原 logger.py) - 保持逻辑不变，仅包结构调整。
 */
public final class LoggerUtil {
    private LoggerUtil() { }
    public static Logger getLogger(String name) { return LoggerFactory.getLogger(name); }
    public static void logException(Logger logger, Throwable ex, String message) {
        if (logger == null) logger = LoggerFactory.getLogger("UnknownLogger");
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        logger.error(message + " | " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n" + sw);
    }
}

