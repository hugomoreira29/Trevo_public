package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

class CloudinaryServiceTest extends BaseUnitTest {

    private static final byte[] PNG_BYTES = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47,
        0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52
    };

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryService(cloudinary, 5_242_880);
    }

    private ResponseStatusException expectResponseStatus(Throwable throwable, HttpStatus status) {
        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) throwable;
        assertThat(exception.getStatusCode()).isEqualTo(status);
        return exception;
    }

    @Test
    void uploadImageReturnsUrlAndPublicIdForValidImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shirt.png",
            "image/png",
            PNG_BYTES
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(
            Map.of(
                "public_id", "trevo/products/shirt",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/shirt.png"
            )
        );

        CloudinaryUploadResult result = cloudinaryService.uploadImage(file);

        assertThat(result.publicId()).isEqualTo("trevo/products/shirt");
        assertThat(result.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/shirt.png");
        verify(uploader).upload(any(byte[].class), any(Map.class));
    }

    @Test
    void uploadImageRejectsNonImageFiles() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "notes.txt",
            "image/png",
            "not-an-image".getBytes()
        );

        Throwable thrown = catchThrowable(() -> cloudinaryService.uploadImage(file));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Only image files are allowed");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void deleteImageDelegatesToCloudinaryUploader() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);

        cloudinaryService.deleteImage("trevo/products/shirt");

        verify(uploader).destroy(eq("trevo/products/shirt"), any(Map.class));
    }

    @Test
    void deleteImageRejectsBlankPublicId() {
        Throwable thrown = catchThrowable(() -> cloudinaryService.deleteImage("  "));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Image publicId is required");
    }

    @Test
    void uploadImageWrapsCloudinaryIoFailures() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shirt.png",
            "image/png",
            PNG_BYTES
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));

        Throwable thrown = catchThrowable(() -> cloudinaryService.uploadImage(file));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("Failed to upload image to Cloudinary");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
    }

    @Test
    void uploadImageFallsBackToUrlWhenSecureUrlIsMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shirt.png",
            "image/png",
            PNG_BYTES
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(
            Map.of(
                "public_id", "trevo/products/shirt",
                "url", "http://res.cloudinary.com/demo/image/upload/shirt.png"
            )
        );

        CloudinaryUploadResult result = cloudinaryService.uploadImage(file);

        assertThat(result.publicId()).isEqualTo("trevo/products/shirt");
        assertThat(result.url()).isEqualTo("http://res.cloudinary.com/demo/image/upload/shirt.png");
    }

    @Test
    void uploadImageRejectsNullFile() {
        Throwable thrown = catchThrowable(() -> cloudinaryService.uploadImage(null));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Image file is required");
    }

    @Test
    void uploadImageRejectsFilesAboveConfiguredLimit() {
        CloudinaryService limitedService = new CloudinaryService(cloudinary, 8);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shirt.png",
            "image/png",
            PNG_BYTES
        );

        Throwable thrown = catchThrowable(() -> limitedService.uploadImage(file));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Image file exceeds maximum allowed size");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImageRejectsWhenCloudinaryResponseMissesPublicId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shirt.png",
            "image/png",
            PNG_BYTES
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(
            Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/shirt.png")
        );

        Throwable thrown = catchThrowable(() -> cloudinaryService.uploadImage(file));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("Cloudinary response did not include image identifiers");
    }

    @Test
    void deleteImageWrapsCloudinaryIoFailures() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(String.class), any(Map.class))).thenThrow(new IOException("boom"));

        Throwable thrown = catchThrowable(() -> cloudinaryService.deleteImage("trevo/products/shirt"));

        ResponseStatusException exception = expectResponseStatus(thrown, HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("Failed to delete image from Cloudinary");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
    }
}
