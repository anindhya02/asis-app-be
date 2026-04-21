package io.propenuy.asis_app_be.restservice;

import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.request.CreateInventoryItemRequestDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemResponseDTO;

public interface InventoryRestService {
    InventoryItemListResponseDTO list(String search, int page, int limit);

    InventoryItemResponseDTO getById(String id);

    InventoryItemResponseDTO create(
            CreateInventoryItemRequestDTO request,
            MultipartFile photoFile,
            String currentUsername
    );
}
