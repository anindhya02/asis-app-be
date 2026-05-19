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
public class DonationGrowthDTO {
    /**
     * Persentase perubahan vs periode pembanding. Null jika basis pembanding 0 dan donasi
     * saat ini > 0 (pertumbuhan tak terhingga — tidak dipaksakan ke angka).
     */
    private BigDecimal percentage;
    private String direction;
}
