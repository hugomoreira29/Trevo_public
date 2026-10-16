package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.AsyncImageUploadAcceptedResponse;
import pt.trevo.trevo_api.dto.AsyncImageUploadStatusResponse;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

class AsyncImageUploadServiceTest extends BaseUnitTest {

    @Mock
    private CloudinaryService cloudinaryService;

    private RecordingExecutor executor;
    private AsyncImageUploadService service;

    @BeforeEach
    void setUp() {
        executor = new RecordingExecutor();
        service = new AsyncImageUploadService(cloudinaryService, executor);
    }

    @Test
    void startUploadReturnsPendingJobBeforeCloudinaryCallRuns() {
        MockMultipartFile file = pngFile();

        AsyncImageUploadAcceptedResponse response = service.startUpload(file, 11L);

        assertThat(response.uploadId()).isNotBlank();
        assertThat(response.status()).isEqualTo(AsyncImageUploadStatus.PENDING);
        assertThat(response.statusUrl()).isEqualTo("/api/uploads/images/" + response.uploadId());
        assertThat(executor.queuedTasks()).isEqualTo(1);
        verify(cloudinaryService, never()).uploadImage(any(byte[].class), any(), any());

        AsyncImageUploadStatusResponse status = service.getStatus(response.uploadId(), 11L);
        assertThat(status.status()).isEqualTo(AsyncImageUploadStatus.PENDING);
        assertThat(status.result()).isNull();
    }

    @Test
    void queuedUploadStoresCloudinaryResultWhenCompleted() {
        MockMultipartFile file = pngFile();
        when(cloudinaryService.uploadImage(any(byte[].class), eq("logo.png"), eq("image/png")))
            .thenReturn(new CloudinaryUploadResult("trevo/logo", "https://res.cloudinary.com/demo/logo.png"));

        AsyncImageUploadAcceptedResponse response = service.startUpload(file, 11L);
        executor.runNext();

        AsyncImageUploadStatusResponse status = service.getStatus(response.uploadId(), 11L);
        assertThat(status.status()).isEqualTo(AsyncImageUploadStatus.COMPLETED);
        assertThat(status.result().publicId()).isEqualTo("trevo/logo");
        assertThat(status.result().url()).isEqualTo("https://res.cloudinary.com/demo/logo.png");
        assertThat(status.completedAt()).isNotNull();
    }

    @Test
    void queuedUploadStoresFailureWhenCloudinaryFails() {
        MockMultipartFile file = pngFile();
        when(cloudinaryService.uploadImage(any(byte[].class), eq("logo.png"), eq("image/png")))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary unavailable"));

        AsyncImageUploadAcceptedResponse response = service.startUpload(file, 11L);
        executor.runNext();

        AsyncImageUploadStatusResponse status = service.getStatus(response.uploadId(), 11L);
        assertThat(status.status()).isEqualTo(AsyncImageUploadStatus.FAILED);
        assertThat(status.error()).isEqualTo("Cloudinary unavailable");
        assertThat(status.result()).isNull();
    }

    @Test
    void getStatusReturnsNotFoundForUnknownUploadId() {
        Throwable thrown = catchThrowable(() -> service.getStatus("missing", 11L));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Upload job not found");
    }

    @Test
    void getStatusReturnsNotFoundWhenUploadBelongsToAnotherUser() {
        AsyncImageUploadAcceptedResponse response = service.startUpload(pngFile(), 11L);

        Throwable thrown = catchThrowable(() -> service.getStatus(response.uploadId(), 22L));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Upload job not found");
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});
    }

    private static final class RecordingExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private int queuedTasks() {
            return tasks.size();
        }

        private void runNext() {
            tasks.remove().run();
        }
    }
}
