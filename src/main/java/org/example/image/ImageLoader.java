package org.example.image;

import javafx.application.Platform;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageLoader {
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
