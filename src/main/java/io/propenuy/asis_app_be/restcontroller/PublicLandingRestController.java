package io.propenuy.asis_app_be.restcontroller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PublicLandingResponseDTO;

@RestController
public class PublicLandingRestController {

    private static final PublicLandingResponseDTO PUBLIC_PAYLOAD = PublicLandingResponseDTO.builder()
            .applicationName("ASIS (Ash-Sholati Information System)")
            .summary(
                    "Sistem informasi yayasan untuk mendukung transparansi program "
                            + "dan pengelolaan kegiatan publik yang aman melalui aplikasi web resmi."
            )
            .build();

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponseDTO<PublicLandingResponseDTO>> getPublicLanding() {
        return ResponseEntity.ok(
                BaseResponseDTO.<PublicLandingResponseDTO>builder()
                        .status("success")
                        .message("Landing publik ASIS")
                        .data(PUBLIC_PAYLOAD)
                        .build()
        );
    }
}
