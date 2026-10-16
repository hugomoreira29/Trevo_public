package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderCollectionsRequest(
    @NotNull(message = "Collection IDs are required")
    List<Long> collectionIds
) {
}
