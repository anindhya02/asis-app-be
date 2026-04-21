package io.propenuy.asis_app_be.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.propenuy.asis_app_be.model.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID>, JpaSpecificationExecutor<InventoryItem> {
}
