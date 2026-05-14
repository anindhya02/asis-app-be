package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.OperationalDashboardResponseDTO;

public interface OperationalDashboardService {
    OperationalDashboardResponseDTO getOperationalDashboard(String period, Integer year, Integer month);
}
