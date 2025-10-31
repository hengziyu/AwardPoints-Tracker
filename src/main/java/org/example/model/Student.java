package org.example.model;

import java.util.List;

/**
 * 学生模型
 */
public class Student {
    private long studentId;
    private String name;
    private String className;
    private List<Award> awards;
    private int totalAwards;

    public Student(long studentId, String name, String className, List<Award> awards) {
        this.studentId = studentId;
        this.name = name;
        this.className = className;
        this.awards = awards;
        this.totalAwards = awards != null ? awards.size() : 0;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long id) {
        this.studentId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String c) {
        this.className = c;
    }

    public List<Award> getAwards() {
        return awards;
    }

    public void setAwards(List<Award> a) {
        this.awards = a;
        this.totalAwards = a == null ? 0 : a.size();
    }

    public int getTotalAwards() {
        return totalAwards;
    }

    public void setTotalAwards(int t) {
        this.totalAwards = t;
    }
}

