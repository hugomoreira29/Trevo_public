package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.controller.ImageUploadValidator;
import pt.trevo.trevo_api.controller.ShopController;
import pt.trevo.trevo_api.dto.UpdateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ShopController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class ShopControllerUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private CollectionService collectionService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateShopUpdatesDescriptionWhenOwner() throws Exception {
        Shop updatedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .description("Updated description")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopService.updateShop(eq(11L), eq(1L), any(UpdateShopRequest.class))).thenReturn(updatedShop);

        mockMvc.perform(
            put("/api/shops/{shopId}", 1L)
                .param("description", "Updated description")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateShopUploadsLogoWhenProvided() throws Exception {
        Shop updatedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .logoUrl("https://cloudinary.com/logo.jpg")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopService.updateShop(eq(11L), eq(1L), any(UpdateShopRequest.class))).thenReturn(updatedShop);

        MockMultipartFile logoFile = new MockMultipartFile(
            "logo",
            "logo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            new byte[] {1, 2, 3}
        );

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/api/shops/{shopId}", 1L)
                .file(logoFile)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logoUrl").value("https://cloudinary.com/logo.jpg"));
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateShopUploadsBannerWhenProvided() throws Exception {
        Shop updatedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .bannerUrl("https://cloudinary.com/banner.jpg")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopService.updateShop(eq(11L), eq(1L), any(UpdateShopRequest.class))).thenReturn(updatedShop);

        MockMultipartFile bannerFile = new MockMultipartFile(
            "banner",
            "banner.webp",
            "image/webp",
            new byte[] {1, 2, 3}
        );

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/api/shops/{shopId}", 1L)
                .file(bannerFile)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bannerUrl").value("https://cloudinary.com/banner.jpg"));
    }

    @Test
    @WithMockUser(username = "22", roles = "BUYER")
    void updateShopReturnsForbiddenWhenNotOwnerRole() throws Exception {
        mockMvc.perform(
            put("/api/shops/{shopId}", 1L)
                .param("description", "Updated")
        )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateShopReturnsNotFoundWhenShopDoesNotExist() throws Exception {
        when(shopService.updateShop(eq(11L), eq(999L), any(UpdateShopRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(
            put("/api/shops/{shopId}", 999L)
                .param("description", "Updated")
        )
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateShopRejectsUnsupportedImageTypesBeforeServiceCall() throws Exception {
        MockMultipartFile logoFile = new MockMultipartFile(
            "logo",
            "logo.svg",
            "image/svg+xml",
            "<svg></svg>".getBytes()
        );

        mockMvc.perform(
            multipart(HttpMethod.PUT, "/api/shops/{shopId}", 1L)
                .file(logoFile)
        )
            .andExpect(status().isBadRequest());
    }
}
