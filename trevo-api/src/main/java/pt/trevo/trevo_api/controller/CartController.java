package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.AddCartItemRequest;
import pt.trevo.trevo_api.dto.CartResponse;
import pt.trevo.trevo_api.dto.UpdateCartItemRequest;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CartService;

@RestController
@RequestMapping("/api/buyer/cart")
@PreAuthorize("hasRole('BUYER')")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    public CartController(CartService cartService, SecurityUtils securityUtils) {
        this.cartService = cartService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return cartService.getCart(buyerId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
        @Valid @RequestBody AddCartItemRequest request,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return cartService.addItem(buyerId, request.variantId(), request.quantity());
    }

    @PutMapping("/items/{variantId}")
    public CartResponse updateItem(
        @PathVariable Long variantId,
        @Valid @RequestBody UpdateCartItemRequest request,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return cartService.updateQuantity(buyerId, variantId, request.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
        @PathVariable Long variantId,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        cartService.removeItem(buyerId, variantId);
    }
}
