package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 学生奖项记录
 */
public class StudentAwardRecord {
    private final long studentId;
    private final String name;
    private final String className;
    private final List<Map<String, String>> awards = new ArrayList<>();
    private double certTotalPoints = 0.0;
    private double awardTotalPoints = 0.0;
    private int recordedAwardCount = 0;

    public StudentAwardRecord(long studentId, String name, String className) {
        this.studentId = studentId;
        this.name = name;
        this.className = className;
    }

    public void addAward(String awardName, String imageUrl, String category) {
        this.awards.add(Map.of("name", awardName, "image", imageUrl, "category", category));
    }

    public List<Map<String, String>> getAwards() {
        return awards;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public double getCertTotalPoints() {
        return certTotalPoints;
    }

    public double getAwardTotalPoints() {
        return awardTotalPoints;
    }

    public void setCertTotalPoints(double certTotalPoints) {
        this.certTotalPoints = certTotalPoints;
    }

    public void setAwardTotalPoints(double awardTotalPoints) {
        this.awardTotalPoints = awardTotalPoints;
    }

    public int getRecordedAwardCount() {
        return recordedAwardCount;
    }

    public void setRecordedAwardCount(int count) {
        this.recordedAwardCount = count;
    }

    public void addCertTotalPoints(double d) {
        certTotalPoints += d;
    }

    public void addAwardTotalPoints(double d) {
        awardTotalPoints += d;
    }

    public void incrementRecordedAwardCount() {
        recordedAwardCount++;
    }

    public void decrementRecordedAwardCount() {
        if (recordedAwardCount > 0) recordedAwardCount--;
    }

    public String getAwardLabel(int i) {
        if (i >= 0 && i < awards.size()) {
            return awards.get(i).get("category");
        }
        return "";
    }

    public void setAwardLabel(int i, String l) {
        if (i >= 0 && i < awards.size()) {
            Map<String, String> award = new java.util.HashMap<>(awards.get(i));
            award.put("category", l);
            awards.set(i, award);
        }
    }
}
