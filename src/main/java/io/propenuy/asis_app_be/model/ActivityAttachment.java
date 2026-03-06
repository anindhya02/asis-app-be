package io.propenuy.asis_app_be.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activity_attachments")
public class ActivityAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType; // e.g. image/jpeg, image/png

    @Column(nullable = false)
    private Long fileSize; // in bytes

    @Column(nullable = false)
    private String storagePath; // public_id in Cloudinary

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl; // public/signed URL

    @CreationTimestamp
    private LocalDateTime createdAt;
}
