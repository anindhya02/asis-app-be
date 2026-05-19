package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.propenuy.asis_app_be.model.InventoryItem;
import io.propenuy.asis_app_be.model.InventoryItemBreakdown;
import io.propenuy.asis_app_be.model.enums.InventoryCategory;
import io.propenuy.asis_app_be.model.enums.InventoryUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryItemAuditSnapshot {

    private final ObjectMapper objectMapper;

    public InventoryItemAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record BreakdownLine(String name, BigDecimal amount) {
    }

    public String toJson(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return toCompactJson(
                item.getItemName(),
                item.getDonorSource(),
                item.getCategory(),
                item.getNote(),
                item.getQuantity(),
                item.getUnit(),
                normalizePhotoUrl(item.getPhotoUrl()),
                breakdownLinesFromEntity(item.getBreakdowns()));
    }

    public static List<BreakdownLine> breakdownLinesFromEntity(List<InventoryItemBreakdown> breakdowns) {
        if (breakdowns == null || breakdowns.isEmpty()) {
            return List.of();
        }
        List<BreakdownLine> lines = new ArrayList<>();
        for (InventoryItemBreakdown b : breakdowns) {
            if (b == null) {
                continue;
            }
            lines.add(new BreakdownLine(b.getName(), b.getAmount()));
        }
        return lines;
    }

    private static String normalizePhotoUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String t = url.trim();
        if (t.startsWith("//")) {
            return "https:" + t;
        }
        return t;
    }

    private String toCompactJson(
            String itemName,
            String donorSource,
            InventoryCategory category,
            String note,
            BigDecimal quantity,
            InventoryUnit unit,
            String photoUrl,
            List<BreakdownLine> breakdowns) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (itemName != null && !itemName.isBlank()) {
                node.put("itemName", itemName.trim());
            }
            if (donorSource != null && !donorSource.isBlank()) {
                node.put("donorSource", donorSource.trim());
            }
            if (category != null) {
                node.put("category", category.name());
            }
            if (note != null && !note.isBlank()) {
                node.put("note", note.trim());
            }
            if (quantity != null) {
                node.put("quantity", quantity);
            }
            if (unit != null) {
                node.put("unit", unit.name());
            }
            if (photoUrl != null && !photoUrl.isBlank()) {
                node.put("photoUrl", photoUrl);
            }
            if (breakdowns != null && !breakdowns.isEmpty()) {
                ArrayNode arr = node.putArray("breakdowns");
                for (BreakdownLine line : breakdowns) {
                    ObjectNode item = arr.addObject();
                    item.put("name", line.name() != null ? line.name() : "");
                    if (line.amount() != null) {
                        item.put("amount", line.amount());
                    }
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
