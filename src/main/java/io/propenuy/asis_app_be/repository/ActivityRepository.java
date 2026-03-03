package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.enums.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findAllByStatusNot(ActivityStatus status);

    Optional<Activity> findByIdAndStatusNot(UUID id, ActivityStatus status);
}
