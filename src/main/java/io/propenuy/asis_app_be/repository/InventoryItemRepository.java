package io.propenuy.asis_app_be.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.propenuy.asis_app_be.model.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID>, JpaSpecificationExecutor<InventoryItem> {

    @Query("SELECT DISTINCT i FROM InventoryItem i LEFT JOIN FETCH i.breakdowns WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithBreakdowns(@Param("id") UUID id);
}
