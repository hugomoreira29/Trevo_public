package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import pt.trevo.trevo_api.dto.CreateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ShopController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class ShopControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void createShopReturnsCreatedShopWhenAuthorized() throws Exception {
        CreateShopRequest request = new CreateShopRequest("My Shop", "A description", "123456789", null, null);

        Shop savedShop = Shop.builder()
            .id(1L)
            .owner(User.builder().id(11L).build())
            .name("My Shop")
            .description("A description")
            .nif("123456789")
            .status(ShopStatus.PENDING)
            .build();

        when(shopService.createShop(eq(11L), any(CreateShopRequest.class))).thenReturn(savedShop);

        mockMvc.perform(
            post("/api/shops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.ownerId").value(11))
            .andExpect(jsonPath("$.name").value("My Shop"))
            .andExpect(jsonPath("$.description").value("A description"))
            .andExpect(jsonPath("$.nif").value("123456789"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "22", roles = "BUYER")
    void createShopReturnsForbiddenWhenCallerIsNotShopOwner() throws Exception {
        CreateShopRequest request = new CreateShopRequest("My Shop", "A description", "123456789", null, null);

        mockMvc.perform(
            post("/api/shops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void createShopReturnsConflictWhenOwnerAlreadyHasShop() throws Exception {
        CreateShopRequest request = new CreateShopRequest("My Shop", "A description", "123456789", null, null);

        when(shopService.createShop(eq(11L), any(CreateShopRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Owner already has a shop"));

        mockMvc.perform(
            post("/api/shops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict());
    }
}
