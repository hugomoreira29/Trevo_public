package pt.trevo.trevo_api.service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.controller.ImageUploadValidator;
import pt.trevo.trevo_api.controller.ShopController;
import pt.trevo.trevo_api.dto.CollectionResponse;
import pt.trevo.trevo_api.dto.ShopProfileResponse;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ShopController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class ShopControllerPublicProfileIntegrationTest {

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
    void getShopProfileReturnsShopDataWithoutAuthentication() throws Exception {
        ShopProfileResponse profile = new ShopProfileResponse(
            1L, 10L, "My Shop", "A description",
            "https://logo.url", "https://banner.url",
            ShopStatus.ACTIVE, 4.5,
            List.of(new CollectionResponse(1L, 1L, "Summer", 0))
        );

        when(shopService.getPublicProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/shops/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.ownerId").value(10))
            .andExpect(jsonPath("$.name").value("My Shop"))
            .andExpect(jsonPath("$.description").value("A description"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.collections[0].name").value("Summer"))
            .andExpect(jsonPath("$.collections[0].displayOrder").value(0));
    }

    @Test
    void getShopProfileReturnsNullRatingWhenNoReviews() throws Exception {
        ShopProfileResponse profile = new ShopProfileResponse(
            1L, 10L, "My Shop", null,
            null, null,
            ShopStatus.ACTIVE, null,
            List.of()
        );

        when(shopService.getPublicProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/shops/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").doesNotExist())
            .andExpect(jsonPath("$.collections").isArray())
            .andExpect(jsonPath("$.collections").isEmpty());
    }

    @Test
    void getShopProfileReturnsNotFoundWhenShopIsDeactivated() throws Exception {
        when(shopService.getPublicProfile(1L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(get("/api/shops/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getShopProfileReturnsNotFoundWhenShopDoesNotExist() throws Exception {
        when(shopService.getPublicProfile(999L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(get("/api/shops/999"))
            .andExpect(status().isNotFound());
    }
}
