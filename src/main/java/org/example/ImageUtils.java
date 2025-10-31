package org.example;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.LoggerUtil;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 迁移自 image_utils.py
 * 功能：
 *  - 异步图片加载 (loadImage)
 *  - resizeImage 等比缩放
 *  - rotateImage 旋转
 *  - mirrorImage 镜像（水平翻转）
 * 不再使用 SwingFXUtils/BufferedImage，避免额外模块依赖。
 */
public class ImageUtils { /* 占位命名空间 */ }

class ImageLoader {
    private static final Logger LOGGER = LoggerUtil.getLogger(ImageLoader.class.getName());
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void loadImage(String url, Consumer<Image> callback) {
        if (url == null || url.isEmpty()) {
            Platform.runLater(() -> callback.accept(null));
            return;
        }
        executor.submit(() -> {
            Image image = null;
            try {
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        try (InputStream is = response.body().byteStream()) {
                            image = new Image(is);
                        }
                    }
                }
            } catch (Exception ex) {
                LoggerUtil.logException(LOGGER, ex, "Error loading image from " + url);
            }
            Image finalImage = image;
            Platform.runLater(() -> callback.accept(finalImage));
        });
    }
}

class ImageTransform {
    /** 等比缩放图片到指定区域（返回新 Image） */
    public static Image resizeImage(Image src, int targetW, int targetH) {
        if (src == null || targetW <= 0 || targetH <= 0) return src;
        double aspect = src.getWidth() / src.getHeight();
        int newW, newH;
        if ((double) targetW / targetH > aspect) {
            newH = targetH;
            newW = (int) (newH * aspect);
        } else {
            newW = targetW;
            newH = (int) (newW / aspect);
        }
        Canvas canvas = new Canvas(newW, newH);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(src, 0, 0, newW, newH);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return canvas.snapshot(sp, null);
    }

    /** 旋转图片（返回新快照） */
    public static Image rotateImage(Image src, double degrees) {
        if (src == null) return null;
        ImageView view = new ImageView(src);
        view.setRotate(degrees);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return view.snapshot(sp, null);
    }

    /** 水平镜像图片（返回新快照） */
    public static Image mirrorImage(Image src) {
        if (src == null) return null;
        ImageView view = new ImageView(src);
        view.setScaleX(-1); // 水平翻转
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return view.snapshot(sp, null);
    }
}
