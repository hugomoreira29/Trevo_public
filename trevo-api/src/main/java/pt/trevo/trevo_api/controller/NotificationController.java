package pt.trevo.trevo_api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.NotificationResponse;
import pt.trevo.trevo_api.dto.NotificationsResponse;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public NotificationsResponse getNotifications(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long recipientId = securityUtils.requireUserId(authentication);
        return notificationService.getNotifications(recipientId, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public NotificationResponse markAsRead(
        Authentication authentication,
        @PathVariable Long notificationId
    ) {
        Long recipientId = securityUtils.requireUserId(authentication);
        return notificationService.markAsRead(notificationId, recipientId);
    }
}
