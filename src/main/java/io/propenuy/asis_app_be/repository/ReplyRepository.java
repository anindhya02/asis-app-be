package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReplyRepository extends JpaRepository<Reply, UUID> {

    List<Reply> findAllByActivityIdOrderByCreatedAtAsc(UUID activityId);

    List<Reply> findAllByParentReplyReplyId(UUID parentReplyId);
}
