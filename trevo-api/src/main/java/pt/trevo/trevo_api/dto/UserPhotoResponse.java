package pt.trevo.trevo_api.dto;

import java.time.Instant;

public record UserPhotoResponse(Long id, String url, Instant uploadedAt) {
}
