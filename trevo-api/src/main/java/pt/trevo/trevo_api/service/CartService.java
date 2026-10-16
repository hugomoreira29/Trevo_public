package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CartItemResponse;
import pt.trevo.trevo_api.dto.CartResponse;
import pt.trevo.trevo_api.dto.CartShopGroupResponse;
import pt.trevo.trevo_api.persistence.entity.Cart;
import pt.trevo.trevo_api.persistence.entity.CartItem;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.CartRepository;
import pt.trevo.trevo_api.persistence.repository.ProductImageRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final VariantRepository variantRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;

    public CartService(
        CartRepository cartRepository,
        VariantRepository variantRepository,
        UserRepository userRepository,
        ProductImageRepository productImageRepository
    ) {
        this.cartRepository = cartRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional
    public CartResponse addItem(Long buyerId, Long variantId, Integer quantity) {
        validateCartItemRequest(buyerId, variantId, quantity, "Invalid add item request");

        Variant variant = findVariant(variantId);
        validateStock(variant, quantity);
        Cart cart = findOrCreateCart(buyerId);
        CartItem item = findItem(cart, variantId);
        int newQuantity = quantity + (item != null ? item.getQuantity() : 0);
        validateStock(variant, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        } else {
            item.setQuantity(newQuantity);
        }

        cartRepository.save(cart);
        return toCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long buyerId) {
        if (buyerId == null) {
            throw badRequest("Invalid cart request");
        }

        Cart cart = cartRepository.findByBuyerIdWithItems(buyerId).orElse(null);
        if (cart == null) {
            return new CartResponse(List.of(), BigDecimal.ZERO);
        }

        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(Long buyerId, Long variantId, Integer quantity) {
        validateCartItemRequest(buyerId, variantId, quantity, "Invalid update quantity request");

        Cart cart = findCart(buyerId);
        CartItem item = findExistingItem(cart, variantId);
        validateStock(item.getVariant(), quantity);
        item.setQuantity(quantity);

        cartRepository.save(cart);
        return toCartResponse(cart);
    }

    @Transactional
    public void removeItem(Long buyerId, Long variantId) {
        if (buyerId == null || variantId == null) {
            throw badRequest("Invalid remove item request");
        }

        Cart cart = findCart(buyerId);
        CartItem item = findExistingItem(cart, variantId);
        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    private CartResponse toCartResponse(Cart cart) {
        Map<Long, String> primaryImages = loadPrimaryImages(cart);
        Map<Long, ShopGroupAccumulator> groups = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            Variant variant = item.getVariant();
            Product product = variant.getProduct();
            Shop shop = product.getShop();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            Long shopId = shop.getId();

            ShopGroupAccumulator group = groups.computeIfAbsent(
                shopId,
                ignored -> new ShopGroupAccumulator(shopId, shop.getName())
            );
            group.items.add(new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                primaryImages.get(product.getId()),
                variant.getSize(),
                variant.getColor(),
                item.getQuantity(),
                unitPrice,
                subtotal
            ));
            group.subtotal = group.subtotal.add(subtotal);
        }

        List<CartShopGroupResponse> shops = groups.values().stream()
            .map(g -> new CartShopGroupResponse(g.shopId, g.shopName, List.copyOf(g.items), g.subtotal))
            .toList();
        BigDecimal grandTotal = shops.stream()
            .map(CartShopGroupResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(shops, grandTotal);
    }

    private Cart findOrCreateCart(Long buyerId) {
        return cartRepository.findByBuyerIdWithItems(buyerId).orElseGet(() -> {
            User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));
            Cart cart = new Cart();
            cart.setBuyer(buyer);
            return cart;
        });
    }

    private Cart findCart(Long buyerId) {
        return cartRepository.findByBuyerIdWithItems(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
    }

    private Variant findVariant(Long variantId) {
        return variantRepository.findById(variantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found"));
    }

    private CartItem findExistingItem(Cart cart, Long variantId) {
        CartItem item = findItem(cart, variantId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        return item;
    }

    private CartItem findItem(Cart cart, Long variantId) {
        return cart.getItems().stream()
            .filter(item -> item.getVariant() != null && Objects.equals(item.getVariant().getId(), variantId))
            .findFirst()
            .orElse(null);
    }

    private Map<Long, String> loadPrimaryImages(Cart cart) {
        List<Long> productIds = cart.getItems().stream()
            .map(CartItem::getVariant)
            .filter(Objects::nonNull)
            .map(Variant::getProduct)
            .filter(Objects::nonNull)
            .map(Product::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Comparator<ProductImage> imageOrder = Comparator
            .comparing(ProductImage::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ProductImage::getId, Comparator.nullsLast(Long::compareTo));
        Map<Long, ProductImage> primaryImages = new HashMap<>();
        for (ProductImage image : productImageRepository.findByProductIdIn(productIds)) {
            Long productId = image.getProduct().getId();
            ProductImage current = primaryImages.get(productId);
            if (current == null || imageOrder.compare(image, current) < 0) {
                primaryImages.put(productId, image);
            }
        }

        Map<Long, String> imageUrls = new HashMap<>();
        primaryImages.forEach((productId, image) -> imageUrls.put(productId, image.getImageUrl()));
        return imageUrls;
    }

    private void validateCartItemRequest(Long buyerId, Long variantId, Integer quantity, String reason) {
        if (buyerId == null || variantId == null || quantity == null || quantity <= 0) {
            throw badRequest(reason);
        }
    }

    private void validateStock(Variant variant, Integer quantity) {
        Integer stock = variant.getStock();
        if (stock == null || quantity > stock) {
            throw badRequest("Requested quantity exceeds available stock");
        }
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private static class ShopGroupAccumulator {
        private final Long shopId;
        private final String shopName;
        private final ArrayList<CartItemResponse> items = new ArrayList<>();
        private BigDecimal subtotal = BigDecimal.ZERO;

        private ShopGroupAccumulator(Long shopId, String shopName) {
            this.shopId = shopId;
            this.shopName = shopName;
        }
    }
}
