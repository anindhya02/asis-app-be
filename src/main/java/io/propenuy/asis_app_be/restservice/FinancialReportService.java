package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.FinancialReportResponseDTO;

public interface FinancialReportService {

    FinancialReportResponseDTO getFinancialReport(
            String period,
            int year,
            Integer month,
            Integer quarter,
            String categoryIds,
            String programId
    );
}
