package pt.trevo.trevo_api.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.SubscriptionResponse;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.SubscriptionService;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SecurityUtils securityUtils;

    public SubscriptionController(SubscriptionService subscriptionService, SecurityUtils securityUtils) {
        this.subscriptionService = subscriptionService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/api/buyer/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public SubscriptionResponse createSubscription(Authentication authentication) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return subscriptionService.createSubscription(buyerId);
    }

    @DeleteMapping("/api/buyer/subscription")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BUYER')")
    public void cancelSubscription(Authentication authentication) {
        Long buyerId = securityUtils.requireUserId(authentication);
        subscriptionService.cancelSubscription(buyerId);
    }

    @GetMapping("/api/buyer/subscription/pro-status")
    @PreAuthorize("hasRole('BUYER')")
    public Map<String, Boolean> proStatus(Authentication authentication) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return Map.of("pro", subscriptionService.isProUser(buyerId));
    }

}
