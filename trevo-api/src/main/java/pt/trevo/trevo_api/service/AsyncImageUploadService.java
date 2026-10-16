package pt.trevo.trevo_api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.AsyncImageUploadAcceptedResponse;
import pt.trevo.trevo_api.dto.AsyncImageUploadStatusResponse;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

@Service
public class AsyncImageUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncImageUploadService.class);
    private static final Duration JOB_TTL = Duration.ofMinutes(10);
    private static final long MAX_JOBS = 10_000;

    private final CloudinaryService cloudinaryService;
    private final Executor imageUploadExecutor;
    private final Cache<String, UploadJob> jobs;

    public AsyncImageUploadService(
        CloudinaryService cloudinaryService,
        @Qualifier("imageUploadExecutor") Executor imageUploadExecutor
    ) {
        this.cloudinaryService = cloudinaryService;
        this.imageUploadExecutor = imageUploadExecutor;
        this.jobs = Caffeine.newBuilder()
            .maximumSize(MAX_JOBS)
            .expireAfterWrite(JOB_TTL)
            .build();
    }

    public AsyncImageUploadAcceptedResponse startUpload(MultipartFile file, Long ownerId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user id is unavailable");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image file", ex);
        }

        String uploadId = UUID.randomUUID().toString();
        UploadJob job = UploadJob.pending(uploadId, ownerId);
        jobs.put(uploadId, job);

        try {
            CompletableFuture.runAsync(
                () -> processUpload(job, bytes, file.getOriginalFilename(), file.getContentType()),
                imageUploadExecutor
            );
        } catch (RejectedExecutionException ex) {
            jobs.invalidate(uploadId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Image upload queue is full", ex);
        }

        return new AsyncImageUploadAcceptedResponse(
            uploadId,
            AsyncImageUploadStatus.PENDING,
            "/api/uploads/images/" + uploadId
        );
    }

    public AsyncImageUploadStatusResponse getStatus(String uploadId, Long ownerId) {
        return Optional.ofNullable(jobs.getIfPresent(uploadId))
            .filter(job -> job.isOwnedBy(ownerId))
            .map(UploadJob::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload job not found"));
    }

    private void processUpload(UploadJob job, byte[] bytes, String originalFilename, String contentType) {
        job.markProcessing();

        try {
            CloudinaryUploadResult result = cloudinaryService.uploadImage(bytes, originalFilename, contentType);
            job.markCompleted(result);
        } catch (ResponseStatusException ex) {
            LOGGER.warn("Async image upload {} failed: {}", job.uploadId, ex.getReason());
            job.markFailed(ex.getReason());
        } catch (RuntimeException ex) {
            LOGGER.error("Async image upload {} failed unexpectedly", job.uploadId, ex);
            job.markFailed("Failed to upload image");
        }
    }

    private static final class UploadJob {
        private final String uploadId;
        private final Long ownerId;
        private final Instant createdAt;
        private volatile AsyncImageUploadStatus status;
        private volatile CloudinaryUploadResult result;
        private volatile String error;
        private volatile Instant completedAt;

        private UploadJob(String uploadId, Long ownerId, Instant createdAt) {
            this.uploadId = uploadId;
            this.ownerId = ownerId;
            this.createdAt = createdAt;
            this.status = AsyncImageUploadStatus.PENDING;
        }

        private static UploadJob pending(String uploadId, Long ownerId) {
            return new UploadJob(uploadId, ownerId, Instant.now());
        }

        private boolean isOwnedBy(Long userId) {
            return ownerId.equals(userId);
        }

        private void markProcessing() {
            this.status = AsyncImageUploadStatus.PROCESSING;
        }

        private void markCompleted(CloudinaryUploadResult result) {
            this.result = result;
            this.status = AsyncImageUploadStatus.COMPLETED;
            this.completedAt = Instant.now();
        }

        private void markFailed(String error) {
            this.error = error;
            this.status = AsyncImageUploadStatus.FAILED;
            this.completedAt = Instant.now();
        }

        private AsyncImageUploadStatusResponse toResponse() {
            return new AsyncImageUploadStatusResponse(uploadId, status, result, error, createdAt, completedAt);
        }
    }
}
