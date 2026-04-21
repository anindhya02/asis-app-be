package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.FinancialReportResponseDTO;
import io.propenuy.asis_app_be.restservice.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mis")
@RequiredArgsConstructor
public class MisFinancialReportRestController {

    private final FinancialReportService financialReportService;

    @GetMapping("/financial-report")
    public ResponseEntity<BaseResponseDTO<FinancialReportResponseDTO>> getFinancialReport(
            @RequestParam("period") String period,
            @RequestParam("year") int year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "quarter", required = false) Integer quarter,
            @RequestParam(value = "categoryIds", required = false) String categoryIds,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "programId", required = false) String programId
    ) {
        try {
            String categoryParam = (categoryIds != null && !categoryIds.isBlank()) ? categoryIds : categoryId;
            FinancialReportResponseDTO data = financialReportService.getFinancialReport(
                    period, year, month, quarter, categoryParam, programId);
            return ResponseEntity.ok(
                    BaseResponseDTO.<FinancialReportResponseDTO>builder()
                            .status("success")
                            .message("Financial report retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<FinancialReportResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}
