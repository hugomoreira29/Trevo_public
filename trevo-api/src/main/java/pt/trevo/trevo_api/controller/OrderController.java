package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.CheckoutRequest;
import pt.trevo.trevo_api.dto.CheckoutResponse;
import pt.trevo.trevo_api.dto.OrderPageResponse;
import pt.trevo.trevo_api.dto.OrderResponse;
import pt.trevo.trevo_api.dto.UpdateOrderStatusRequest;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.OrderService;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderController(OrderService orderService, SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/api/buyer/orders/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public CheckoutResponse checkout(
        @Valid @RequestBody CheckoutRequest request,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return orderService.checkout(buyerId, request.paymentMethod(), request.phone());
    }

    @GetMapping("/api/buyer/orders")
    @PreAuthorize("hasRole('BUYER')")
    public OrderPageResponse getBuyerOrders(
        Authentication authentication,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return orderService.getBuyerOrders(buyerId, pageable);
    }

    @GetMapping("/api/buyer/orders/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse getBuyerOrder(
        @PathVariable Long orderId,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return orderService.getBuyerOrder(orderId, buyerId);
    }

    @PostMapping("/api/buyer/orders/{orderId}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse cancelOrder(
        @PathVariable Long orderId,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return orderService.cancelOrder(orderId, buyerId);
    }

    @GetMapping("/api/orders")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public OrderPageResponse getShopOrders(
        Authentication authentication,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long ownerId = securityUtils.requireUserId(authentication);
        return orderService.getShopOrders(ownerId, pageable);
    }

    @PatchMapping("/api/orders/{orderId}/status")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public OrderResponse updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusRequest request,
        Authentication authentication
    ) {
        Long ownerId = securityUtils.requireUserId(authentication);
        return orderService.updateStatus(orderId, request.status(), ownerId);
    }
}
