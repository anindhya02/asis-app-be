package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExecutiveFinancialResponseDTO;
import io.propenuy.asis_app_be.restservice.EisExecutiveFinancialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eis")
@RequiredArgsConstructor
public class EisExecutiveFinancialRestController {

    private static final Logger logger = LoggerFactory.getLogger(EisExecutiveFinancialRestController.class);
    private final EisExecutiveFinancialService eisExecutiveFinancialService;

    @GetMapping("/executive-financial")
    public ResponseEntity<BaseResponseDTO<ExecutiveFinancialResponseDTO>> getExecutiveFinancial(
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        try {
            logger.info(
                    "EIS executive-financial requested period={} startDate={} endDate={}",
                    period, startDate, endDate
            );
            ExecutiveFinancialResponseDTO data = eisExecutiveFinancialService.getExecutiveFinancial(
                    period, startDate, endDate);
            return ResponseEntity.ok(
                    BaseResponseDTO.<ExecutiveFinancialResponseDTO>builder()
                            .status("success")
                            .message("Executive financial dashboard retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ExecutiveFinancialResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}
