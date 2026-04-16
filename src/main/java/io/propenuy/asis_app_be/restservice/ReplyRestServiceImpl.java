package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.Reply;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.repository.ActivityRepository;
import io.propenuy.asis_app_be.repository.ReplyRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.request.CreateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.response.ReplyResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReplyRestServiceImpl implements ReplyRestService {

    private final ReplyRepository replyRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReplyResponseDTO createReply(UUID activityId, CreateReplyRequestDTO request, String currentUsername) {
        String content = validateContent(request == null ? null : request.getContent());

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Postingan kegiatan dengan id " + activityId + " tidak ditemukan"));

        User author = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        Reply parentReply = null;
        if (request != null && request.getParentReplyId() != null) {
            parentReply = replyRepository.findById(request.getParentReplyId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Reply parent dengan id " + request.getParentReplyId() + " tidak ditemukan"));

            if (!parentReply.getActivity().getId().equals(activityId)) {
            throw new IllegalArgumentException("Reply parent tidak berada pada postingan kegiatan yang sama");
            }
        }

        Reply reply = Reply.builder()
                .activity(activity)
                .author(author)
            .parentReply(parentReply)
                .content(content)
                .build();

        replyRepository.save(reply);

        return toResponseDTO(reply);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplyResponseDTO> getRepliesByActivityId(UUID activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Postingan kegiatan dengan id " + activityId + " tidak ditemukan"));

        return replyRepository.findAllByActivityIdOrderByCreatedAtAsc(activityId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ReplyResponseDTO updateReply(UUID replyId, UpdateReplyRequestDTO request, String currentUsername) {
        String content = validateContent(request == null ? null : request.getContent());

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reply dengan id " + replyId + " tidak ditemukan"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!reply.getAuthor().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Anda tidak memiliki izin untuk mengubah reply ini");
        }

        reply.setContent(content);
        replyRepository.save(reply);

        return toResponseDTO(reply);
    }

    @Override
    @Transactional
    public void deleteReply(UUID replyId, String currentUsername) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reply dengan id " + replyId + " tidak ditemukan"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!reply.getAuthor().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Anda tidak memiliki izin untuk menghapus reply ini");
        }

        deleteReplyWithChildren(reply);
    }

    private String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Konten reply wajib diisi");
        }

        return content.trim();
    }

    private ReplyResponseDTO toResponseDTO(Reply reply) {
        return ReplyResponseDTO.builder()
                .replyId(reply.getReplyId())
                .activityId(reply.getActivity().getId())
                .parentReplyId(reply.getParentReply() != null ? reply.getParentReply().getReplyId() : null)
                .authorId(reply.getAuthor().getUserId())
                .authorName(reply.getAuthor().getNama())
                .authorUsername(reply.getAuthor().getUsername())
                .authorRole(reply.getAuthor().getRole())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    private void deleteReplyWithChildren(Reply reply) {
        List<Reply> children = replyRepository.findAllByParentReplyReplyId(reply.getReplyId());
        for (Reply child : children) {
            deleteReplyWithChildren(child);
        }
        replyRepository.delete(reply);
    }
}
