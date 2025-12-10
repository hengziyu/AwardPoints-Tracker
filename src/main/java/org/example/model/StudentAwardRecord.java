package org.example.model;

import java.util.Arrays;

/**
 * 学生奖项记录
 */
public class StudentAwardRecord {
    private final long studentId;
    private final String name;
    private final String className;
    private final String[] awardLabels = new String[50];
    private double certTotalPoints = 0.0;
    private double awardTotalPoints = 0.0;
    private int recordedAwardCount = 0;

    public StudentAwardRecord(long studentId, String name, String className) {
        this.studentId = studentId;
        this.name = name;
        this.className = className;
        Arrays.fill(awardLabels, "");
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

    public int getRecordedAwardCount() {
        return recordedAwardCount;
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
        return awardLabels[i];
    }

    public void setAwardLabel(int i, String l) {
        awardLabels[i] = l;
    }

    public String[] getAwardLabels() {
        return awardLabels;
    }

    public void setCertTotalPoints(double v) {
        this.certTotalPoints = v;
    }

    public void setAwardTotalPoints(double v) {
        this.awardTotalPoints = v;
    }

    public void setRecordedAwardCount(int v) {
        this.recordedAwardCount = v;
    }

    public String[] copyAwardLabels() {
        return awardLabels.clone();
    }
}
