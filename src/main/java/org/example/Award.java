package org.example;

/**
 * 代表一个奖项的数据模型。
 * 包含奖项名称、证书图片 URL 和分数。
 */
public class Award {

    private String name;
    private String imageUrl;
    private double score;

    public Award(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.score = 0.0; // 默认分数为 0
    }

    // --- Getters and Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
