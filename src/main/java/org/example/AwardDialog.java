package org.example;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.LoggerUtil;
import org.slf4j.Logger;

/**
 * 奖项详情对话框 (一比一复原 award_dialog.py 核心功能)
 * 功能包括：
 *  - 奖项浏览 (上一张/下一张)
 *  - 图片异步加载 + 预加载前后图片
 *  - 图片旋转/镜像
 *  - 奖项分类按钮（证书/国/省市/校/院/无）积分逻辑，同步到内存 + Excel + SQLite
 *  - 按钮选中状态更新
 *
 * 不使用内部类；辅助类在同文件以包级可见类实现。
 */
public class AwardDialog {
    private static final Logger LOGGER = LoggerUtil.getLogger(AwardDialog.class.getName());

    private final Stage stage;
    private final Student studentData;
    private final List<Award> awards;
    private final NewDataManager newDataManager;
    private final AwardDialogListener listener;

    private int currentAwardIndex = 0;

    // UI Components
    private Label awardLabel;
    private Label indexLabel;
    private ImageView imageView;
    private StackPane imageContainer; // 新增：容器用于叠放图片与加载提示
    private Label loadingLabel;       // 新增：居中加载提示
    private Button prevButton;
    private Button nextButton;

    private final Map<String, Button> categoryButtons = new HashMap<>();

    // Image handling & preloading
    private Image originalImage;
    private Image nextPreloadedImage;
    private Image prevPreloadedImage;

    private boolean imageLoading = false;

    private final ImageLoader imageLoader = new ImageLoader();

    /**
     * 评分映射表
     */
    private static final Map<String, Double> SCORE_MAP = new HashMap<>();

    static {
        SCORE_MAP.put("证书", 0.2); // 证书总分
        SCORE_MAP.put("国", 0.8);
        SCORE_MAP.put("省市", 0.5);
        SCORE_MAP.put("校", 0.3);
        SCORE_MAP.put("院", 0.2);
        SCORE_MAP.put("无", 0.0); // 无不记分，仅用于 UI
    }

    /**
     * 构造函数
     *
     * @param owner          父窗口
     * @param studentData    学生对象
     * @param newDataManager 奖项记录与积分持久化管理
     * @param listener       更新回调（用于刷新父界面）
     */
    public AwardDialog(Stage owner, Student studentData, NewDataManager newDataManager, AwardDialogListener listener) {
        this.studentData = studentData;
        this.awards = studentData.getAwards();
        this.newDataManager = newDataManager;
        this.listener = listener;

        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("奖项详情 of 🍊🐟");
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        BorderPane root = createWidgets();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.widthProperty().addListener((o, ov, nv) -> adjustImageSize());
        stage.heightProperty().addListener((o, ov, nv) -> adjustImageSize());

        updateAwardDisplay();
    }

    /**
     * 显示对话框
     */
    public void show() {
        stage.show();
    }

    /**
     * 创建界面组件
     */
    private BorderPane createWidgets() {
        BorderPane root = new BorderPane();

        // Top Panel (赋值字段而非局部变量)
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setAlignment(Pos.CENTER_LEFT);

        createCategoryButton(topPanel, "证书");
        createCategoryButton(topPanel, "国");
        createCategoryButton(topPanel, "省市");
        createCategoryButton(topPanel, "校");
        createCategoryButton(topPanel, "院");
        createCategoryButton(topPanel, "无");

        indexLabel = new Label();

        Button rotateButton = new Button("翻转");
        rotateButton.setOnAction(e -> rotateImage());
        Button mirrorButton = new Button("镜像");
        mirrorButton.setOnAction(e -> mirrorImage());

        topPanel.getChildren().addAll(indexLabel, rotateButton, mirrorButton);
        root.setTop(topPanel);

        // Center Panel
        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setFillWidth(true);
        awardLabel = new Label();
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        loadingLabel = new Label("加载中...");
        loadingLabel.setStyle("-fx-font-size:16px;-fx-text-fill:#666;");
        loadingLabel.setVisible(false);
        imageContainer = new StackPane();
        imageContainer.setPadding(new Insets(5));
        imageContainer.getChildren().addAll(imageView, loadingLabel);
        StackPane.setAlignment(loadingLabel, Pos.CENTER);
        VBox.setVgrow(imageContainer, javafx.scene.layout.Priority.ALWAYS);
        imageContainer.widthProperty().addListener((o, ov, nv) -> adjustImageSize());
        imageContainer.heightProperty().addListener((o, ov, nv) -> adjustImageSize());
        centerPanel.getChildren().addAll(awardLabel, imageContainer);
        root.setCenter(centerPanel);

        // Bottom navigation (赋值字段)
        HBox navPanel = new HBox(10);
        navPanel.setPadding(new Insets(10));
        navPanel.setAlignment(Pos.CENTER);
        prevButton = new Button("上一张");
        prevButton.setOnAction(e -> prevAward());
        nextButton = new Button("下一张");
        nextButton.setOnAction(e -> nextAward());
        navPanel.getChildren().addAll(prevButton, nextButton);
        root.setBottom(navPanel);

        return root;
    }

