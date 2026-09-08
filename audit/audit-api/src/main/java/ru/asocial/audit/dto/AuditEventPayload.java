package ru.asocial.audit.dto;

public record AuditEventPayload(
        String eventType,
        String username,
        String ipAddress,
        String metadata,
        String userRole) {

    public static AuditEventPayload forLogin(String username, String ipAddress, String userRole) {
        return new AuditEventPayload("LOGIN_SUCCESSFUL", username, ipAddress, null, userRole);
    }

    public static AuditEventPayload forTaskCreated(Long taskId, String taskType, String parameters,
                                                   String username, String ipAddress, String userRole) {
        StringBuilder metadata = new StringBuilder("{\"taskId\":");
        metadata.append(taskId);
        metadata.append(",\"taskType\":\"");
        metadata.append(jsonEscape(taskType));
        metadata.append("\"");
        if (parameters != null && !parameters.isBlank()) {
            metadata.append(",\"parameters\":\"");
            metadata.append(jsonEscape(parameters));
            metadata.append("\"");
        }
        metadata.append("}");
        return new AuditEventPayload("TASK_CREATED", username, ipAddress, metadata.toString(), userRole);
    }

    public static AuditEventPayload forTaskStatusChanged(Long taskId, String from, String to,
                                                         String username, String ipAddress, String userRole) {
        String metadata = String.format("{\"taskId\":%d,\"from\":\"%s\",\"to\":\"%s\"}", taskId, from, to);
        return new AuditEventPayload("TASK_STATUS_CHANGED", username, ipAddress, metadata, userRole);
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
