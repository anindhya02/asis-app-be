package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.ExecutiveFinancialResponseDTO;

public interface EisExecutiveFinancialService {

    ExecutiveFinancialResponseDTO getExecutiveFinancial(String period, String startDateStr, String endDateStr);
}
