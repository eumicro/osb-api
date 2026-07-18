package io.osb.workflow.n8n;

import java.util.Map;

final class CommandJson {

    private CommandJson() {}

    static String of(
            String instanceId, String serviceId, String planId, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"instanceId\":\"").append(escape(instanceId)).append("\",");
        sb.append("\"serviceId\":\"").append(escape(serviceId)).append("\",");
        sb.append("\"planId\":\"").append(escape(planId)).append("\"");
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(",\"parameters\":").append(mapToJson(parameters));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) nested;
                sb.append(mapToJson(typed));
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
