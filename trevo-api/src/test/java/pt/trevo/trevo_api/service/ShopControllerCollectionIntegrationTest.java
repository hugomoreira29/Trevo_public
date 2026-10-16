package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.util.List;
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
import pt.trevo.trevo_api.dto.CreateCollectionRequest;
import pt.trevo.trevo_api.dto.RenameCollectionRequest;
import pt.trevo.trevo_api.dto.ReorderCollectionsRequest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ShopController.class)
@Import({SecurityConfig.class, SecurityUtils.class, ImageUploadValidator.class})
class ShopControllerCollectionIntegrationTest {

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

    // ── POST /api/shops/{shopId}/collections ─────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void createCollectionReturnsCreatedWhenAuthorized() throws Exception {
        CreateCollectionRequest request = new CreateCollectionRequest("Summer");
        Shop shop = Shop.builder().id(10L).owner(User.builder().id(11L).build()).build();
        Collection saved = Collection.builder().id(1L).shop(shop).name("Summer").displayOrder(0).build();

        when(collectionService.createCollection(eq(11L), eq(10L), eq("Summer"))).thenReturn(saved);

        mockMvc.perform(post("/api/shops/10/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.shopId").value(10))
            .andExpect(jsonPath("$.name").value("Summer"))
            .andExpect(jsonPath("$.displayOrder").value(0));
    }

    @Test
    @WithMockUser(username = "11", roles = "BUYER")
    void createCollectionReturnsForbiddenWhenNotShopOwner() throws Exception {
        mockMvc.perform(post("/api/shops/10/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCollectionRequest("Summer"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createCollectionReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/shops/10/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCollectionRequest("Summer"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void createCollectionReturnsNotFoundWhenShopDoesNotExist() throws Exception {
        when(collectionService.createCollection(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(post("/api/shops/999/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCollectionRequest("Summer"))))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "99", roles = "SHOP_OWNER")
    void createCollectionReturnsForbiddenWhenCallerDoesNotOwnShop() throws Exception {
        when(collectionService.createCollection(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections"));

        mockMvc.perform(post("/api/shops/10/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCollectionRequest("Summer"))))
            .andExpect(status().isForbidden());
    }

    // ── PATCH /api/shops/{shopId}/collections/{collectionId} ─────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void renameCollectionReturnsOkWhenAuthorized() throws Exception {
        RenameCollectionRequest request = new RenameCollectionRequest("New Name");
        Shop shop = Shop.builder().id(10L).owner(User.builder().id(11L).build()).build();
        Collection updated = Collection.builder().id(5L).shop(shop).name("New Name").displayOrder(0).build();

        when(collectionService.renameCollection(eq(11L), eq(5L), eq("New Name"))).thenReturn(updated);

        mockMvc.perform(patch("/api/shops/10/collections/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @WithMockUser(username = "11", roles = "BUYER")
    void renameCollectionReturnsForbiddenWhenNotShopOwner() throws Exception {
        mockMvc.perform(patch("/api/shops/10/collections/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RenameCollectionRequest("Name"))))
            .andExpect(status().isForbidden());
    }

    // ── PUT /api/shops/{shopId}/collections/reorder ──────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void reorderCollectionsReturnsOkWithUpdatedOrder() throws Exception {
        ReorderCollectionsRequest request = new ReorderCollectionsRequest(List.of(3L, 1L, 2L));
        Shop shop = Shop.builder().id(10L).owner(User.builder().id(11L).build()).build();
        List<Collection> reordered = List.of(
            Collection.builder().id(3L).shop(shop).name("C").displayOrder(0).build(),
            Collection.builder().id(1L).shop(shop).name("A").displayOrder(1).build(),
            Collection.builder().id(2L).shop(shop).name("B").displayOrder(2).build()
        );

        when(collectionService.reorderCollections(eq(11L), eq(10L), eq(List.of(3L, 1L, 2L))))
            .thenReturn(reordered);

        mockMvc.perform(put("/api/shops/10/collections/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[0].displayOrder").value(0))
            .andExpect(jsonPath("$[1].id").value(1))
            .andExpect(jsonPath("$[1].displayOrder").value(1));
    }

    @Test
    @WithMockUser(username = "11", roles = "BUYER")
    void reorderCollectionsReturnsForbiddenWhenNotShopOwner() throws Exception {
        mockMvc.perform(put("/api/shops/10/collections/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReorderCollectionsRequest(List.of(1L)))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void reorderCollectionsReturnsBadRequestWhenIdsMismatch() throws Exception {
        when(collectionService.reorderCollections(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collection IDs list must include all shop collections"));

        mockMvc.perform(put("/api/shops/10/collections/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReorderCollectionsRequest(List.of(1L)))))
            .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/shops/{shopId}/collections/{collectionId} ────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void deleteCollectionReturnsNoContentWhenAuthorized() throws Exception {
        doNothing().when(collectionService).deleteCollection(eq(11L), eq(5L));

        mockMvc.perform(delete("/api/shops/10/collections/5"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "11", roles = "BUYER")
    void deleteCollectionReturnsForbiddenWhenNotShopOwner() throws Exception {
        mockMvc.perform(delete("/api/shops/10/collections/5"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteCollectionReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/shops/10/collections/5"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void deleteCollectionReturnsNotFoundWhenCollectionDoesNotExist() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"))
            .when(collectionService).deleteCollection(any(), any());

        mockMvc.perform(delete("/api/shops/10/collections/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "99", roles = "SHOP_OWNER")
    void deleteCollectionReturnsForbiddenWhenCallerDoesNotOwnShop() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections"))
            .when(collectionService).deleteCollection(any(), any());

        mockMvc.perform(delete("/api/shops/10/collections/5"))
            .andExpect(status().isForbidden());
    }
}
