package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ActivityAuditSnapshot {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    public ActivityAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(
            String title,
            String category,
            String program,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            String activityPhotoUrl,
            Boolean photoChanged,
            String activityPhotoUrlOld,
            String activityPhotoUrlNew) {
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
            if (activityPhotoUrl != null && !activityPhotoUrl.isBlank()) {
                node.put("activityPhotoUrl", activityPhotoUrl);
            }
            if (photoChanged != null) {
                node.put("photoChanged", photoChanged);
            }
            if (activityPhotoUrlOld != null && !activityPhotoUrlOld.isBlank()) {
                node.put("activityPhotoUrlOld", activityPhotoUrlOld);
            }
            if (activityPhotoUrlNew != null && !activityPhotoUrlNew.isBlank()) {
                node.put("activityPhotoUrlNew", activityPhotoUrlNew);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    public String toJson(ActivityAuditRecorder.BeforeState state, Boolean photoChanged, String photoUrlOld, String photoUrlNew) {
        if (state == null) {
            return null;
        }
        String primaryUrl = photoUrlNew != null ? photoUrlNew : state.activityPhotoUrl();
        return toJson(
                state.title(),
                state.category(),
                state.program(),
                state.startDate(),
                state.endDate(),
                state.description(),
                primaryUrl,
                photoChanged,
                photoUrlOld,
                photoUrlNew);
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
