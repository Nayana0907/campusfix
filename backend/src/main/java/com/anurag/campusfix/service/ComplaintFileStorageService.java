package com.anurag.campusfix.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.anurag.campusfix.exception.FileUploadValidationException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;

import jakarta.annotation.PostConstruct;

@Service
public class ComplaintFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintFileStorageService.class);

    private final Path uploadDir;
    private final long maxFileSizeBytes;
    private final int maxImagesPerComplaint;

    public ComplaintFileStorageService(
            @Value("${app.complaint.upload.dir:uploads/complaints}") String uploadDirProperty,
            @Value("${app.complaint.upload.max-file-size-bytes:5242880}") long maxFileSizeBytes, // 5MB
            @Value("${app.complaint.upload.max-images-per-complaint:5}") int maxImagesPerComplaint) {

        this.uploadDir = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxImagesPerComplaint = maxImagesPerComplaint;
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new FileUploadValidationException("Could not initialize upload storage directory", e);
        }
    }

    public int getMaxImagesPerComplaint() {
        return maxImagesPerComplaint;
    }

    /**
     * Validates file size, Content-Type, and magic bytes, then saves with an opaque storage key.
     */
    public StoredFileResult storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadValidationException("Cannot upload an empty file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new FileUploadValidationException(
                    "File exceeds maximum permitted size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "image";
        } else {
            // Sanitize original filename (strip directories safely)
            Path p = Paths.get(originalFilename).getFileName();
            originalFilename = (p != null && !p.toString().isBlank()) ? p.toString() : "image";
        }

        String detectedExtension = validateAndGetExtension(file);

        String storageKey = UUID.randomUUID() + "." + detectedExtension;
        Path targetLocation = uploadDir.resolve(storageKey).normalize();

        // Path traversal defense
        if (!targetLocation.startsWith(uploadDir)) {
            throw new FileUploadValidationException("Invalid storage destination");
        }

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new FileUploadValidationException("Could not initialize upload storage directory", e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFileResult(
                    storageKey,
                    originalFilename,
                    resolveMediaType(detectedExtension),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new FileUploadValidationException("Failed to persist uploaded file", e);
        }
    }

    /**
     * Loads a file resource by storage key with path traversal protection.
     */
    public Resource loadAsResource(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResourceNotFoundException("Image storage key cannot be empty");
        }

        try {
            Path filePath = uploadDir.resolve(storageKey).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new UnauthorizedComplaintAccessException("Access to outside storage path is denied");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Image file not found on disk");
            }
        } catch (UnauthorizedComplaintAccessException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Image file not found: " + storageKey);
        }
    }

    /**
     * Deletes a stored file if it exists.
     */
    public void deleteFile(String storageKey) {
        try {
            Path filePath = uploadDir.resolve(storageKey).normalize();
            if (filePath.startsWith(uploadDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete stored file: {}", storageKey, e);
        }
    }

    private String validateAndGetExtension(MultipartFile file) {
        byte[] headerBytes = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(headerBytes);
            if (read < 4) {
                throw new FileUploadValidationException("File is too small to be a valid image");
            }
        } catch (IOException e) {
            throw new FileUploadValidationException("Could not read file stream", e);
        }

        // Validate Magic Bytes:
        // JPEG: FF D8 FF
        if ((headerBytes[0] & 0xFF) == 0xFF
                && (headerBytes[1] & 0xFF) == 0xD8
                && (headerBytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((headerBytes[0] & 0xFF) == 0x89
                && headerBytes[1] == 0x50 // 'P'
                && headerBytes[2] == 0x4E // 'N'
                && headerBytes[3] == 0x47 // 'G'
                && headerBytes[4] == 0x0D
                && headerBytes[5] == 0x0A
                && headerBytes[6] == 0x1A
                && headerBytes[7] == 0x0A) {
            return "png";
        }

        // WebP: RIFF....WEBP
        if (headerBytes[0] == 'R' && headerBytes[1] == 'I' && headerBytes[2] == 'F' && headerBytes[3] == 'F'
                && headerBytes[8] == 'W' && headerBytes[9] == 'E' && headerBytes[10] == 'B' && headerBytes[11] == 'P') {
            return "webp";
        }

        throw new FileUploadValidationException(
                "Unsupported file type. Only JPEG, PNG, and WebP images are allowed.");
    }

    private String resolveMediaType(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    public record StoredFileResult(
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSize
    ) {}
}
