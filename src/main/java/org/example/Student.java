package org.example;

import java.util.List;

/**
 * 代表一个学生的数据模型。
 * 用于替代原 Python 项目中的 Pandas DataFrame 行。
 */
public class Student {

    private long studentId;
    private String name;
    private String className;
    private List<Award> awards;
    private int totalAwards; // 总奖项数

    public Student(long studentId, String name, String className, List<Award> awards) {
        this.studentId = studentId;
        this.name = name;
        this.className = className;
        this.awards = awards;
        this.totalAwards = awards != null ? awards.size() : 0;
    }

    // --- Getters and Setters ---

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Award> getAwards() {
        return awards;
    }

    public void setAwards(List<Award> awards) {
        this.awards = awards;
        this.totalAwards = (awards == null) ? 0 : awards.size(); // ensure totalAwards stays in sync
    }

    public int getTotalAwards() {
        return totalAwards;
    }

    public void setTotalAwards(int totalAwards) {
        this.totalAwards = totalAwards;
    }
}
