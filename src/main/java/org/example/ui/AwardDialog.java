package org.example.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.config.Config;
import org.example.image.ImageLoader;
import org.example.image.ImageTransform;
import org.example.model.Award;
import org.example.model.Student;
import org.example.model.StudentAwardRecord;
import org.example.persistence.NewDataManager;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 奖项对话框 (逻辑未改)
 */
public class AwardDialog {
    private static final Logger LOGGER = LoggerUtil.getLogger(AwardDialog.class.getName());
    private static final Map<String, Double> SCORE_MAP = new HashMap<>();

    static {
        SCORE_MAP.put(Config.CATEGORY_CERT, Config.SCORE_CERT);
        SCORE_MAP.put(Config.CATEGORY_NATIONAL, Config.SCORE_NATIONAL);
        SCORE_MAP.put(Config.CATEGORY_PROVINCE_CITY, Config.SCORE_PROVINCE_CITY);
        SCORE_MAP.put(Config.CATEGORY_SCHOOL, Config.SCORE_SCHOOL);
        SCORE_MAP.put(Config.CATEGORY_COLLEGE, Config.SCORE_COLLEGE);
        SCORE_MAP.put(Config.CATEGORY_NONE, Config.SCORE_NONE);
    }

    private final Stage stage;
    private final Student studentData;
    private final List<Award> awards;
    private final NewDataManager newDataManager;
    private final AwardDialogListener listener;
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private final ImageLoader imageLoader = new ImageLoader();
    private int currentAwardIndex = 0;
    private Label awardLabel;
    private Label indexLabel;
    private ImageView imageView;
    private StackPane imageContainer;
    private Label loadingLabel;
    private Button prevButton;
    private Button nextButton;
    private Image originalImage;
    private Image nextPreloadedImage;
    private Image prevPreloadedImage;
    private boolean imageLoading = false;
    private Label selectedCountLabel; // 已选择数量显示

    public AwardDialog(Stage owner, Student studentData, NewDataManager newDataManager, AwardDialogListener listener) {
        this.studentData = studentData;
        this.awards = studentData.getAwards();
        this.newDataManager = newDataManager;
        this.listener = listener;
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("奖项详情");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        BorderPane root = createWidgets();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.widthProperty().addListener((o, ov, nv) -> adjustImageSize());
        stage.heightProperty().addListener((o, ov, nv) -> adjustImageSize());
        updateAwardDisplay();
    }

    public void show() {
        stage.show();
    }

    private BorderPane createWidgets() {
        BorderPane root = new BorderPane();
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setAlignment(Pos.CENTER_LEFT);
        createCategoryButton(topPanel, Config.CATEGORY_CERT);
        createCategoryButton(topPanel, Config.CATEGORY_NATIONAL);
        createCategoryButton(topPanel, Config.CATEGORY_PROVINCE_CITY);
        createCategoryButton(topPanel, Config.CATEGORY_SCHOOL);
        createCategoryButton(topPanel, Config.CATEGORY_COLLEGE);
        createCategoryButton(topPanel, Config.CATEGORY_NONE);
        indexLabel = new Label();
        selectedCountLabel = new Label();
        Button rotateButton = new Button("翻转");
        rotateButton.setOnAction(e -> rotateImage());
        Button mirrorButton = new Button("镜像");
        mirrorButton.setOnAction(e -> mirrorImage());
        topPanel.getChildren().addAll(indexLabel, selectedCountLabel, rotateButton, mirrorButton);
        root.setTop(topPanel);
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
        VBox.setVgrow(imageContainer, Priority.ALWAYS);
        imageContainer.widthProperty().addListener((o, ov, nv) -> adjustImageSize());
        imageContainer.heightProperty().addListener((o, ov, nv) -> adjustImageSize());
        centerPanel.getChildren().addAll(awardLabel, imageContainer);
        root.setCenter(centerPanel);
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

    private void createCategoryButton(HBox parent, String label) {
        Button btn = new Button(label);
        btn.setOnAction(e -> updateAwardPoints(label));
        btn.getStyleClass().add("category-button");
        categoryButtons.put(label, btn);
        parent.getChildren().add(btn);
    }

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
        imageLoader.loadImage(currentAward.getImageUrl(), img -> Platform.runLater(() -> {
            imageLoading = false;
            if (img != null) displayImage(img);
            else displayPlaceholder();
            updateNavigationButtons();
        }));
        if (Config.ENABLE_IMAGE_PRELOAD && currentAwardIndex + 1 < awards.size()) {
            String nextUrl = awards.get(currentAwardIndex + 1).getImageUrl();
            imageLoader.loadImage(nextUrl, img -> nextPreloadedImage = img);
        } else nextPreloadedImage = null;
        if (Config.ENABLE_IMAGE_PRELOAD && currentAwardIndex - 1 >= 0) {
            String prevUrl = awards.get(currentAwardIndex - 1).getImageUrl();
            imageLoader.loadImage(prevUrl, img -> prevPreloadedImage = img);
        } else prevPreloadedImage = null;
        updateButtonStates();
        updateNavigationButtons();
        updateSelectedCountLabel();
    }

