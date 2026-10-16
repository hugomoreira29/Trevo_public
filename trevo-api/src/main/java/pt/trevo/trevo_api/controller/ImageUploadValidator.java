package pt.trevo.trevo_api.controller;

import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ImageUploadValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final long maxFileSizeBytes;

    public ImageUploadValidator(@Value("${app.upload.image.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void validateOptionalImage(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw badRequest(fieldName + " must be a JPEG, PNG, GIF, or WEBP image");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw badRequest(fieldName + " exceeds the maximum allowed size");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw badRequest(fieldName + " must use a standard web image extension");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "";
        }

        return filename.substring(extensionIndex).toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
