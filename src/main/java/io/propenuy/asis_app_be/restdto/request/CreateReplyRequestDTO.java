package io.propenuy.asis_app_be.restdto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequestDTO {

    private String content;
    private UUID parentReplyId;
}
