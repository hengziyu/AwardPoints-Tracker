package org.example;

/**
 * 单独的表格展示行数据模型，原先内嵌于 App。
 */
public class StudentRow {
    private final long studentId;
    private final String name;
    private final String clazz;
    private final String progress;

    public StudentRow(long studentId, String name, String clazz, String progress) {
        this.studentId = studentId;
        this.name = name;
        this.clazz = clazz;
        this.progress = progress;
    }
    public long getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getClazz() { return clazz; }
    public String getProgress() { return progress; }
}

