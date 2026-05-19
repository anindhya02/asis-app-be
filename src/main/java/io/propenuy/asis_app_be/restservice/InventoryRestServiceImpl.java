package io.propenuy.asis_app_be.restservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.propenuy.asis_app_be.audit.InventoryItemAuditRecorder;
import io.propenuy.asis_app_be.model.InventoryItem;
import io.propenuy.asis_app_be.model.InventoryItemBreakdown;
import io.propenuy.asis_app_be.model.InventoryUsageLog;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.InventoryCategory;
import io.propenuy.asis_app_be.model.enums.InventoryUnit;
import io.propenuy.asis_app_be.repository.InventoryItemRepository;
import io.propenuy.asis_app_be.repository.InventoryUsageLogRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.request.CreateInventoryItemRequestDTO;
import io.propenuy.asis_app_be.restdto.request.RecordInventoryUsageRequestDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.InventoryItemResponseDTO;
import io.propenuy.asis_app_be.restdto.response.RecordInventoryUsageResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryRestServiceImpl implements InventoryRestService {
    private static final String ROLE_PENGURUS = "PENGURUS";
    private static final String ROLE_KETUA_YAYASAN = "KETUA YAYASAN";
    private static final String CLOUDINARY_FOLDER = "inventory-items";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryUsageLogRepository inventoryUsageLogRepository;
    private final UserRepository userRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ObjectMapper objectMapper;
    private final InventoryItemAuditRecorder inventoryItemAuditRecorder;

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponseDTO getById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        InventoryItem item = inventoryItemRepository.findByIdWithBreakdowns(uuid).orElse(null);
        if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            return null;
        }

        return toResponseDTO(item);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemListResponseDTO list(String search, int page, int limit) {
        final String searchTerm = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                limit > 0 ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<InventoryItem> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (searchTerm != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("itemName")), "%" + searchTerm + "%"),
                        cb.like(cb.lower(root.get("donorSource")), "%" + searchTerm + "%")
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<InventoryItem> pageResult = inventoryItemRepository.findAll(spec, pageable);
        List<InventoryItemListResponseDTO.InventoryItemSummaryResponseDTO> content = pageResult.getContent().stream()
                .map(item -> InventoryItemListResponseDTO.InventoryItemSummaryResponseDTO.builder()
                        .id(item.getId())
                        .itemName(item.getItemName())
                        .category(item.getCategory() != null ? item.getCategory().name() : null)
                        .quantity(item.getQuantity())
                        .unit(item.getUnit().name())
                        .donorSource(item.getDonorSource())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        return InventoryItemListResponseDTO.builder()
                .content(content)
                .page(pageResult.getNumber())
                .limit(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public InventoryItemResponseDTO create(
            CreateInventoryItemRequestDTO request,
            MultipartFile photoFile,
            String currentUsername
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request inventory tidak boleh kosong");
        }
        if (request.getItemName() == null || request.getItemName().isBlank()) {
            throw new IllegalArgumentException("Nama barang wajib diisi");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("Kategori inventory wajib diisi");
        }
        if (request.getDonorSource() == null || request.getDonorSource().isBlank()) {
            throw new IllegalArgumentException("Sumber donasi wajib diisi");
        }
        if (request.getQuantity() == null || request.getQuantity().isBlank()) {
            throw new IllegalArgumentException("Jumlah barang wajib diisi");
        }
        if (request.getUnit() == null || request.getUnit().isBlank()) {
            throw new IllegalArgumentException("Satuan barang wajib diisi");
        }

        User createdByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        assertAllowedRole(createdByUser);

        InventoryCategory category;
        try {
            category = parseCategory(request.getCategory());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Kategori tidak valid. Nilai yang diterima: ASET, KEBUTUHAN_POKOK, PERLENGKAPAN_IBADAH, PENDIDIKAN"
            );
        }

        InventoryUnit unit;
        try {
            unit = parseUnit(request.getUnit());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Satuan tidak valid. Nilai yang diterima: BUAH, UNIT, KARDUS, LUSIN, PCS, SET, LEMBAR, KG, PACK, RIM"
            );
        }

        BigDecimal quantity;
        try {
            quantity = new BigDecimal(request.getQuantity().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Jumlah barang harus berupa angka");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Jumlah barang harus lebih dari 0");
        }

        String normalizedPhotoUrl = request.getPhotoUrl() == null ? "" : request.getPhotoUrl().trim();
        String uploadedPhotoUrl = uploadPhotoIfProvided(photoFile);
        if ((normalizedPhotoUrl.isBlank()) && uploadedPhotoUrl == null) {
            throw new IllegalArgumentException("Foto barang wajib diisi (photoUrl atau photoFile)");
        }

        List<InventoryItemBreakdown> breakdowns = parseAndValidateBreakdowns(
                request.getBreakdownsList(),
                quantity
        );

        InventoryItem item = InventoryItem.builder()
                .itemName(request.getItemName().trim())
                .category(category)
                .donorSource(request.getDonorSource().trim())
                .photoUrl(uploadedPhotoUrl != null ? uploadedPhotoUrl : normalizedPhotoUrl)
                .quantity(quantity)
                .unit(unit)
                .note(request.getNote() != null && !request.getNote().isBlank() ? request.getNote().trim() : null)
                .createdBy(createdByUser)
                .build();

        for (InventoryItemBreakdown breakdown : breakdowns) {
            breakdown.setInventoryItem(item);
        }
        item.getBreakdowns().addAll(breakdowns);

        inventoryItemRepository.save(item);
        inventoryItemAuditRecorder.recordAfterCreate(item);
        return toResponseDTO(item);
    }

    @Override
    @Transactional
    public RecordInventoryUsageResponseDTO recordUsage(
            String id,
            RecordInventoryUsageRequestDTO request,
            String currentUsername
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        InventoryItem item = inventoryItemRepository.findByIdWithBreakdowns(uuid).orElse(null);
        if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            return null;
        }

        User actor = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        assertAllowedRole(actor);

        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong");
        }
        if (request.getUsagePurpose() == null || request.getUsagePurpose().isBlank()) {
            throw new IllegalArgumentException("Tujuan pemakaian wajib diisi");
        }

        UsageDeductionPlan plan = buildUsageDeductionPlan(item, request.getBreakdownsList());
        BigDecimal totalUsed = plan.totalUsed();
        if (totalUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Minimal satu sub-item harus memiliki jumlah pemakaian lebih dari 0");
        }
        if (totalUsed.compareTo(item.getQuantity()) > 0) {
            throw new IllegalArgumentException("Jumlah pemakaian melebihi stok yang tersedia");
        }

        String purposeTrimmed = request.getUsagePurpose().trim();
        String unitWord = inventoryUnitWordForAudit(item.getUnit());

        List<InventoryItemResponseDTO.UsageLogBreakdownResponseDTO> positiveBreakdowns = new ArrayList<>();
        List<String> auditSegments = new ArrayList<>();
        for (UsageDeductionPlan.Line line : plan.lines()) {
            if (line.useAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            positiveBreakdowns.add(InventoryItemResponseDTO.UsageLogBreakdownResponseDTO.builder()
                    .breakdownId(line.breakdown().getId())
                    .name(line.breakdown().getName())
                    .amount(line.useAmount())
                    .build());
            auditSegments.add(formatQuantityPlain(line.useAmount())
                    + " "
                    + unitWord
                    + " "
                    + line.breakdown().getName());
        }

        String middle = String.join(", ", auditSegments);
        String safePurpose = purposeTrimmed.replace("\"", "'");
        String auditMessage = actor.getUsername()
                + " memakai sebanyak "
                + middle
                + " dengan tujuan pemakaian \""
                + safePurpose
                + "\"";

        String breakdownJson;
        try {
            breakdownJson = objectMapper.writeValueAsString(positiveBreakdowns);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Gagal menyiapkan riwayat pemakaian", e);
        }

        InventoryUsageLog log = InventoryUsageLog.builder()
                .inventoryItem(item)
                .quantityUsed(totalUsed)
                .usagePurpose(purposeTrimmed)
                .auditMessage(auditMessage)
                .breakdownUsagesJson(breakdownJson)
                .createdBy(actor)
                .build();

        for (UsageDeductionPlan.Line line : plan.lines()) {
            line.breakdown().setAmount(line.breakdown().getAmount().subtract(line.useAmount()));
        }
        item.setQuantity(item.getQuantity().subtract(totalUsed));
        inventoryUsageLogRepository.save(log);
        inventoryItemRepository.save(item);
        inventoryItemRepository.flush();

        InventoryItem refreshed = inventoryItemRepository.findByIdWithBreakdowns(item.getId()).orElse(item);

        return RecordInventoryUsageResponseDTO.builder()
                .usage(RecordInventoryUsageResponseDTO.InventoryUsageRecordedDTO.builder()
                        .id(log.getId())
                        .quantityUsed(log.getQuantityUsed())
                        .usagePurpose(log.getUsagePurpose())
                        .auditMessage(log.getAuditMessage())
                        .breakdownUsages(positiveBreakdowns)
                        .createdByUsername(actor.getUsername())
                        .createdAt(log.getCreatedAt())
                        .build())
                .item(toResponseDTO(refreshed))
                .build();
    }

    private UsageDeductionPlan buildUsageDeductionPlan(
            InventoryItem item,
            List<RecordInventoryUsageRequestDTO.BreakdownRequestDTO> breakdownsList
    ) {
        if (item.getBreakdowns() == null || item.getBreakdowns().isEmpty()) {
            throw new IllegalArgumentException("Item tidak memiliki rincian barang; tidak dapat mencatat pemakaian");
        }

        if (breakdownsList == null || breakdownsList.isEmpty()) {
            throw new IllegalArgumentException("breakdownsList wajib berisi pemakaian per sub-item");
        }

        java.util.Map<UUID, BigDecimal> requestedById = new java.util.HashMap<>();
        int index = 0;
        for (RecordInventoryUsageRequestDTO.BreakdownRequestDTO row : breakdownsList) {
            index++;
            if (row == null) {
                throw new IllegalArgumentException("breakdownsList item ke-" + index + " tidak valid");
            }
            if (row.getBreakdownId() == null || row.getBreakdownId().isBlank()) {
                throw new IllegalArgumentException("breakdownId item ke-" + index + " wajib diisi");
            }
            UUID breakdownUuid;
            try {
                breakdownUuid = UUID.fromString(row.getBreakdownId().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("breakdownId item ke-" + index + " tidak valid");
            }
            if (row.getAmount() == null) {
                throw new IllegalArgumentException("Jumlah pemakaian item ke-" + index + " wajib diisi");
            }
            BigDecimal use = row.getAmount();
            if (use.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Jumlah pemakaian tidak boleh kurang dari 0");
            }
            if (requestedById.containsKey(breakdownUuid)) {
                throw new IllegalArgumentException("breakdownId duplikat pada permintaan");
            }
            requestedById.put(breakdownUuid, use);
        }

        java.util.Map<UUID, InventoryItemBreakdown> byId = new java.util.HashMap<>();
        for (InventoryItemBreakdown b : item.getBreakdowns()) {
            byId.put(b.getId(), b);
        }

        for (UUID rid : requestedById.keySet()) {
            if (!byId.containsKey(rid)) {
                throw new IllegalArgumentException("Sub-item dengan ID tidak termasuk pada item inventory ini");
            }
        }
        if (requestedById.size() != byId.size()) {
            throw new IllegalArgumentException("breakdownsList harus memuat seluruh sub-item beserta jumlah pemakaiannya");
        }

        List<UsageDeductionPlan.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (java.util.Map.Entry<UUID, BigDecimal> e : requestedById.entrySet()) {
            InventoryItemBreakdown b = byId.get(e.getKey());
            BigDecimal use = e.getValue();
            if (use.compareTo(b.getAmount()) > 0) {
                throw new IllegalArgumentException(
                        "Pemakaian untuk sub-item \"" + b.getName() + "\" melebihi jumlah yang tersedia"
                );
            }
            total = total.add(use);
            lines.add(new UsageDeductionPlan.Line(b, use));
        }

        return new UsageDeductionPlan(lines, total);
    }

    private record UsageDeductionPlan(List<Line> lines, BigDecimal totalUsed) {
        private record Line(InventoryItemBreakdown breakdown, BigDecimal useAmount) {}
    }

    private String inventoryUnitWordForAudit(InventoryUnit unit) {
        if (unit == null) {
            return "unit";
        }
        return unit.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private List<InventoryItemResponseDTO.UsageLogBreakdownResponseDTO> parseUsageLogBreakdownsJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<InventoryItemResponseDTO.UsageLogBreakdownResponseDTO>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private String formatQuantityPlain(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    private void assertAllowedRole(User user) {
        String role = user.getRole() == null ? "" : user.getRole().trim();
        if (!ROLE_PENGURUS.equalsIgnoreCase(role) && !ROLE_KETUA_YAYASAN.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Hanya Pengurus dan Ketua Yayasan yang dapat membuat item inventory");
        }
    }

    private String uploadPhotoIfProvided(MultipartFile photoFile) {
        if (photoFile == null || photoFile.isEmpty()) {
            return null;
        }

        String contentType = photoFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Format foto tidak didukung. Gunakan JPG, PNG, GIF, atau WebP");
        }

        try {
            String originalFilename = photoFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String storagePath = UUID.randomUUID() + extension;
            return cloudinaryStorageService.uploadFile(photoFile, storagePath, CLOUDINARY_FOLDER, "image");
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengupload foto barang: " + e.getMessage());
        }
    }

    private List<InventoryItemBreakdown> parseAndValidateBreakdowns(
            List<CreateInventoryItemRequestDTO.BreakdownRequestDTO> breakdownsList,
            BigDecimal quantity
    ) {
        List<InventoryItemBreakdown> parsed = new ArrayList<>();
        if (breakdownsList == null || breakdownsList.isEmpty()) {
            return parsed;
        }

        BigDecimal total = BigDecimal.ZERO;
        int index = 0;
        for (CreateInventoryItemRequestDTO.BreakdownRequestDTO row : breakdownsList) {
            index++;
            String name = row == null || row.getName() == null ? "" : row.getName().trim();
            String amountRaw = row == null || row.getAmount() == null ? "" : row.getAmount().trim();

            if (name.isBlank()) {
                throw new IllegalArgumentException("Nama breakdown item ke-" + index + " wajib diisi");
            }
            if (amountRaw.isBlank()) {
                throw new IllegalArgumentException("Jumlah breakdown item ke-" + index + " wajib diisi");
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountRaw.replace(",", "."));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Jumlah breakdown item ke-" + index + " harus berupa angka");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Jumlah breakdown item ke-" + index + " harus lebih dari 0");
            }

            total = total.add(amount);
            parsed.add(InventoryItemBreakdown.builder()
                    .name(name)
                    .amount(amount)
                    .build());
        }

        if (total.compareTo(quantity) != 0) {
            throw new IllegalArgumentException(
                    "Total breakdown (" + total.toPlainString() + ") harus sama dengan quantity (" + quantity.toPlainString() + ")"
            );
        }

        return parsed;
    }

    private InventoryCategory parseCategory(String rawCategory) {
        String normalized = rawCategory.toUpperCase().replace("-", "_").replace(" ", "_");
        return InventoryCategory.valueOf(normalized);
    }

    private InventoryUnit parseUnit(String rawUnit) {
        String normalized = rawUnit.toUpperCase().replace("-", "_").replace(" ", "_");
        return InventoryUnit.valueOf(normalized);
    }

    private InventoryItemResponseDTO toResponseDTO(InventoryItem item) {
        List<InventoryItemResponseDTO.BreakdownResponseDTO> breakdowns = item.getBreakdowns().stream()
                .map(b -> InventoryItemResponseDTO.BreakdownResponseDTO.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .amount(b.getAmount())
                        .build())
                .toList();

        List<InventoryItemResponseDTO.InventoryUsageLogResponseDTO> usageLogs = item.getUsageLogs() == null
                ? List.of()
                : item.getUsageLogs().stream()
                        .map(ul -> InventoryItemResponseDTO.InventoryUsageLogResponseDTO.builder()
                                .id(ul.getId())
                                .quantityUsed(ul.getQuantityUsed())
                                .usagePurpose(ul.getUsagePurpose())
                                .auditMessage(ul.getAuditMessage())
                                .breakdownUsages(parseUsageLogBreakdownsJson(ul.getBreakdownUsagesJson()))
                                .createdByUsername(ul.getCreatedBy() != null ? ul.getCreatedBy().getUsername() : null)
                                .createdAt(ul.getCreatedAt())
                                .build())
                        .toList();

        return InventoryItemResponseDTO.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .category(item.getCategory().name())
                .donorSource(item.getDonorSource())
                .photoUrl(item.getPhotoUrl())
                .quantity(item.getQuantity())
                .unit(item.getUnit().name())
                .breakdownsList(breakdowns)
                .usageLogs(usageLogs)
                .note(item.getNote())
                .createdBy(item.getCreatedBy() != null ? item.getCreatedBy().getUsername() : null)
                .createdByUsername(item.getCreatedBy() != null ? item.getCreatedBy().getUsername() : null)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
