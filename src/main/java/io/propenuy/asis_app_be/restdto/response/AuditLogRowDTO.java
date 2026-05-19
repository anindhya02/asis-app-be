package io.propenuy.asis_app_be.restdto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRowDTO {
    private String id;
    private LocalDateTime occurredAt;
    private String actionType;
    private String moduleCode;
    private String moduleLabel;
    private String entityClassName;
    private String entityId;
    private String oldValueJson;
    private String newValueJson;
    private String actorUsername;
    private String actorNama;
    private String actorRole;
}
