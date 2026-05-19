package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.AuditLogPageDTO;

import java.time.LocalDate;

public interface AuditLogRestService {

    AuditLogPageDTO getAuditLogs(
            LocalDate fromDate,
            LocalDate toDate,
            String actionType,
            String moduleCode,
            String userSearch,
            int page,
            int size
    );
}
