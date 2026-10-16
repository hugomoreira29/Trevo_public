package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.OrderResponse;
import pt.trevo.trevo_api.persistence.entity.Cart;
import pt.trevo.trevo_api.persistence.entity.CartItem;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderItem;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.CartRepository;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

class OrderServiceTest extends BaseUnitTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VariantRepository variantRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PayoutService payoutService;

    @Mock
    private PayPalService payPalService;

    @InjectMocks
    private OrderService orderService;

    // CART-B04: checkout

    @Test
    void multiShopCartCreatesOneOrderPerShop() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Shop north = shop(301L, 901L, "North Shop");
        Shop south = shop(302L, 902L, "South Shop");
        Variant v1 = variant(101L, product(201L, north, "Linen Shirt", "29.99"), "M", "Blue", 5);
        Variant v2 = variant(102L, product(202L, south, "Wool Scarf", "12.00"), "S", "Green", 6);
        Cart cart = cart(buyer, item(1L, v1, 1), item(2L, v2, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(v1, v2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        List<OrderResponse> orders = orderService.checkout(buyerId, PaymentMethod.MBWAY, null).orders();

        assertThat(orders).hasSize(2);
        assertThat(orders.stream().map(OrderResponse::shopId).toList())
                .containsExactlyInAnyOrder(301L, 302L);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void checkoutDecrementsVariantStockPerOrderItem() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Variant variant = variant(101L, product(201L, shop(301L, 901L, "North Shop"), "Linen Shirt", "29.99"), "M", "Blue", 5);
        Cart cart = cart(buyer, item(1L, variant, 2));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(variant));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.checkout(buyerId, PaymentMethod.MBWAY, null);

        assertThat(variant.getStock()).isEqualTo(3);
        verify(variantRepository).save(variant);
    }

    @Test
    void checkoutClearsCartAfterSuccess() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Variant variant = variant(101L, product(201L, shop(301L, 901L, "North Shop"), "Linen Shirt", "29.99"), "M", "Blue", 5);
        Cart cart = cart(buyer, item(1L, variant, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(variant));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.checkout(buyerId, PaymentMethod.MBWAY, null);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void checkoutCreatesNewOrderNotificationsForShopOwners() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Shop north = shop(301L, 901L, "North Shop");
        Shop south = shop(302L, 902L, "South Shop");
        Variant v1 = variant(101L, product(201L, north, "Linen Shirt", "29.99"), "M", "Blue", 5);
        Variant v2 = variant(102L, product(202L, south, "Wool Scarf", "12.00"), "S", "Green", 6);
        Cart cart = cart(buyer, item(1L, v1, 1), item(2L, v2, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(v1, v2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.checkout(buyerId, PaymentMethod.MBWAY, null);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(notificationService, times(2)).createNewOrderNotification(orderCaptor.capture());
        assertThat(orderCaptor.getAllValues().stream()
                .map(order -> order.getShop().getOwner().getId())
                .toList())
            .containsExactlyInAnyOrder(901L, 902L);
    }

    @Test
    void checkoutStillSucceedsWhenNotificationCreationFails() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Variant variant = variant(101L, product(201L, shop(301L, 901L, "North Shop"), "Linen Shirt", "29.99"), "M", "Blue", 5);
        Cart cart = cart(buyer, item(1L, variant, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(variant));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("notification DB unavailable"))
            .when(notificationService).createNewOrderNotification(any(Order.class));

        List<OrderResponse> orders = orderService.checkout(buyerId, PaymentMethod.MBWAY, null).orders();

        assertThat(orders).hasSize(1);
        assertThat(cart.getItems()).isEmpty();
        assertThat(variant.getStock()).isEqualTo(4);
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void checkoutWithStockConflictReturns400ListingAffectedVariants() {
        Long buyerId = 10L;
        User buyer = User.builder().id(buyerId).build();
        Variant variant = variant(101L, product(201L, shop(301L, 901L, "North Shop"), "Linen Shirt", "29.99"), "M", "Blue", 1);
        Cart cart = cart(buyer, item(1L, variant, 3));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(variantRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(variant));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.checkout(buyerId, PaymentMethod.MBWAY, null)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("101");
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).createNewOrderNotification(any());
    }

    // CART-B05: updateStatus

    @Test
    void validStatusTransitionSucceeds() {
        Long orderId = 1L;
        Long shopOwnerId = 901L;
        Shop shop = shop(301L, shopOwnerId, "North Shop");
        Order order = order(orderId, User.builder().id(10L).build(), shop, OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse updated = orderService.updateStatus(orderId, OrderStatus.CONFIRMED, shopOwnerId);

        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void invalidStatusTransitionThrows400() {
        Long orderId = 1L;
        Long shopOwnerId = 901L;
        Shop shop = shop(301L, shopOwnerId, "North Shop");
        Order order = order(orderId, User.builder().id(10L).build(), shop, OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.updateStatus(orderId, OrderStatus.SHIPPED, shopOwnerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void nonOwnerCannotUpdateStatusThrows403() {
        Long orderId = 1L;
        Long shopOwnerId = 901L;
        Long intruderId = 999L;
        Shop shop = shop(301L, shopOwnerId, "North Shop");
        Order order = order(orderId, User.builder().id(10L).build(), shop, OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.updateStatus(orderId, OrderStatus.CONFIRMED, intruderId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // PAY-B05: escrow release on DELIVERED

    @Test
    void transitionToDeliveredCreatesPayoutRecord() {
        Long orderId = 1L;
        Long shopOwnerId = 901L;
        Shop shop = shop(301L, shopOwnerId, "North Shop");
        Order order = order(orderId, User.builder().id(10L).build(), shop, OrderStatus.SHIPPED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payoutService.createPayout(any(Order.class))).thenReturn(new Payout());

        OrderResponse updated = orderService.updateStatus(orderId, OrderStatus.DELIVERED, shopOwnerId);

        assertThat(updated.status()).isEqualTo(OrderStatus.DELIVERED);
        verify(payoutService).createPayout(any(Order.class));
    }

    @Test
    void transitionToNonDeliveredStatusDoesNotCreatePayout() {
        Long orderId = 1L;
        Long shopOwnerId = 901L;
        Shop shop = shop(301L, shopOwnerId, "North Shop");
        Order order = order(orderId, User.builder().id(10L).build(), shop, OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(orderId, OrderStatus.CONFIRMED, shopOwnerId);

        verify(payoutService, never()).createPayout(any(Order.class));
    }

    // CART-B06: cancelOrder

    @Test
    void cancelWhilePlacedSucceedsAndRestoresStock() {
        Long orderId = 1L;
        Long buyerId = 10L;
        Shop shop = shop(301L, 901L, "North Shop");
        Variant variant = variant(101L, product(201L, shop, "Linen Shirt", "29.99"), "M", "Blue", 3);
        OrderItem orderItem = orderItem(1L, variant, 2, "29.99");
        Order order = order(orderId, User.builder().id(buyerId).build(), shop, OrderStatus.PLACED, orderItem);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse cancelled = orderService.cancelOrder(orderId, buyerId);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(variant.getStock()).isEqualTo(5);
        verify(variantRepository).save(variant);
    }

    @Test
    void cancelAfterConfirmedThrows400() {
        Long orderId = 1L;
        Long buyerId = 10L;
        Shop shop = shop(301L, 901L, "North Shop");
        Order order = order(orderId, User.builder().id(buyerId).build(), shop, OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.cancelOrder(orderId, buyerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // Helpers

    private Cart cart(User buyer, CartItem... items) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        for (CartItem cartItem : items) {
            cartItem.setCart(cart);
            cart.getItems().add(cartItem);
        }
        return cart;
    }

    private CartItem item(Long id, Variant variant, Integer quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private Order order(Long id, User buyer, Shop shop, OrderStatus status, OrderItem... items) {
        Order order = new Order();
        order.setId(id);
        order.setBuyer(buyer);
        order.setShop(shop);
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.MBWAY);
        order.setTotalAmount(BigDecimal.TEN);
        order.setCommissionAmount(BigDecimal.ONE);
        order.setCreatedAt(Instant.now());
        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }
        return order;
    }

    private OrderItem orderItem(Long id, Variant variant, Integer quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    private Variant variant(Long id, Product product, String size, String color, Integer stock) {
        return Variant.builder()
                .id(id)
                .product(product)
                .size(size)
                .color(color)
                .stock(stock)
                .build();
    }

    private Product product(Long id, Shop shop, String name, String price) {
        return Product.builder()
                .id(id)
                .shop(shop)
                .name(name)
                .description(name + " description")
                .price(new BigDecimal(price))
                .brand("Trevo")
                .materials("Cotton")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private Shop shop(Long id, Long ownerId, String name) {
        return Shop.builder()
                .id(id)
                .owner(User.builder().id(ownerId).build())
                .name(name)
                .build();
    }
}
