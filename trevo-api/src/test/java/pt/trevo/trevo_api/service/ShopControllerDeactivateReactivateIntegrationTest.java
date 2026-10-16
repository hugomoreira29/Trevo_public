package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ShopController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class ShopControllerDeactivateReactivateIntegrationTest {

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
    @WithMockUser(username = "1", authorities = "ROLE_ADMIN")
    void deactivateShopAllowsAdminsToManageAnyShop() throws Exception {
        Shop deactivatedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopService.deactivateShop(eq(1L), eq(1L), eq(true))).thenReturn(deactivatedShop);

        mockMvc.perform(post("/api/shops/{shopId}/deactivate", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_ADMIN")
    void reactivateShopAllowsAdminsToManageAnyShop() throws Exception {
        Shop reactivatedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopService.reactivateShop(eq(1L), eq(1L), eq(true))).thenReturn(reactivatedShop);

        mockMvc.perform(post("/api/shops/{shopId}/reactivate", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "22", roles = "SHOP_OWNER")
    void deactivateShopReturnsForbiddenWhenShopOwnerDoesNotOwnTargetShop() throws Exception {
        when(shopService.deactivateShop(eq(1L), eq(22L), eq(false)))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can deactivate this shop"));

        mockMvc.perform(post("/api/shops/{shopId}/deactivate", 1L))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_ADMIN")
    void deactivateShopReturnsNotFoundWhenShopDoesNotExist() throws Exception {
        when(shopService.deactivateShop(eq(999L), eq(1L), eq(true)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(post("/api/shops/{shopId}/deactivate", 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_ADMIN")
    void reactivateShopReturnsNotFoundWhenShopDoesNotExist() throws Exception {
        when(shopService.reactivateShop(eq(999L), eq(1L), eq(true)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(post("/api/shops/{shopId}/reactivate", 999L))
            .andExpect(status().isNotFound());
    }
}
