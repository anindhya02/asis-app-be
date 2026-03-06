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
     * Upload file ke Cloudinary.
     * @param file MultipartFile dari request
     * @param storagePath public_id untuk file (e.g. "activityId/timestamp-filename")
     * @return public URL dari file yang diupload
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String storagePath) throws IOException {
        // Remove file extension from public_id (Cloudinary adds it automatically)
        String publicId = storagePath.contains(".")
                ? storagePath.substring(0, storagePath.lastIndexOf('.'))
                : storagePath;

        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "activity-attachments",
                "resource_type", "image"
        ));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Delete file dari Cloudinary.
     * @param storagePath public_id file
     */
    public void deleteFile(String storagePath) throws IOException {
        String publicId = storagePath.contains(".")
                ? storagePath.substring(0, storagePath.lastIndexOf('.'))
                : storagePath;

        String fullPublicId = "activity-attachments/" + publicId;

        cloudinary.uploader().destroy(fullPublicId, ObjectUtils.asMap(
                "resource_type", "image"
        ));
    }
}
