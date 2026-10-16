package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
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
import pt.trevo.trevo_api.dto.CheckoutRequest;
import pt.trevo.trevo_api.dto.OrderPageResponse;
import pt.trevo.trevo_api.dto.OrderResponse;
import pt.trevo.trevo_api.dto.UpdateOrderStatusRequest;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.OrderService;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class OrderControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

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

    private OrderResponse sampleOrder(Long id, OrderStatus status) {
        return new OrderResponse(
            id, 301L, "North Shop", status, null,
            new BigDecimal("59.98"), new BigDecimal("6.00"),
            PaymentMethod.MBWAY, null,
            Instant.parse("2025-06-01T10:00:00Z"), Instant.parse("2025-06-01T10:00:00Z"),
            List.of()
        );
    }

    private OrderPageResponse samplePage(List<OrderResponse> orders) {
        return new OrderPageResponse(orders.size(), 1, 0, 20, orders);
    }

    // --- POST /api/buyer/orders/checkout ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void checkoutReturnsCreatedWithOrderList() throws Exception {
        CheckoutRequest request = new CheckoutRequest(PaymentMethod.MBWAY, null);
        OrderResponse order = sampleOrder(10L, OrderStatus.PLACED);

        when(orderService.checkout(eq(1L), eq(PaymentMethod.MBWAY), any()))
            .thenReturn(new pt.trevo.trevo_api.dto.CheckoutResponse(List.of(order), null));

        mockMvc.perform(post("/api/buyer/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orders[0].id").value(10))
            .andExpect(jsonPath("$.orders[0].status").value("PLACED"))
            .andExpect(jsonPath("$[0].shopName").value("North Shop"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void checkoutReturnsBadRequestForEmptyCart() throws Exception {
        CheckoutRequest request = new CheckoutRequest(PaymentMethod.MBWAY, null);
        when(orderService.checkout(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));

        mockMvc.perform(post("/api/buyer/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void checkoutReturnsBadRequestOnStockConflict() throws Exception {
        CheckoutRequest request = new CheckoutRequest(PaymentMethod.MBWAY, null);
        when(orderService.checkout(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock conflict for variants: [101]"));

        mockMvc.perform(post("/api/buyer/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutRequiresAuthentication() throws Exception {
        CheckoutRequest request = new CheckoutRequest(PaymentMethod.MBWAY, null);
        mockMvc.perform(post("/api/buyer/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void checkoutForbidsShopOwner() throws Exception {
        CheckoutRequest request = new CheckoutRequest(PaymentMethod.MBWAY, null);
        mockMvc.perform(post("/api/buyer/orders/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/buyer/orders ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getBuyerOrdersReturnsPaginatedOrders() throws Exception {
        OrderPageResponse page = samplePage(List.of(sampleOrder(10L, OrderStatus.PLACED)));
        when(orderService.getBuyerOrders(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/buyer/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.orders[0].id").value(10))
            .andExpect(jsonPath("$.orders[0].status").value("PLACED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getBuyerOrdersSupportsPagination() throws Exception {
        OrderPageResponse page = samplePage(List.of());
        when(orderService.getBuyerOrders(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/buyer/orders?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    void getBuyerOrdersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/buyer/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void getBuyerOrdersForbidsShopOwner() throws Exception {
        mockMvc.perform(get("/api/buyer/orders"))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/buyer/orders/{orderId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getBuyerOrderByIdReturnsOrder() throws Exception {
        OrderResponse order = sampleOrder(10L, OrderStatus.CONFIRMED);
        when(orderService.getBuyerOrder(eq(10L), eq(1L))).thenReturn(order);

        mockMvc.perform(get("/api/buyer/orders/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getBuyerOrderByIdReturnsForbiddenForOtherBuyerOrder() throws Exception {
        when(orderService.getBuyerOrder(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/buyer/orders/10"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getBuyerOrderByIdReturnsNotFoundForUnknownOrder() throws Exception {
        when(orderService.getBuyerOrder(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mockMvc.perform(get("/api/buyer/orders/999"))
            .andExpect(status().isNotFound());
    }

    // --- POST /api/buyer/orders/{orderId}/cancel ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void cancelOrderReturnsCancelledOrder() throws Exception {
        OrderResponse cancelled = sampleOrder(10L, OrderStatus.CANCELLED);
        when(orderService.cancelOrder(eq(10L), eq(1L))).thenReturn(cancelled);

        mockMvc.perform(post("/api/buyer/orders/10/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void cancelOrderReturnsBadRequestWhenAlreadyConfirmed() throws Exception {
        when(orderService.cancelOrder(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order can only be cancelled while in PLACED status"));

        mockMvc.perform(post("/api/buyer/orders/10/cancel"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void cancelOrderReturnsForbiddenForOtherBuyerOrder() throws Exception {
        when(orderService.cancelOrder(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyer can cancel order"));

        mockMvc.perform(post("/api/buyer/orders/10/cancel"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancelOrderRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/buyer/orders/10/cancel"))
            .andExpect(status().isUnauthorized());
    }

    // --- GET /api/orders ---

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void getShopOrdersReturnsPaginatedOrders() throws Exception {
        OrderPageResponse page = samplePage(List.of(sampleOrder(10L, OrderStatus.PLACED), sampleOrder(11L, OrderStatus.CONFIRMED)));
        when(orderService.getShopOrders(eq(2L), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.orders[0].id").value(10))
            .andExpect(jsonPath("$.orders[1].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void getShopOrdersSupportsPagination() throws Exception {
        OrderPageResponse page = samplePage(List.of());
        when(orderService.getShopOrders(eq(2L), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders?page=1&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void getShopOrdersReturnsNotFoundWhenNoShop() throws Exception {
        when(orderService.getShopOrders(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getShopOrdersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getShopOrdersForbidsBuyer() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isForbidden());
    }

    // --- PATCH /api/orders/{orderId}/status ---

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void updateOrderStatusReturnsUpdatedOrder() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        OrderResponse updated = sampleOrder(10L, OrderStatus.CONFIRMED);

        when(orderService.updateStatus(eq(10L), eq(OrderStatus.CONFIRMED), eq(2L))).thenReturn(updated);

        mockMvc.perform(patch("/api/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void updateOrderStatusReturnsBadRequestForInvalidTransition() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);
        when(orderService.updateStatus(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition"));

        mockMvc.perform(patch("/api/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void updateOrderStatusReturnsForbiddenForNonOwner() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        when(orderService.updateStatus(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can update order status"));

        mockMvc.perform(patch("/api/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatusRequiresAuthentication() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        mockMvc.perform(patch("/api/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateOrderStatusForbidsBuyer() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        mockMvc.perform(patch("/api/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
