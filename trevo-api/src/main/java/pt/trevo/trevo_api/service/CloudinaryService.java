package pt.trevo.trevo_api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final long maxFileSizeBytes;

    public CloudinaryService(
        Cloudinary cloudinary,
        @Value("${app.upload.image.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.cloudinary = cloudinary;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            return uploadImageBytes(file.getBytes());
        } catch (IOException ex) {
            throw badGateway("Failed to upload image to Cloudinary", ex);
        }
    }

    public CloudinaryUploadResult uploadImage(byte[] bytes, String originalFilename, String contentType) {
        validateImageBytes(bytes, contentType);
        return uploadImageBytes(bytes);
    }

    public void deleteImage(String publicId) {
        if (isBlank(publicId)) {
            throw badRequest("Image publicId is required");
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException ex) {
            throw badGateway("Failed to delete image from Cloudinary", ex);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Image file is required");
        }

        String contentType = file.getContentType();
        if (isBlank(contentType) || !contentType.startsWith("image/")) {
            throw badRequest("Only image files are allowed");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw badRequest("Image file exceeds maximum allowed size");
        }

        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            String detectedContentType = URLConnection.guessContentTypeFromStream(inputStream);

            if (isBlank(detectedContentType) || !detectedContentType.startsWith("image/")) {
                throw badRequest("Only image files are allowed");
            }
        } catch (IOException ex) {
            throw badRequest("Unable to read image file", ex);
        }
    }

    private void validateImageBytes(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw badRequest("Image file is required");
        }

        if (isBlank(contentType) || !contentType.startsWith("image/")) {
            throw badRequest("Only image files are allowed");
        }

        if (bytes.length > maxFileSizeBytes) {
            throw badRequest("Image file exceeds maximum allowed size");
        }

        try (InputStream inputStream = new BufferedInputStream(new java.io.ByteArrayInputStream(bytes))) {
            String detectedContentType = URLConnection.guessContentTypeFromStream(inputStream);

            if (isBlank(detectedContentType) || !detectedContentType.startsWith("image/")) {
                throw badRequest("Only image files are allowed");
            }
        } catch (IOException ex) {
            throw badRequest("Unable to read image file", ex);
        }
    }

    private String asString(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private ResponseStatusException badRequest(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason, cause);
    }

    private ResponseStatusException badGateway(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason);
    }

    private ResponseStatusException badGateway(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, cause);
    }

    private CloudinaryUploadResult uploadImageBytes(byte[] bytes) {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                bytes,
                ObjectUtils.asMap("resource_type", "image")
            );

            String publicId = asString(uploadResult.get("public_id"));
            String secureUrl = asString(uploadResult.get("secure_url"));
            String fallbackUrl = asString(uploadResult.get("url"));
            String imageUrl = secureUrl != null ? secureUrl : fallbackUrl;

            if (publicId == null || imageUrl == null) {
                throw badGateway("Cloudinary response did not include image identifiers");
            }

            return new CloudinaryUploadResult(publicId, imageUrl);
        } catch (IOException ex) {
            throw badGateway("Failed to upload image to Cloudinary", ex);
        }
    }
}