    /**
     * 创建分类按钮并注册事件
     */
    private void createCategoryButton(HBox parent, String label) {
        Button btn = new Button(label);
        btn.setOnAction(e -> updateAwardPoints(label));
        btn.getStyleClass().add("category-button");
        categoryButtons.put(label, btn);
        parent.getChildren().add(btn);
    }

    /**
     * 更新当前奖项展示及按钮状态
     */
    private void updateAwardDisplay() {
        if (awards == null || awards.isEmpty()) {
            LOGGER.warn("学生 {} 无奖项", studentData.getStudentId());
            awardLabel.setText("该学生没有奖项");
            indexLabel.setText("0/0");
            disableNavigation();
            return;
        }
        Award currentAward = awards.get(currentAwardIndex);
        awardLabel.setText(currentAward.getName());
        indexLabel.setText(String.format("%d/%d", currentAwardIndex + 1, awards.size()));

        imageLoading = true;
        loadingLabel.setVisible(true);
        imageView.setImage(null);

        // 异步加载当前图片
        imageLoader.loadImage(currentAward.getImageUrl(), img -> Platform.runLater(() -> {
            imageLoading = false;
            if (img != null) {
                displayImage(img);
            } else {
                displayPlaceholder();
            }
            updateNavigationButtons();
        }));

        // 预加载下一张
        if (currentAwardIndex + 1 < awards.size()) {
            String nextUrl = awards.get(currentAwardIndex + 1).getImageUrl();
            imageLoader.loadImage(nextUrl, img -> nextPreloadedImage = img);
        } else {
            nextPreloadedImage = null;
        }
        // 预加载上一张
        if (currentAwardIndex - 1 >= 0) {
            String prevUrl = awards.get(currentAwardIndex - 1).getImageUrl();
            imageLoader.loadImage(prevUrl, img -> prevPreloadedImage = img);
        } else {
            prevPreloadedImage = null;
        }

        updateButtonStates();
        updateNavigationButtons();
    }

    // 重写尺寸逻辑：基于窗口剩余空间，不让图片挤走按钮
    private void adjustImageSize() {
        HBox topPanel = new HBox(10);
        HBox navPanel = new HBox(10);
        if (originalImage == null) return;
        double stageW = stage.getWidth();
        double stageH = stage.getHeight();
        if (stageW <= 0 || stageH <= 0) return; // 尚未布局完成
        double topH = (topPanel != null && topPanel.getHeight() > 0) ? topPanel.getHeight() : 70;
        double bottomH = (navPanel != null && navPanel.getHeight() > 0) ? navPanel.getHeight() : 60;
        double labelH = (awardLabel != null && awardLabel.getHeight() > 0) ? awardLabel.getHeight() : 30;
        double verticalPadding = 40; // 额外留白
        double horizontalPadding = 40;
        double availableW = Math.max(50, stageW - horizontalPadding);
        double availableH = Math.max(50, stageH - topH - bottomH - labelH - verticalPadding);
        if (availableH <= 0) return;
        double imgW = originalImage.getWidth();
        double imgH = originalImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return;
        double scale = Math.min(availableW / imgW, availableH / imgH);
        imageView.setFitWidth(imgW * scale);
        imageView.setFitHeight(imgH * scale);
    }

