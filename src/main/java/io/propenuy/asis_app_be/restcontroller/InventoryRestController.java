package io.propenuy.asis_app_be.restcontroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.propenuy.asis_app_be.restdto.request.CreateInventoryItemRequestDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemResponseDTO;
import io.propenuy.asis_app_be.restservice.InventoryRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryRestController {
    private final InventoryRestService inventoryRestService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<BaseResponseDTO<InventoryItemListResponseDTO>> list(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        try {
            InventoryItemListResponseDTO data = inventoryRestService.list(search, page, limit);
            return ResponseEntity.ok(
                    BaseResponseDTO.<InventoryItemListResponseDTO>builder()
                            .status("success")
                            .message("Daftar item inventory berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<InventoryItemListResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<InventoryItemListResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<InventoryItemResponseDTO>> getById(
            @PathVariable("id") String id
    ) {
        try {
            InventoryItemResponseDTO data = inventoryRestService.getById(id);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        BaseResponseDTO.<InventoryItemResponseDTO>builder()
                                .status("error")
                                .message("Item inventory tidak ditemukan")
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("success")
                            .message("Detail item inventory berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<InventoryItemResponseDTO>> create(
            @RequestParam(value = "itemName", required = false) String itemName,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "donorSource", required = false) String donorSource,
            @RequestParam(value = "photoUrl", required = false) String photoUrl,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "quantity", required = false) String quantity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "breakdownsList", required = false) String breakdownsListJson,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<InventoryItemResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            CreateInventoryItemRequestDTO request = CreateInventoryItemRequestDTO.builder()
                    .itemName(itemName)
                    .category(category)
                    .donorSource(donorSource)
                    .photoUrl(photoUrl)
                    .quantity(quantity)
                    .unit(unit)
                    .breakdownsList(parseBreakdownsList(breakdownsListJson))
                    .note(note)
                    .build();

            InventoryItemResponseDTO response = inventoryRestService.create(request, photoFile, currentUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("success")
                            .message("Item inventory berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<InventoryItemResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    private List<CreateInventoryItemRequestDTO.BreakdownRequestDTO> parseBreakdownsList(String breakdownsListJson) {
        if (breakdownsListJson == null || breakdownsListJson.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> rawRows = objectMapper.readValue(
                    breakdownsListJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<CreateInventoryItemRequestDTO.BreakdownRequestDTO> parsed = new ArrayList<>();
            for (Map<String, Object> row : rawRows) {
                String name = row == null || row.get("name") == null ? null : String.valueOf(row.get("name"));
                String amount = row == null || row.get("amount") == null ? null : String.valueOf(row.get("amount"));
                parsed.add(CreateInventoryItemRequestDTO.BreakdownRequestDTO.builder()
                        .name(name)
                        .amount(amount)
                        .build());
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Format breakdownsList tidak valid");
        }
    }
}
