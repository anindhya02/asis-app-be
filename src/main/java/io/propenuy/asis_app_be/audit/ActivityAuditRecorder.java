package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.ActivityAttachment;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.repository.ActivityAttachmentRepository;
import io.propenuy.asis_app_be.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActivityAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final ActivityAuditSnapshot snapshot;
    private final ActivityAttachmentRepository attachmentRepository;
    private final AuditLogRepository auditLogRepository;

    /** Snapshot sebelum update yang ditunda sampai unggah lampiran selesai (satu log UPDATE). */
    private final ConcurrentHashMap<UUID, BeforeState> pendingUpdateBefore = new ConcurrentHashMap<>();

    public ActivityAuditRecorder(
            AuditLogWriter auditLogWriter,
            ActivityAuditSnapshot snapshot,
            ActivityAttachmentRepository attachmentRepository,
            AuditLogRepository auditLogRepository) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
        this.attachmentRepository = attachmentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public record BeforeState(
            String title,
            String category,
            String program,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            List<String> activityPhotoUrls) {
    }

    public BeforeState capture(Activity activity) {
        return new BeforeState(
                activity.getTitle(),
                activity.getCategory(),
                activity.getProgram(),
                activity.getStartDate(),
                activity.getEndDate(),
                activity.getDescription(),
                allImageUrls(activity.getId()));
    }

    public void stashPendingUpdateBefore(UUID activityId, BeforeState before) {
        if (activityId != null && before != null) {
            pendingUpdateBefore.put(activityId, before);
        }
    }

    public BeforeState consumePendingUpdateBefore(UUID activityId) {
        if (activityId == null) {
            return null;
        }
        return pendingUpdateBefore.remove(activityId);
    }

    public boolean hasCreateAudit(UUID activityId) {
        if (activityId == null) {
            return false;
        }
        return auditLogRepository.existsByModuleCodeAndEntityIdAndActionType(
                AuditModuleCode.ACTIVITY,
                activityId.toString(),
                AuditActionType.CREATE);
    }

    public void recordAfterCreate(Activity activity) {
        BeforeState state = capture(activity);
        persist(
                activity.getId(),
                AuditActionType.CREATE,
                null,
                snapshot.toJson(state));
    }

    public void recordAfterUpdate(BeforeState before, Activity after) {
        BeforeState afterState = capture(after);
        persist(
                after.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(before),
                snapshot.toJson(afterState));
    }

    public void recordAfterSoftDelete(BeforeState before, UUID activityId) {
        consumePendingUpdateBefore(activityId);
        persist(
                activityId,
                AuditActionType.DELETE,
                snapshot.toJson(before),
                null);
    }

    /**
     * Setelah unggah lampiran: satu log CREATE (belum ada) atau satu log UPDATE (gabung field + foto).
     */
    public void recordAfterAttachmentsUploaded(BeforeState beforeUpload, Activity activityAfter) {
        UUID activityId = activityAfter.getId();
        BeforeState pendingUpdate = consumePendingUpdateBefore(activityId);

        if (pendingUpdate != null) {
            recordAfterUpdate(pendingUpdate, activityAfter);
            return;
        }

        if (!hasCreateAudit(activityId)) {
            recordAfterCreate(activityAfter);
            return;
        }

        if (photoUrlsEqual(beforeUpload.activityPhotoUrls(), allImageUrls(activityId))) {
            return;
        }
        recordAfterUpdate(beforeUpload, activityAfter);
    }

    public List<String> allImageUrls(UUID activityId) {
        if (activityId == null) {
            return List.of();
        }
        return attachmentRepository.findAllByActivityId(activityId).stream()
                .filter(a -> a.getFileType() != null && a.getFileType().startsWith("image/"))
                .filter(a -> a.getFileUrl() != null && !a.getFileUrl().isBlank())
                .sorted(Comparator.comparing(
                        ActivityAttachment::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> normalizeUrl(a.getFileUrl()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String t = url.trim();
        if (t.startsWith("//")) {
            return "https:" + t;
        }
        return t;
    }

    private static boolean photoUrlsEqual(List<String> left, List<String> right) {
        List<String> a = left != null ? left : List.of();
        List<String> b = right != null ? right : List.of();
        return Objects.equals(a, b);
    }

    private void persist(UUID activityId, AuditActionType actionType, String oldJson, String newJson) {
        auditLogWriter.persist(
                AuditModuleCode.ACTIVITY,
                Activity.class,
                activityId,
                actionType,
                oldJson,
                newJson);
    }
}
