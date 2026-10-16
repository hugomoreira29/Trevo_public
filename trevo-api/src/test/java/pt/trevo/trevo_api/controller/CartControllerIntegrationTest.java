package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
import pt.trevo.trevo_api.dto.AddCartItemRequest;
import pt.trevo.trevo_api.dto.CartItemResponse;
import pt.trevo.trevo_api.dto.CartResponse;
import pt.trevo.trevo_api.dto.CartShopGroupResponse;
import pt.trevo.trevo_api.dto.UpdateCartItemRequest;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CartService;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class CartControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

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

    private CartResponse emptyCart() {
        return new CartResponse(List.of(), BigDecimal.ZERO);
    }

    private CartResponse cartWithOneItem() {
        CartItemResponse item = new CartItemResponse(1L, 101L, 201L, "Linen Shirt", null, "M", "Blue", 2, new BigDecimal("29.99"), new BigDecimal("59.98"));
        CartShopGroupResponse group = new CartShopGroupResponse(301L, "North Shop", List.of(item), new BigDecimal("59.98"));
        return new CartResponse(List.of(group), new BigDecimal("59.98"));
    }

    // --- GET /api/buyer/cart ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getCartReturnsCartResponse() throws Exception {
        when(cartService.getCart(1L)).thenReturn(cartWithOneItem());

        mockMvc.perform(get("/api/buyer/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.grandTotal").value("59.98"))
            .andExpect(jsonPath("$.shops").isArray())
            .andExpect(jsonPath("$.shops[0].shopId").value(301))
            .andExpect(jsonPath("$.shops[0].items[0].variantId").value(101));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getCartReturnsEmptyCartWhenNoItems() throws Exception {
        when(cartService.getCart(1L)).thenReturn(emptyCart());

        mockMvc.perform(get("/api/buyer/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shops").isEmpty())
            .andExpect(jsonPath("$.grandTotal").value("0"));
    }

    @Test
    void getCartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/buyer/cart"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void getCartForbidsShopOwner() throws Exception {
        mockMvc.perform(get("/api/buyer/cart"))
            .andExpect(status().isForbidden());
    }

    // --- POST /api/buyer/cart/items ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void addItemReturnsCreatedWithCartResponse() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(101L, 2);
        when(cartService.addItem(eq(1L), eq(101L), eq(2))).thenReturn(cartWithOneItem());

        mockMvc.perform(post("/api/buyer/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shops[0].items[0].variantId").value(101))
            .andExpect(jsonPath("$.shops[0].items[0].quantity").value(2));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void addItemReturnsBadRequestWhenStockExceeded() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(101L, 99);
        when(cartService.addItem(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity exceeds available stock"));

        mockMvc.perform(post("/api/buyer/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void addItemReturnsNotFoundForUnknownVariant() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(999L, 1);
        when(cartService.addItem(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found"));

        mockMvc.perform(post("/api/buyer/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void addItemRequiresAuthentication() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(101L, 1);
        mockMvc.perform(post("/api/buyer/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void addItemForbidsShopOwner() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(101L, 1);
        mockMvc.perform(post("/api/buyer/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // --- PUT /api/buyer/cart/items/{variantId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateItemReturnsUpdatedCart() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);
        when(cartService.updateQuantity(eq(1L), eq(101L), eq(3))).thenReturn(cartWithOneItem());

        mockMvc.perform(put("/api/buyer/cart/items/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shops[0].items[0].variantId").value(101));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateItemReturnsBadRequestWhenStockExceeded() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(99);
        when(cartService.updateQuantity(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity exceeds available stock"));

        mockMvc.perform(put("/api/buyer/cart/items/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateItemReturnsNotFoundWhenItemNotInCart() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(1);
        when(cartService.updateQuantity(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        mockMvc.perform(put("/api/buyer/cart/items/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateItemRequiresAuthentication() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(1);
        mockMvc.perform(put("/api/buyer/cart/items/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // --- DELETE /api/buyer/cart/items/{variantId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void removeItemReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/buyer/cart/items/101"))
            .andExpect(status().isNoContent());

        verify(cartService).removeItem(1L, 101L);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void removeItemReturnsNotFoundWhenItemNotInCart() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"))
            .when(cartService).removeItem(1L, 999L);

        mockMvc.perform(delete("/api/buyer/cart/items/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void removeItemRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/buyer/cart/items/101"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void removeItemForbidsShopOwner() throws Exception {
        mockMvc.perform(delete("/api/buyer/cart/items/101"))
            .andExpect(status().isForbidden());
    }
}
