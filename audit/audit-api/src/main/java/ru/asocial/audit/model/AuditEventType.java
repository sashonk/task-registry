package ru.asocial.audit.model;

public enum AuditEventType {

    LOGIN_SUCCESSFUL("Успешный вход"),
    TASK_CREATED("Задача создана"),
    TASK_STATUS_CHANGED("Статус задачи изменён");

    private final String displayName;

    AuditEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
