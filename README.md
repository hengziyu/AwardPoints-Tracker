# 奖项管理系统

## 功能概述

- 数据源选择: 启动时选择 从新文件 / 上次文件 / 随机数据。
- 随机构建: `BuildNull` 生成初始随机 Excel 数据源 (`Raw_Source.xlsx`)。
- 汇总构建: `BuildList` 读取源数据并合并生成 `Awards_Summary.xlsx`。
- 主界面: JavaFX 表格展示学号/姓名/班级/奖项录入进度，支持搜索与过滤。
- 奖项详情对话框: 浏览奖项图片，上一张/下一张，异步与预加载，旋转/镜像，分类打分（证书/国/省市/校/院/无）。
- 持久化: 评分与分类写入 `Student_Awards.xlsx` 与 `student.db` (SQLite UPSERT)。
- 图像处理: 异步加载、镜像与旋转；纯 JavaFX 实现避免额外依赖。
- 记录管理: `NewDataManager` 统一 Excel 与数据库读写；`StudentAwardRecord` 管理单个学生的 50 项分类标签与积分。
- 日志: 通过 `LoggerUtil` 统一异常记录与具名 Logger。

## 运行环境

- JDK 17+ (pom 使用 `<maven.compiler.release>17</maven.compiler.release>`)
- Maven 构建
- Windows 平台 JavaFX (pom 指定 `classifier win`)

## 快速开始

```bash
mvn clean package
mvn javafx:run
```

或使用打包后的可执行 JAR：

```bash
java -jar target/untitled-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 目录结构

```
src/main/java/org/example/
  App.java                # 主入口 (JavaFX)
  DataLoader.java         # 启动数据源选择对话框
  BuildNull.java          # 随机源数据构建
  BuildList.java          # 汇总生成
  AwardDialog.java        # 奖项详情对话框
  ImageUtils.java         # 异步图片与变换
  NewDataManager.java     # Student_Awards 与 SQLite 管理
  DbOperation.java        # SQLite 基础操作
  FileOperations.java     # Excel 读写辅助 (原 file_operations)
  DataProcessing.java     # 数据批量加载 (原 data_processing)
  Student.java / Award.java / StudentAwardRecord.java
  GuiElements.java        # 主界面控件构建
  Events.java             # 搜索、过滤与刷新逻辑
  Config.java             # 全局配置常量
  LoggerUtil.java         # 日志工具 (原 logger.py)
```

## 数据文件说明

- `Raw_Source.xlsx`: 随机构建的源数据 (BuildNull)。
- `Awards_Summary.xlsx`: 汇总后的奖项 JSON 列表 (BuildList)。
- `Student_Awards.xlsx`: 用户录入与分类积分持久化表。
- `student.db`: SQLite 存储总积分 (cert_total_points / award_total_points)。

## 奖项分类积分规则

| 分类 | 积分  |
|----|-----|
| 证书 | 0.2 |
| 国  | 0.8 |
| 省市 | 0.5 |
| 校  | 0.3 |
| 院  | 0.2 |
| 无  | 0.0 |

重复点击同一分类撤销积分；切换分类自动平衡旧分类扣分与新分类加分；首次录入增加已录入奖项数。

## 异常与日志

使用 `LoggerUtil.logException(logger, ex, message)` 统一输出：

- message | 异常类型: 异常信息
- 堆栈跟踪

## 测试

现有 `AppTest` 为占位测试；可扩展：

- AwardDialog 积分切换逻辑单元测试
- NewDataManager 读写回归测试

## 后续可选改进

- 国际化 (多语言切换)
- 奖项图片本地缓存减少重复网络请求
- 使用模块化 (Java Platform Module System) 明确导出/依赖

## 许可证

示例迁移项目，未附带版权文件，可自行添加 MIT 或 Apache-2.0。
