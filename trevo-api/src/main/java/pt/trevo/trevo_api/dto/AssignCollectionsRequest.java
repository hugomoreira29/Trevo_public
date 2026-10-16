package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignCollectionsRequest(
    @NotNull List<Long> collectionIds
) {}
