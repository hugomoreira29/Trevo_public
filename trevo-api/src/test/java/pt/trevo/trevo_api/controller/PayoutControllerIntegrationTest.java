package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.persistence.entity.PayoutStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.PayoutService;

@WebMvcTest(PayoutController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class PayoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayoutService payoutService;

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
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void getPendingBalanceReturnsOwnerBalance() throws Exception {
        when(payoutService.getPendingBalanceForOwner(901L)).thenReturn(new BigDecimal("270.00"));

        mockMvc.perform(get("/api/payouts/pending-balance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingBalance").value(270.00));
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void getPayoutHistoryReturnsReleasedPayouts() throws Exception {
        Payout payout = payout(1L, 77L, "90.00", Instant.parse("2026-05-10T10:00:00Z"));
        when(payoutService.getPayoutHistoryForOwner(eq(901L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/payouts/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.payouts[0].id").value(1))
            .andExpect(jsonPath("$.payouts[0].orderId").value(77))
            .andExpect(jsonPath("$.payouts[0].payoutAmount").value(90.00))
            .andExpect(jsonPath("$.payouts[0].status").value("RELEASED"))
            .andExpect(jsonPath("$.payouts[0].releasedAt").value("2026-05-10T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void getPayoutHistoryPassesPaginationToService() throws Exception {
        Payout payout = payout(1L, 77L, "90.00", Instant.parse("2026-05-10T10:00:00Z"));
        when(payoutService.getPayoutHistoryForOwner(eq(901L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(payout), PageRequest.of(1, 5), 12));

        mockMvc.perform(get("/api/payouts/history?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.payouts[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void getPayoutHistoryReturnsNotFoundWhenOwnerHasNoShop() throws Exception {
        when(payoutService.getPayoutHistoryForOwner(eq(901L), any(Pageable.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(get("/api/payouts/history"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void payoutEndpointsForbidBuyers() throws Exception {
        mockMvc.perform(get("/api/payouts/history"))
            .andExpect(status().isForbidden());
    }

    @Test
    void payoutEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/payouts/history"))
            .andExpect(status().isUnauthorized());
    }

    private Payout payout(Long id, Long orderId, String amount, Instant releasedAt) {
        Order order = new Order();
        order.setId(orderId);
        order.setShop(Shop.builder().id(301L).name("North Shop").build());

        Payout payout = new Payout();
        payout.setId(id);
        payout.setOrder(order);
        payout.setPayoutAmount(new BigDecimal(amount));
        payout.setStatus(PayoutStatus.RELEASED);
        payout.setCreatedAt(releasedAt.minusSeconds(3600));
        payout.setReleasedAt(releasedAt);
        return payout;
    }
}
