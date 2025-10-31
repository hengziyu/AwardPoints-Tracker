package org.example.model;

/** 奖项模型 */
public class Award {
    private String name; private String imageUrl; private double score;
    public Award(String name, String imageUrl){ this.name=name; this.imageUrl=imageUrl; this.score=0.0; }
    public String getName(){ return name; } public void setName(String name){ this.name=name; }
    public String getImageUrl(){ return imageUrl; } public void setImageUrl(String imageUrl){ this.imageUrl=imageUrl; }
    public double getScore(){ return score; } public void setScore(double score){ this.score=score; }
}

