package org.example;

import java.util.Arrays;

/**
 * 学生奖项记录 (原 award_dialog.py 中行结构) 分离为独立类。
 */
public class StudentAwardRecord {
    private final long studentId;
    private final String name;
    private final String className;
    private double certTotalPoints = 0.0; // 证书总分
    private double awardTotalPoints = 0.0; // 奖项总分
    private int recordedAwardCount = 0;    // 已录入奖项数
    private final String[] awardLabels = new String[50]; // 奖项1..50 分类标签

    public StudentAwardRecord(long studentId, String name, String className) {
        this.studentId = studentId;
        this.name = name;
        this.className = className;
        Arrays.fill(awardLabels, "");
    }

    public long getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getClassName() { return className; }
    public double getCertTotalPoints() { return certTotalPoints; }
    public double getAwardTotalPoints() { return awardTotalPoints; }
    public int getRecordedAwardCount() { return recordedAwardCount; }

    public void addCertTotalPoints(double delta) { certTotalPoints += delta; }
    public void addAwardTotalPoints(double delta) { awardTotalPoints += delta; }
    public void incrementRecordedAwardCount() { recordedAwardCount++; }
    public void decrementRecordedAwardCount() { if (recordedAwardCount > 0) recordedAwardCount--; }

    public String getAwardLabel(int index) { return awardLabels[index]; }
    public void setAwardLabel(int index, String label) { awardLabels[index] = label; }

    public String[] getAwardLabels() { return awardLabels; }
}

