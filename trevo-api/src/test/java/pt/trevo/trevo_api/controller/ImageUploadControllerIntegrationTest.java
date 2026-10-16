package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.dto.AsyncImageUploadAcceptedResponse;
import pt.trevo.trevo_api.dto.AsyncImageUploadStatusResponse;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.AsyncImageUploadService;
import pt.trevo.trevo_api.service.AsyncImageUploadStatus;

@WebMvcTest(ImageUploadController.class)
@Import({SecurityConfig.class, ImageUploadValidator.class, SecurityUtils.class})
class ImageUploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsyncImageUploadService asyncImageUploadService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void uploadImageReturnsAcceptedJobWithoutWaitingForCloudinaryUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] {1, 2, 3}
        );

        when(asyncImageUploadService.startUpload(any(), eq(11L))).thenReturn(
            new AsyncImageUploadAcceptedResponse(
                "upload-123",
                AsyncImageUploadStatus.PENDING,
                "/api/uploads/images/upload-123"
            )
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.uploadId").value("upload-123"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.statusUrl").value("/api/uploads/images/upload-123"));
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void uploadImageRejectsUnsupportedImageTypesBeforeServiceCall() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.svg",
            "image/svg+xml",
            "<svg></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file))
            .andExpect(status().isBadRequest());

        verify(asyncImageUploadService, never()).startUpload(any(), any());
    }

    @Test
    void uploadImageRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/uploads/images").file(file))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void getUploadStatusReturnsCompletedCloudinaryResult() throws Exception {
        when(asyncImageUploadService.getStatus("upload-123", 11L)).thenReturn(
            new AsyncImageUploadStatusResponse(
                "upload-123",
                AsyncImageUploadStatus.COMPLETED,
                new CloudinaryUploadResult("trevo/logo", "https://res.cloudinary.com/demo/image/upload/logo.png"),
                null,
                Instant.parse("2026-05-15T10:00:00Z"),
                Instant.parse("2026-05-15T10:00:02Z")
            )
        );

        mockMvc.perform(get("/api/uploads/images/upload-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uploadId").value("upload-123"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.publicId").value("trevo/logo"))
            .andExpect(jsonPath("$.result.url").value("https://res.cloudinary.com/demo/image/upload/logo.png"));
    }
}
