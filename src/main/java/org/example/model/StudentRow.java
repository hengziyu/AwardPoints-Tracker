package org.example.model;

/**
 * 表格行模型
 */
public class StudentRow {
    private final long studentId;
    private final String name;
    private final String clazz;
    private final String progress;

    public StudentRow(long id, String name, String clazz, String progress) {
        this.studentId = id;
        this.name = name;
        this.clazz = clazz;
        this.progress = progress;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getClazz() {
        return clazz;
    }

    public String getProgress() {
        return progress;
    }
}

