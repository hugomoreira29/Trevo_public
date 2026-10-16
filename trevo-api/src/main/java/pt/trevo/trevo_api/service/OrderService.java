package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CheckoutResponse;
import pt.trevo.trevo_api.dto.OrderItemResponse;
import pt.trevo.trevo_api.dto.OrderPageResponse;
import pt.trevo.trevo_api.dto.OrderResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;
import pt.trevo.trevo_api.persistence.entity.Cart;
import pt.trevo.trevo_api.persistence.entity.CartItem;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderItem;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.CartRepository;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final VariantRepository variantRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;
    private final PayoutService payoutService;
    private final PayPalService payPalService;
    private final MbWayService mbWayService;

    public OrderService(
        CartRepository cartRepository,
        OrderRepository orderRepository,
        VariantRepository variantRepository,
        ShopRepository shopRepository,
        NotificationService notificationService,
        PayoutService payoutService,
        PayPalService payPalService,
        MbWayService mbWayService
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.shopRepository = shopRepository;
        this.notificationService = notificationService;
        this.payoutService = payoutService;
        this.payPalService = payPalService;
        this.mbWayService = mbWayService;
    }

    @Transactional
    public CheckoutResponse checkout(Long buyerId, PaymentMethod paymentMethod, String phone) {
        if (buyerId == null || paymentMethod == null) {
            throw badRequest("Invalid checkout request");
        }
        if (paymentMethod == PaymentMethod.MBWAY && (phone == null || phone.isBlank())) {
            throw badRequest("Phone number is required for MB WAY payments");
        }

        Cart cart = cartRepository.findByBuyerIdWithItems(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw badRequest("Cart is empty");
        }

        // Acquire pessimistic write locks on all variants before validating stock to prevent concurrent over-selling.
        // ORDER BY v.id ensures a consistent lock acquisition order across transactions, preventing deadlocks.
        List<Long> variantIds = cart.getItems().stream()
            .map(ci -> ci.getVariant().getId())
            .sorted()
            .toList();
        Map<Long, Variant> lockedVariants = variantRepository.findByIdsForUpdate(variantIds)
            .stream()
            .collect(Collectors.toMap(Variant::getId, v -> v));

        // Validate stock for all items before creating any order
        List<Long> conflictingVariantIds = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Variant variant = lockedVariants.get(cartItem.getVariant().getId());
            if (variant.getStock() < cartItem.getQuantity()) {
                conflictingVariantIds.add(variant.getId());
            }
        }
        if (!conflictingVariantIds.isEmpty()) {
            throw badRequest("Stock conflict for variants: " + conflictingVariantIds);
        }

        // Group items by shop
        Map<Long, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem cartItem : cart.getItems()) {
            Long shopId = cartItem.getVariant().getProduct().getShop().getId();
            byShop.computeIfAbsent(shopId, id -> new ArrayList<>()).add(cartItem);
        }

        // Create one order per shop
        List<OrderResponse> orders = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            List<CartItem> shopItems = entry.getValue();
            Shop shop = shopItems.get(0).getVariant().getProduct().getShop();

            Order order = new Order();
            order.setBuyer(cart.getBuyer());
            order.setShop(shop);
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setPaymentMethod(paymentMethod);
            order.setCreatedAt(Instant.now());

            BigDecimal total = BigDecimal.ZERO;
            for (CartItem cartItem : shopItems) {
                Variant variant = lockedVariants.get(cartItem.getVariant().getId());
                BigDecimal unitPrice = cartItem.getVariant().getProduct().getPrice();

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setVariant(variant);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(unitPrice);
                order.getItems().add(orderItem);

                total = total.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));

                variant.setStock(variant.getStock() - cartItem.getQuantity());
                variantRepository.save(variant);
            }

            order.setTotalAmount(total);
            order.setCommissionAmount(total.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP));
            Order savedOrder = orderRepository.save(order);
            try {
                notificationService.createNewOrderNotification(savedOrder);
            } catch (RuntimeException ex) {
                LOGGER.warn("Failed to create notification for order {}: {}", savedOrder.getId(), ex.getMessage(), ex);
            }
            orders.add(toResponse(savedOrder));
        }

        // Clear the cart
        cart.getItems().clear();
        cartRepository.save(cart);

        String approveUrl = null;
        if (paymentMethod == PaymentMethod.PAYPAL && !orders.isEmpty()) {
            Order firstOrder = orderRepository.findById(orders.getFirst().id())
                .orElseThrow(() -> badRequest("Order not found"));
            PayPalOrderResponse payPalOrder = payPalService.createOrder(firstOrder);
            approveUrl = payPalOrder.approveUrl();
        } else if (paymentMethod == PaymentMethod.MBWAY) {
            for (OrderResponse orderResponse : orders) {
                Order order = orderRepository.findById(orderResponse.id())
                    .orElseThrow(() -> badRequest("Order not found"));
                mbWayService.createPaymentRequest(order, phone);
            }
        }

        return new CheckoutResponse(orders, approveUrl);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, Long requesterId) {
        if (orderId == null || newStatus == null || requesterId == null) {
            throw badRequest("Invalid update status request");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getShop().getOwner().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can update order status");
        }

        if (!isValidTransition(order.getStatus(), newStatus)) {
            throw badRequest("Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        if (newStatus == OrderStatus.DELIVERED) {
            payoutService.createPayout(saved);
        }

        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long requesterId) {
        if (orderId == null || requesterId == null) {
            throw badRequest("Invalid cancel request");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyer().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyer can cancel order");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw badRequest("Order can only be cancelled while in PLACED status");
        }

        for (OrderItem orderItem : order.getItems()) {
            Variant variant = orderItem.getVariant();
            variant.setStock(variant.getStock() + orderItem.getQuantity());
            variantRepository.save(variant);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getBuyerOrders(Long buyerId, Pageable pageable) {
        if (buyerId == null) {
            throw badRequest("Invalid buyer id");
        }
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findAllByBuyerId(buyerId, sorted);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse getBuyerOrder(Long orderId, Long buyerId) {
        if (orderId == null || buyerId == null) {
            throw badRequest("Invalid request");
        }
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getShopOrders(Long ownerId, Pageable pageable) {
        if (ownerId == null) {
            throw badRequest("Invalid owner id");
        }
        Shop shop = shopRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findAllByShopId(shop.getId(), sorted);
        return toPageResponse(page);
    }

    private OrderPageResponse toPageResponse(Page<Order> page) {
        List<OrderResponse> orders = page.getContent().stream().map(this::toResponse).toList();
        return new OrderPageResponse(page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize(), orders);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
            order.getId(),
            order.getShop().getId(),
            order.getShop().getName(),
            order.getStatus(),
            order.getTrackingNumber(),
            order.getTotalAmount(),
            order.getCommissionAmount(),
            order.getPaymentMethod(),
            order.getPaymentReference(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Variant variant = item.getVariant();
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
            item.getId(),
            variant.getId(),
            variant.getProduct().getId(),
            variant.getProduct().getName(),
            variant.getSize(),
            variant.getColor(),
            item.getQuantity(),
            item.getUnitPrice(),
            subtotal
        );
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PLACED -> to == OrderStatus.CONFIRMED;
            case CONFIRMED -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.RETURNED;
            default -> false;
        };
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
