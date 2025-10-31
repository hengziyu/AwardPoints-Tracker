package org.example.image;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class ImageTransform {
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

    public static Image rotateImage(Image src, double degrees) {
        if (src == null) return null;
        ImageView view = new ImageView(src);
        view.setRotate(degrees);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return view.snapshot(sp, null);
    }

    public static Image mirrorImage(Image src) {
        if (src == null) return null;
        ImageView view = new ImageView(src);
        view.setScaleX(-1);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return view.snapshot(sp, null);
    }
}
