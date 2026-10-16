package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.dto.TryOnImageResponse;
import pt.trevo.trevo_api.dto.TryOnUsageResponse;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CloudinaryService;
import pt.trevo.trevo_api.service.TryOnService;

@WebMvcTest(TryOnController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class TryOnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TryOnService tryOnService;
    @MockitoBean private CloudinaryService cloudinaryService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void postTryOnReturnsOkWithSinglePhoto() throws Exception {
        when(tryOnService.generateTryOn(eq(10L), eq(20L), any()))
            .thenReturn(new TryOnImageResponse("https://ai.example/result.png"));

        mockMvc.perform(multipart("/api/buyer/try-on")
                .file(photo("photo.jpg"))
                .param("productId", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageUrl").value("https://ai.example/result.png"));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void postTryOnReturnsPaymentRequiredWhenFreeTierExhausted() throws Exception {
        when(tryOnService.generateTryOn(eq(10L), eq(20L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Free users can generate up to 3 try-on images"));

        mockMvc.perform(multipart("/api/buyer/try-on")
                .file(photo("photo.jpg"))
                .param("productId", "20"))
            .andExpect(status().isPaymentRequired());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void postTryOnRejectsUnsupportedPhotoType() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile("photo", "photo.txt", "text/plain", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/buyer/try-on")
                .file(textFile)
                .param("productId", "20"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void postTryOnRejectsOversizedPhoto() throws Exception {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile("photo", "large.jpg", "image/jpeg", oversized);

        mockMvc.perform(multipart("/api/buyer/try-on")
                .file(largeFile)
                .param("productId", "20"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void getUsageReturnsUsageStats() throws Exception {
        when(tryOnService.getUsage(10L))
            .thenReturn(new TryOnUsageResponse(false, 2, 1));

        mockMvc.perform(get("/api/buyer/try-on/usage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pro").value(false))
            .andExpect(jsonPath("$.generationsUsed").value(2))
            .andExpect(jsonPath("$.generationsLeft").value(1));
    }

    private MockMultipartFile photo(String filename) {
        return new MockMultipartFile("photo", filename, "image/jpeg", new byte[] {1, 2, 3});
    }
}
