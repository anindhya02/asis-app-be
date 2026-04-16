package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.request.CreateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.response.ReplyResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ReplyRestService {

    ReplyResponseDTO createReply(UUID activityId, CreateReplyRequestDTO request, String currentUsername);

    List<ReplyResponseDTO> getRepliesByActivityId(UUID activityId);

    ReplyResponseDTO updateReply(UUID replyId, UpdateReplyRequestDTO request, String currentUsername);

    void deleteReply(UUID replyId, String currentUsername);
}
