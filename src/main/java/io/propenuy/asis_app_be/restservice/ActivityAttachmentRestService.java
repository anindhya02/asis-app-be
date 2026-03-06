package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.AttachmentResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ActivityAttachmentRestService {

    List<AttachmentResponseDTO> uploadAttachments(UUID activityId, MultipartFile[] files);

    List<AttachmentResponseDTO> getAttachmentsByActivityId(UUID activityId);

    void deleteAttachment(UUID attachmentId);
}
