package pt.trevo.trevo_api.dto;

import java.time.Instant;
import pt.trevo.trevo_api.service.AsyncImageUploadStatus;

public record AsyncImageUploadStatusResponse(
    String uploadId,
    AsyncImageUploadStatus status,
    CloudinaryUploadResult result,
    String error,
    Instant createdAt,
    Instant completedAt
) {
}
