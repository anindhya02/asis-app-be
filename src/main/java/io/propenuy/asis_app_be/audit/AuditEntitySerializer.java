package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AuditEntitySerializer {

    private final ObjectMapper auditObjectMapper;

    public AuditEntitySerializer(@Qualifier("auditObjectMapper") ObjectMapper auditObjectMapper) {
        this.auditObjectMapper = auditObjectMapper;
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return auditObjectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"_auditSerializationError\":true,\"type\":\""
                    + value.getClass().getSimpleName()
                    + "\",\"message\":\""
                    + escapeJson(e.getMessage())
                    + "\"}";
        }
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
