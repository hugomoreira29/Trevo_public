package pt.trevo.trevo_api.dto;

import pt.trevo.trevo_api.service.AsyncImageUploadStatus;

public record AsyncImageUploadAcceptedResponse(
    String uploadId,
    AsyncImageUploadStatus status,
    String statusUrl
) {
}
