package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.ActivityAttachment;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.repository.ActivityAttachmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActivityAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final ActivityAuditSnapshot snapshot;
    private final ActivityAttachmentRepository attachmentRepository;

    public ActivityAuditRecorder(
            AuditLogWriter auditLogWriter,
            ActivityAuditSnapshot snapshot,
            ActivityAttachmentRepository attachmentRepository) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
        this.attachmentRepository = attachmentRepository;
    }

    public record BeforeState(
            String title,
            String category,
            String program,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            String activityPhotoUrl) {
    }

    public BeforeState capture(Activity activity) {
        return new BeforeState(
                activity.getTitle(),
                activity.getCategory(),
                activity.getProgram(),
                activity.getStartDate(),
                activity.getEndDate(),
                activity.getDescription(),
                primaryPhotoUrl(activity.getId()));
    }

    public void recordAfterCreate(Activity activity) {
        String photoUrl = primaryPhotoUrl(activity.getId());
        persist(
                activity.getId(),
                AuditActionType.CREATE,
                null,
                snapshot.toJson(
                        activity.getTitle(),
                        activity.getCategory(),
                        activity.getProgram(),
                        activity.getStartDate(),
                        activity.getEndDate(),
                        activity.getDescription(),
                        photoUrl,
                        null,
                        null,
                        null));
    }

    public void recordAfterUpdate(BeforeState before, Activity after) {
        String newPhotoUrl = primaryPhotoUrl(after.getId());
        String oldPhotoUrl = before.activityPhotoUrl();
        boolean photoChanged = !Objects.equals(
                normalizeUrl(oldPhotoUrl),
                normalizeUrl(newPhotoUrl));
        persist(
                after.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(before, null, null, null),
                snapshot.toJson(
                        capture(after),
                        photoChanged,
                        photoChanged ? oldPhotoUrl : null,
                        photoChanged ? newPhotoUrl : null));
    }

    public void recordAfterPhotoChange(BeforeState activityBefore, Activity activityAfter) {
        String oldPhotoUrl = activityBefore.activityPhotoUrl();
        String newPhotoUrl = primaryPhotoUrl(activityAfter.getId());
        if (Objects.equals(normalizeUrl(oldPhotoUrl), normalizeUrl(newPhotoUrl))) {
            return;
        }
        persist(
                activityAfter.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(activityBefore, null, null, null),
                snapshot.toJson(
                        capture(activityAfter),
                        true,
                        oldPhotoUrl,
                        newPhotoUrl));
    }

    public void recordAfterSoftDelete(BeforeState before, UUID activityId) {
        persist(
                activityId,
                AuditActionType.DELETE,
                snapshot.toJson(before, null, null, null),
                null);
    }

    public String primaryPhotoUrl(UUID activityId) {
        if (activityId == null) {
            return null;
        }
        List<ActivityAttachment> attachments = attachmentRepository.findAllByActivityId(activityId);
        return attachments.stream()
                .filter(a -> a.getFileType() != null && a.getFileType().startsWith("image/"))
                .filter(a -> a.getFileUrl() != null && !a.getFileUrl().isBlank())
                .min(Comparator.comparing(ActivityAttachment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ActivityAttachment::getFileUrl)
                .orElse(null);
    }

    private static String normalizeUrl(String url) {
        return url == null || url.isBlank() ? null : url.trim();
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