    private void adjustImageSize() {
        if (originalImage == null) return;
        double stageW = stage.getWidth();
        double stageH = stage.getHeight();
        if (stageW <= 0 || stageH <= 0) return;
        double availableW = Math.max(Config.IMAGE_MIN_FIT_WIDTH, stageW - Config.IMAGE_MIN_PADDING);
        double availableH = Math.max(Config.IMAGE_MIN_FIT_HEIGHT, stageH - Config.IMAGE_MIN_VERTICAL_RESERVED);
        double imgW = originalImage.getWidth();
        double imgH = originalImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return;
        double scale = Math.min(availableW / imgW, availableH / imgH);
        imageView.setFitWidth(imgW * scale);
        imageView.setFitHeight(imgH * scale);
    }

    private void displayPlaceholder() {
        loadingLabel.setVisible(false);
        Image placeholder = new Image(Config.PLACEHOLDER_IMAGE, true);
        this.originalImage = placeholder;
        imageView.setImage(placeholder);
        Platform.runLater(this::adjustImageSize);
    }

    private void displayImage(Image img) {
        this.originalImage = img;
        imageView.setImage(img);
        loadingLabel.setVisible(false);
        Platform.runLater(this::adjustImageSize);
    }

    private void updateButtonStates() {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        String currentLabel = record.getAwardLabel(currentAwardIndex);
        for (Map.Entry<String, Button> e : categoryButtons.entrySet()) {
            String label = e.getKey();
            Button btn = e.getValue();
            if (label.equals(currentLabel)) btn.setStyle("-fx-background-color:#4CAF50;-fx-text-fill:white;");
            else btn.setStyle("");
        }
    }

    private void updateNavigationButtons() {
        prevButton.setDisable(currentAwardIndex == 0 || imageLoading);
        nextButton.setDisable(currentAwardIndex == awards.size() - 1 || imageLoading);
    }

    private void disableNavigation() {
        prevButton.setDisable(true);
        nextButton.setDisable(true);
    }

    private void rotateImage() {
        if (originalImage != null) {
            Image rotated = ImageTransform.rotateImage(originalImage, (imageView.getRotate() + 90) % 360);
            displayImage(rotated);
        }
    }

    private void mirrorImage() {
        if (originalImage != null) {
            Image mirrored = ImageTransform.mirrorImage(originalImage);
            displayImage(mirrored);
        }
    }

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

    private void updateSelectedCountLabel() {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        selectedCountLabel.setText("已选:" + record.getRecordedAwardCount());
    }

    private void updateAwardPoints(String label) {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        String previousLabel = record.getAwardLabel(currentAwardIndex);
        boolean isToggleOff = previousLabel.equals(label);
        double scoreChange = 0.0;
        if (isToggleOff) {
            // 撤销选择
            if (previousLabel.equals(Config.CATEGORY_CERT)) {
                record.addCertTotalPoints(-SCORE_MAP.get(previousLabel));
                scoreChange = -SCORE_MAP.get(previousLabel);
            } else if (!previousLabel.equals(Config.CATEGORY_NONE) && !previousLabel.isEmpty()) {
                record.addAwardTotalPoints(-SCORE_MAP.get(previousLabel));
                scoreChange = -SCORE_MAP.get(previousLabel);
            }
            record.setAwardLabel(currentAwardIndex, "");
            record.decrementRecordedAwardCount();
        } else {
            // 切换或新选择
            if (!previousLabel.isEmpty()) {
                // 旧标签扣分（“无”没有分值无需处理）
                if (previousLabel.equals(Config.CATEGORY_CERT)) {
                    record.addCertTotalPoints(-SCORE_MAP.get(previousLabel));
                } else if (!previousLabel.equals(Config.CATEGORY_NONE)) {
                    record.addAwardTotalPoints(-SCORE_MAP.get(previousLabel));
                }
            } else {
                // 从空到一个新标签（包括“无”）计数+1
                record.incrementRecordedAwardCount();
            }
            // 添加新标签分值（“无”不加分）
            if (label.equals(Config.CATEGORY_CERT)) {
                record.addCertTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            } else if (!label.equals(Config.CATEGORY_NONE)) {
                record.addAwardTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            }
            record.setAwardLabel(currentAwardIndex, label); // “无”也存储，便于撤销
        }
        newDataManager.persistRecord(record);
        updateButtonStates();
        updateSelectedCountLabel();
        if (listener != null) listener.onDataUpdated();
        LOGGER.debug("学生=" + studentData.getStudentId() + " 奖项索引=" + currentAwardIndex + " 标签=" + label + " 变化=" + scoreChange + (isToggleOff ? " (撤销)" : ""));
    }
}
