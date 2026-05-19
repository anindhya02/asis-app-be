package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.AuditLogPageDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restservice.AuditLogRestService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auditlog")
@RequiredArgsConstructor
public class AuditLogRestController {

    private final AuditLogRestService auditLogRestService;

    @GetMapping
    public ResponseEntity<BaseResponseDTO<AuditLogPageDTO>> list(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        AuditLogPageDTO data = auditLogRestService.getAuditLogs(from, to, actionType, module, user, page, size);
        return ResponseEntity.ok(
                BaseResponseDTO.<AuditLogPageDTO>builder()
                        .status("success")
                        .message("Audit log retrieved successfully")
                        .data(data)
                        .build()
        );
    }
}
