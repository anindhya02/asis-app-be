package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
}
