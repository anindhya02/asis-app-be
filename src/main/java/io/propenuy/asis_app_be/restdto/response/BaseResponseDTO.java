package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponseDTO<T> {
    private String status;  // "success" atau "error"
    private String message; // penjelasan singkat
    private T data;         // data utama (bisa List atau Object)
}