    /**
     * 显示占位图
     */
    private void displayPlaceholder() {
        loadingLabel.setVisible(false);
        Image placeholder = new Image("https://via.placeholder.com/800x600", true);
        this.originalImage = placeholder;
        imageView.setImage(placeholder);
        Platform.runLater(this::adjustImageSize); // 使用布局完成后的计算
    }

    /**
     * 把图片显示在 ImageView 中
     */
    private void displayImage(Image img) {
        this.originalImage = img;
        imageView.setImage(img);
        loadingLabel.setVisible(false);
        Platform.runLater(this::adjustImageSize);
    }

    /**
     * 按钮选中状态更新
     */
    private void updateButtonStates() {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        String currentLabel = record.getAwardLabel(currentAwardIndex);
        for (Map.Entry<String, Button> entry : categoryButtons.entrySet()) {
            String label = entry.getKey();
            Button btn = entry.getValue();
            if (label.equals(currentLabel)) {
                btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            } else {
                btn.setStyle("");
            }
        }
    }

    /**
     * 导航按钮状态更新
     */
    private void updateNavigationButtons() {
        prevButton.setDisable(currentAwardIndex == 0 || imageLoading);
        nextButton.setDisable(currentAwardIndex == awards.size() - 1 || imageLoading);
    }

    private void disableNavigation() {
        prevButton.setDisable(true);
        nextButton.setDisable(true);
    }

    /**
     * 图片旋转
     */
    private void rotateImage() {
        if (originalImage != null) {
            Image rotated = ImageTransform.rotateImage(originalImage, (imageView.getRotate() + 90) % 360);
            displayImage(rotated);
        }
    }

    /**
     * 图片镜像
     */
    private void mirrorImage() {
        if (originalImage != null) {
            Image mirrored = ImageTransform.mirrorImage(originalImage);
            displayImage(mirrored);
        }
    }

    /**
     * 上一张奖项
     */
    private void prevAward() {
        if (imageLoading) return;
        if (currentAwardIndex > 0) {
            currentAwardIndex--;
            if (prevPreloadedImage != null) {
                displayImage(prevPreloadedImage);
                imageLoading = false;
            }
            updateAwardDisplay();
        }
    }

    /**
     * 下一张奖项
     */
    private void nextAward() {
        if (imageLoading) return;
        if (currentAwardIndex < awards.size() - 1) {
            currentAwardIndex++;
            if (nextPreloadedImage != null) {
                displayImage(nextPreloadedImage);
                imageLoading = false;
            }
            updateAwardDisplay();
        }
    }

    /**
     * 更新积分与记录（完整复原 Python 逻辑）
     */
    private void updateAwardPoints(String label) {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        String previousLabel = record.getAwardLabel(currentAwardIndex);
        double scoreChange = 0.0;

        if (previousLabel.equals(label)) {
            // Same label clicked -> remove
            if (label.equals("证书")) {
                record.addCertTotalPoints(-SCORE_MAP.get(label));
            } else if (!label.equals("无") && !label.isEmpty()) {
                record.addAwardTotalPoints(-SCORE_MAP.get(label));
            }
            record.setAwardLabel(currentAwardIndex, "");
            record.decrementRecordedAwardCount();
        } else {
            // Replace existing label
            if (!previousLabel.isEmpty()) {
                if (previousLabel.equals("证书")) {
                    record.addCertTotalPoints(-SCORE_MAP.get(previousLabel));
                } else if (!previousLabel.equals("无")) {
                    record.addAwardTotalPoints(-SCORE_MAP.get(previousLabel));
                }
            }
            if (label.equals("证书")) {
                record.addCertTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            } else if (!label.equals("无")) {
                record.addAwardTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            }
            if (previousLabel.isEmpty() && !label.equals("无")) { // '无' 不计入已录入奖项数
                record.incrementRecordedAwardCount();
            }
            record.setAwardLabel(currentAwardIndex, label.equals("无") ? "" : label);
        }

        Award currentAward = awards.get(currentAwardIndex);
        currentAward.setScore(scoreChange);

        newDataManager.persistRecord(record); // handles writing to DB via internal method
        newDataManager.saveAll();

        if (listener != null) listener.onDataUpdated();
        updateButtonStates();
    }
}
