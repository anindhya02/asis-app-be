package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.propenuy.asis_app_be.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Snapshot ringkas untuk audit User Management (tanpa password).
 */
@Component
public class UserAuditSnapshot {

    private final ObjectMapper objectMapper;

    public UserAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(User user, Boolean passwordChanged) {
        if (user == null) {
            return null;
        }
        return toCompactJson(user.getNama(), user.getUsername(), user.getRole(), passwordChanged);
    }

    public String toJson(UserAuditRecorder.BeforeState before) {
        if (before == null) {
            return null;
        }
        return toCompactJson(before.nama(), before.username(), before.role(), null);
    }

    private String toCompactJson(String nama, String username, String role, Boolean passwordChanged) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (nama != null) {
                node.put("nama", nama);
            }
            if (username != null) {
                node.put("username", username);
            }
            if (role != null) {
                node.put("role", role);
            }
            if (passwordChanged != null) {
                node.put("passwordChanged", passwordChanged);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    public String compactFromFullJson(String fullJson) {
        if (fullJson == null || fullJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(fullJson);
            ObjectNode node = objectMapper.createObjectNode();
            putText(node, "nama", root.get("nama"));
            putText(node, "username", root.get("username"));
            putText(node, "role", root.get("role"));
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private static void putText(ObjectNode target, String field, JsonNode source) {
        if (source != null && !source.isNull() && source.isValueNode()) {
            target.put(field, source.asText());
        }
    }

    public UUID extractUserId(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode id = root.get("userId");
            if (id == null || id.isNull()) {
                return null;
            }
            return UUID.fromString(id.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
