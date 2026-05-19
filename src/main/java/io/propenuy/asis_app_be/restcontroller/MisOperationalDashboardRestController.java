package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.OperationalDashboardResponseDTO;
import io.propenuy.asis_app_be.restservice.OperationalDashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mis")
@RequiredArgsConstructor
public class MisOperationalDashboardRestController {

    private static final Logger logger = LoggerFactory.getLogger(MisOperationalDashboardRestController.class);
    private final OperationalDashboardService operationalDashboardService;

    @GetMapping("/operational-dashboard")
    public ResponseEntity<BaseResponseDTO<OperationalDashboardResponseDTO>> getOperationalDashboard(
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month
    ) {
        try {
            logger.info("MIS operational-dashboard requested period={} year={} month={}", period, year, month);
            OperationalDashboardResponseDTO data = operationalDashboardService.getOperationalDashboard(period, year, month);
            logger.info(
                    "MIS operational-dashboard success period={} year={} month={} totalFundIn={} totalFundOut={} runningBalance={} ",
                    data.getPeriod(), data.getYear(), data.getMonth(),
                    data.getTotalFundIn(), data.getTotalFundOut(), data.getRunningBalance()
            );
            return ResponseEntity.ok(
                    BaseResponseDTO.<OperationalDashboardResponseDTO>builder()
                            .status("success")
                            .message("Operational dashboard retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<OperationalDashboardResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}
