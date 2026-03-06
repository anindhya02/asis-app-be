package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.ActivityAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityAttachmentRepository extends JpaRepository<ActivityAttachment, UUID> {

    List<ActivityAttachment> findAllByActivityId(UUID activityId);
}
