package org.example.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.*;
import org.example.persistence.NewDataManager;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;
import org.example.image.ImageUtils;
import org.example.image.ImageLoader;
import org.example.image.ImageTransform;

import java.util.*;

/**
 * 奖项对话框 (逻辑未改)
 */
public class AwardDialog {
    private static final Logger LOGGER = LoggerUtil.getLogger(AwardDialog.class.getName());
    private final Stage stage;
    private final Student studentData;
    private final List<Award> awards;
    private final NewDataManager newDataManager;
    private final AwardDialogListener listener;
    private int currentAwardIndex = 0;
    private Label awardLabel;
    private Label indexLabel;
    private ImageView imageView;
    private StackPane imageContainer;
    private Label loadingLabel;
    private Button prevButton;
    private Button nextButton;
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private Image originalImage;
    private Image nextPreloadedImage;
    private Image prevPreloadedImage;
    private boolean imageLoading = false;
    private final ImageLoader imageLoader = new ImageLoader();
    private static final Map<String, Double> SCORE_MAP = new HashMap<>();

    static {
        SCORE_MAP.put("证书", 0.2);
        SCORE_MAP.put("国", 0.8);
        SCORE_MAP.put("省市", 0.5);
        SCORE_MAP.put("校", 0.3);
        SCORE_MAP.put("院", 0.2);
        SCORE_MAP.put("无", 0.0);
    }

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
        if (currentAwardIndex + 1 < awards.size()) {
            String nextUrl = awards.get(currentAwardIndex + 1).getImageUrl();
            imageLoader.loadImage(nextUrl, img -> nextPreloadedImage = img);
        } else nextPreloadedImage = null;
        if (currentAwardIndex - 1 >= 0) {
            String prevUrl = awards.get(currentAwardIndex - 1).getImageUrl();
            imageLoader.loadImage(prevUrl, img -> prevPreloadedImage = img);
        } else prevPreloadedImage = null;
        updateButtonStates();
        updateNavigationButtons();
    }

    private void adjustImageSize() {
        if (originalImage == null) return;
        double stageW = stage.getWidth();
        double stageH = stage.getHeight();
        if (stageW <= 0 || stageH <= 0) return;
        double availableW = Math.max(50, stageW - 40);
        double availableH = Math.max(50, stageH - 70 - 60 - 30 - 40);
        double imgW = originalImage.getWidth();
        double imgH = originalImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return;
        double scale = Math.min(availableW / imgW, availableH / imgH);
        imageView.setFitWidth(imgW * scale);
        imageView.setFitHeight(imgH * scale);
    }

    private void displayPlaceholder() {
        loadingLabel.setVisible(false);
        Image placeholder = new Image("https://via.placeholder.com/800x600", true);
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

    private void updateAwardPoints(String label) {
        StudentAwardRecord record = newDataManager.getOrCreateRecord(studentData.getStudentId(), studentData.getName(), studentData.getClassName());
        String previousLabel = record.getAwardLabel(currentAwardIndex);
        double scoreChange = 0.0;
        if (previousLabel.equals(label)) {
            if (label.equals("证书")) record.addCertTotalPoints(-SCORE_MAP.get(label));
            else if (!label.equals("无") && !label.isEmpty()) record.addAwardTotalPoints(-SCORE_MAP.get(label));
            record.setAwardLabel(currentAwardIndex, "");
            record.decrementRecordedAwardCount();
        } else {
            if (!previousLabel.isEmpty()) {
                if (previousLabel.equals("证书")) record.addCertTotalPoints(-SCORE_MAP.get(previousLabel));
                else if (!previousLabel.equals("无")) record.addAwardTotalPoints(-SCORE_MAP.get(previousLabel));
            }
            if (label.equals("证书")) {
                record.addCertTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            } else if (!label.equals("无")) {
                record.addAwardTotalPoints(SCORE_MAP.get(label));
                scoreChange = SCORE_MAP.get(label);
            }
            if (previousLabel.isEmpty() && !label.equals("无")) record.incrementRecordedAwardCount();
            record.setAwardLabel(currentAwardIndex, label);
        }
        newDataManager.persistRecord(record);
        updateButtonStates();
        if (listener != null) listener.onDataUpdated();
        LOGGER.debug("学生=" + studentData.getStudentId() + " 奖项索引=" + currentAwardIndex + " 标签=" + label + " 变化=" + scoreChange);
    }
}

