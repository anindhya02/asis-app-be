package io.propenuy.asis_app_be.restservice;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Upload file ke Cloudinary (default: activity-attachments, image).
     * @param file MultipartFile dari request
     * @param storagePath public_id untuk file (e.g. "activityId/timestamp-filename")
     * @return public URL dari file yang diupload
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String storagePath) throws IOException {
        return uploadFile(file, storagePath, "activity-attachments", "image");
    }

    /**
     * Upload file ke Cloudinary dengan folder dan resource_type custom.
     * @param file MultipartFile dari request
     * @param storagePath public_id untuk file
     * @param folder folder tujuan di Cloudinary
     * @param resourceType resource_type Cloudinary ("image", "raw", "auto", dll.)
     * @return public URL (secure_url) dari file yang diupload
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String storagePath, String folder, String resourceType) throws IOException {
        String publicId = storagePath.contains(".")
                ? storagePath.substring(0, storagePath.lastIndexOf('.'))
                : storagePath;

        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "folder", folder,
                "resource_type", resourceType
        ));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Delete file dari Cloudinary (default: activity-attachments, image).
     * @param storagePath public_id file
     */
    public void deleteFile(String storagePath) throws IOException {
        deleteFile(storagePath, "activity-attachments", "image");
    }

    /**
     * Delete file dari Cloudinary dengan folder dan resource_type custom.
     * @param storagePath public_id file
     * @param folder folder di Cloudinary
     * @param resourceType resource_type Cloudinary
     */
    public void deleteFile(String storagePath, String folder, String resourceType) throws IOException {
        String publicId = storagePath.contains(".")
                ? storagePath.substring(0, storagePath.lastIndexOf('.'))
                : storagePath;

        String fullPublicId = folder + "/" + publicId;

        cloudinary.uploader().destroy(fullPublicId, ObjectUtils.asMap(
                "resource_type", resourceType
        ));
    }
}
