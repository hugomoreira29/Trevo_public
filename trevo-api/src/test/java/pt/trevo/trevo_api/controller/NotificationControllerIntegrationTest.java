package pt.trevo.trevo_api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.dto.NotificationResponse;
import pt.trevo.trevo_api.dto.NotificationsResponse;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void getNotificationsReturnsUnreadCountAndListForShopOwner() throws Exception {
        NotificationsResponse response = new NotificationsResponse(
            1L,
            1,
            10,
            25L,
            3,
            List.of(new NotificationResponse(
                10L,
                77L,
                "NEW_ORDER",
                "Nova encomenda recebida",
                "Recebeste uma nova encomenda na loja North Shop.",
                false,
                Instant.parse("2026-05-08T10:00:00Z"),
                null
            ))
        );

        when(notificationService.getNotifications(901L, 1, 10)).thenReturn(response);

        mockMvc.perform(get("/api/notifications?page=1&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.notifications[0].id").value(10))
            .andExpect(jsonPath("$.notifications[0].orderId").value(77))
            .andExpect(jsonPath("$.notifications[0].type").value("NEW_ORDER"))
            .andExpect(jsonPath("$.notifications[0].read").value(false));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void getNotificationsReturnsForbiddenWhenCallerIsNotShopOwner() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getNotificationsReturnsUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "901", roles = "SHOP_OWNER")
    void markAsReadReturnsUpdatedNotificationForShopOwner() throws Exception {
        when(notificationService.markAsRead(10L, 901L)).thenReturn(new NotificationResponse(
            10L,
            77L,
            "NEW_ORDER",
            "Nova encomenda recebida",
            "Recebeste uma nova encomenda na loja North Shop.",
            true,
            Instant.parse("2026-05-08T10:00:00Z"),
            Instant.parse("2026-05-08T11:00:00Z")
        ));

        mockMvc.perform(patch("/api/notifications/10/read"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.read").value(true))
            .andExpect(jsonPath("$.readAt").value("2026-05-08T11:00:00Z"));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void markAsReadReturnsForbiddenWhenCallerIsNotShopOwner() throws Exception {
        mockMvc.perform(patch("/api/notifications/10/read"))
            .andExpect(status().isForbidden());
    }
}
