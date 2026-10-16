package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.dto.CreatePaymentIntentRequest;
import pt.trevo.trevo_api.dto.PaymentIntentResponse;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.PaymentService;
import pt.trevo.trevo_api.service.StripeWebhookService;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class PaymentControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private OrderRepository orderRepository;

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
    @WithMockUser(username = "10", roles = "BUYER")
    void createPaymentIntentReturnsStripeClientSecretForBuyerOrder() throws Exception {
        Order order = order(77L, 10L, PaymentMethod.STRIPE);
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));
        when(paymentService.createPaymentIntent(order))
            .thenReturn(new PaymentIntentResponse("pi_123", "pi_123_secret_abc", 2999L, "eur"));

        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("pi_123"))
            .andExpect(jsonPath("$.clientSecret").value("pi_123_secret_abc"))
            .andExpect(jsonPath("$.amount").value(2999))
            .andExpect(jsonPath("$.currency").value("eur"));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void createPaymentIntentReturnsForbiddenForAnotherBuyerOrder() throws Exception {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order(77L, 99L, PaymentMethod.STRIPE)));

        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void createPaymentIntentReturnsBadRequestForNonStripeOrder() throws Exception {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order(77L, 10L, PaymentMethod.MBWAY)));

        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void createPaymentIntentReturnsConflictWhenOrderIsNotAwaitingPayment() throws Exception {
        Order order = order(77L, 10L, PaymentMethod.STRIPE);
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isConflict());

        verify(paymentService, never()).createPaymentIntent(any(Order.class));
    }

    @Test
    void createPaymentIntentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void createPaymentIntentForbidsShopOwner() throws Exception {
        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(77L))))
            .andExpect(status().isForbidden());
    }

    @Test
    void stripeWebhookEndpointAcceptsUnsignedAuthenticatedByStripePayloadWithoutAppAuth() throws Exception {
        mockMvc.perform(post("/api/payments/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=123,v1=abc")
                .content("{\"id\":\"evt_123\"}"))
            .andExpect(status().isOk());

        verify(stripeWebhookService).handleWebhook("{\"id\":\"evt_123\"}", "t=123,v1=abc");
    }

    private Order order(Long id, Long buyerId, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.setId(id);
        order.setBuyer(User.builder().id(buyerId).build());
        order.setShop(Shop.builder().id(301L).name("North Shop").owner(User.builder().id(901L).build()).build());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(new BigDecimal("29.99"));
        order.setCommissionAmount(new BigDecimal("3.00"));
        order.setCreatedAt(Instant.parse("2026-05-15T10:00:00Z"));
        return order;
    }
}
