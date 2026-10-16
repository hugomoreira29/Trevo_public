package pt.trevo.trevo_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.PayoutBalanceResponse;
import pt.trevo.trevo_api.dto.PayoutPageResponse;
import pt.trevo.trevo_api.dto.PayoutResponse;
import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.PayoutService;

@RestController
@RequestMapping("/api/payouts")
@PreAuthorize("hasRole('SHOP_OWNER')")
public class PayoutController {

    private final PayoutService payoutService;
    private final SecurityUtils securityUtils;

    public PayoutController(PayoutService payoutService, SecurityUtils securityUtils) {
        this.payoutService = payoutService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/pending-balance")
    public PayoutBalanceResponse getPendingBalance(Authentication authentication) {
        Long ownerId = securityUtils.requireUserId(authentication);
        return new PayoutBalanceResponse(payoutService.getPendingBalanceForOwner(ownerId));
    }

    @GetMapping("/history")
    public PayoutPageResponse getPayoutHistory(
        Authentication authentication,
        @PageableDefault(size = 20, sort = "releasedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long ownerId = securityUtils.requireUserId(authentication);
        Page<Payout> payouts = payoutService.getPayoutHistoryForOwner(ownerId, pageable);
        return new PayoutPageResponse(
            payouts.getTotalElements(),
            payouts.getTotalPages(),
            payouts.getNumber(),
            payouts.getSize(),
            payouts.getContent().stream()
                .map(this::toResponse)
                .toList()
        );
    }

    private PayoutResponse toResponse(Payout payout) {
        return new PayoutResponse(
            payout.getId(),
            payout.getOrder().getId(),
            payout.getPayoutAmount(),
            payout.getStatus(),
            payout.getCreatedAt(),
            payout.getReleasedAt()
        );
    }
}
