package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ActivityAuditSnapshot {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    public ActivityAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(ActivityAuditRecorder.BeforeState state) {
        if (state == null) {
            return null;
        }
        return toCompactJson(
                state.title(),
                state.category(),
                state.program(),
                state.startDate(),
                state.endDate(),
                state.description(),
                state.activityPhotoUrls());
    }

    private String toCompactJson(
            String title,
            String category,
            String program,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            List<String> activityPhotoUrls) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (title != null && !title.isBlank()) {
                node.put("title", title.trim());
            }
            if (category != null && !category.isBlank()) {
                node.put("category", category.trim());
            }
            if (program != null && !program.isBlank()) {
                node.put("program", program.trim());
            }
            String period = formatPeriod(startDate, endDate);
            if (period != null) {
                node.put("period", period);
            }
            if (description != null && !description.isBlank()) {
                node.put("description", description.trim());
            }
            if (activityPhotoUrls != null && !activityPhotoUrls.isEmpty()) {
                ArrayNode arr = node.putArray("activityPhotoUrls");
                for (String url : activityPhotoUrls) {
                    if (url != null && !url.isBlank()) {
                        arr.add(url.trim());
                    }
                }
                node.put("activityPhotoUrl", activityPhotoUrls.get(0).trim());
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return null;
        }
        String start = DATE_FMT.format(startDate);
        if (endDate == null || endDate.equals(startDate)) {
            return start;
        }
        return start + " – " + DATE_FMT.format(endDate);
    }
}
