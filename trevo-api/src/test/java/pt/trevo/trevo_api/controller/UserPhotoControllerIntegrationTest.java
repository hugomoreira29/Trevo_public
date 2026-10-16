package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import pt.trevo.trevo_api.dto.UserPhotoResponse;
import pt.trevo.trevo_api.dto.UserPhotosResponse;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.UserPhotoService;

@WebMvcTest(UserPhotoController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class UserPhotoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserPhotoService userPhotoService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void uploadPhotosReturnsCreatedWhenBuyerSubmitsExactlyThreePhotos() throws Exception {
        when(userPhotoService.uploadPhotos(eq(10L), any())).thenReturn(new UserPhotosResponse(List.of(
            new UserPhotoResponse(1L, "https://res.cloudinary.com/demo/photo-1.jpg", Instant.parse("2026-05-09T10:00:00Z")),
            new UserPhotoResponse(2L, "https://res.cloudinary.com/demo/photo-2.jpg", Instant.parse("2026-05-09T10:00:00Z")),
            new UserPhotoResponse(3L, "https://res.cloudinary.com/demo/photo-3.jpg", Instant.parse("2026-05-09T10:00:00Z"))
        )));

        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg"))
                .file(photo("photo-3.jpg")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.photos.length()").value(3))
            .andExpect(jsonPath("$.photos[0].url").value("https://res.cloudinary.com/demo/photo-1.jpg"))
            .andExpect(jsonPath("$.photos[1].url").value("https://res.cloudinary.com/demo/photo-2.jpg"))
            .andExpect(jsonPath("$.photos[2].url").value("https://res.cloudinary.com/demo/photo-3.jpg"));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void uploadPhotosRejectsFewerThanThreePhotos() throws Exception {
        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg")))
            .andExpect(status().isBadRequest());

        verify(userPhotoService, never()).uploadPhotos(any(), any());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void uploadPhotosRejectsMoreThanThreePhotos() throws Exception {
        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg"))
                .file(photo("photo-3.jpg"))
                .file(photo("photo-4.jpg")))
            .andExpect(status().isBadRequest());

        verify(userPhotoService, never()).uploadPhotos(any(), any());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void uploadPhotosRejectsUnsupportedImageTypesBeforeServiceCall() throws Exception {
        MockMultipartFile svg = new MockMultipartFile("photos", "photo.svg", "image/svg+xml", "<svg></svg>".getBytes());

        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg"))
                .file(svg))
            .andExpect(status().isBadRequest());

        verify(userPhotoService, never()).uploadPhotos(any(), any());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void uploadPhotosReturnsForbiddenWhenCallerIsNotBuyer() throws Exception {
        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg"))
                .file(photo("photo-3.jpg")))
            .andExpect(status().isForbidden());
    }

    @Test
    void uploadPhotosReturnsUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(multipart("/api/buyer/photos")
                .file(photo("photo-1.jpg"))
                .file(photo("photo-2.jpg"))
                .file(photo("photo-3.jpg")))
            .andExpect(status().isUnauthorized());
    }

    private MockMultipartFile photo(String filename) {
        return new MockMultipartFile("photos", filename, MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
    }
}
