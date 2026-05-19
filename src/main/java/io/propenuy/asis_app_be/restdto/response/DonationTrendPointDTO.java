package io.propenuy.asis_app_be.restdto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationTrendPointDTO {
    /** Kunci seri waktu, mis. {@code 2026-01}, {@code 2026-Q1}, {@code 2026}. */
    private String period;
    private String label;
    private BigDecimal totalDonation;
}
