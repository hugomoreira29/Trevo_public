package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CartResponse;
import pt.trevo.trevo_api.persistence.entity.Cart;
import pt.trevo.trevo_api.persistence.entity.CartItem;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.CartRepository;
import pt.trevo.trevo_api.persistence.repository.ProductImageRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

class CartServiceTest extends BaseUnitTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private VariantRepository variantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemWithValidStockAddsVariantToCart() {
        Long buyerId = 10L;
        Variant variant = variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 5);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.empty());
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        CartResponse response = cartService.addItem(buyerId, 101L, 2);

        assertThat(response.shops()).hasSize(1);
        assertThat(response.shops().getFirst().items()).hasSize(1);
        assertThat(response.shops().getFirst().items().getFirst().variantId()).isEqualTo(101L);
        assertThat(response.shops().getFirst().items().getFirst().quantity()).isEqualTo(2);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemExceedingAvailableStockThrows400() {
        Variant variant = variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 3);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                cartService.addItem(10L, 101L, 4)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemForSameVariantIncrementsQuantityInsteadOfCreatingDuplicate() {
        Long buyerId = 10L;
        Variant variant = variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 5);
        Cart cart = cart(buyerId, item(1L, variant, 2));

        when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        CartResponse response = cartService.addItem(buyerId, 101L, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(4);
        assertThat(response.shops().getFirst().items().getFirst().quantity()).isEqualTo(4);
    }

    @Test
    void getCartGroupsItemsByShop() {
        Long buyerId = 10L;
        Shop north = shop(301L, "North Shop");
        Shop south = shop(302L, "South Shop");
        Cart cart = cart(
                buyerId,
                item(1L, variant(101L, product(201L, north, "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 2), 1),
                item(2L, variant(102L, product(202L, north, "Cotton Hat", "19.50", "hat.jpg"), "One Size", "Black", 4), 2),
                item(3L, variant(103L, product(203L, south, "Wool Scarf", "12.00", "scarf.jpg"), "S", "Green", 6), 1)
        );

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(productImageRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        CartResponse response = cartService.getCart(buyerId);

        assertThat(response.shops()).hasSize(2);
        assertThat(response.shops().get(0).shopId()).isEqualTo(301L);
        assertThat(response.shops().get(0).items()).hasSize(2);
        assertThat(response.shops().get(1).shopId()).isEqualTo(302L);
        assertThat(response.shops().get(1).items()).hasSize(1);
    }

    @Test
    void getCartCalculatesShopSubtotalsAndGrandTotal() {
        Long buyerId = 10L;
        Shop north = shop(301L, "North Shop");
        Shop south = shop(302L, "South Shop");
        Cart cart = cart(
                buyerId,
                item(1L, variant(101L, product(201L, north, "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 2), 2),
                item(2L, variant(102L, product(202L, south, "Wool Scarf", "12.00", "scarf.jpg"), "S", "Green", 6), 3)
        );

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(productImageRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        CartResponse response = cartService.getCart(buyerId);

        assertThat(response.shops().get(0).subtotal()).isEqualByComparingTo("59.98");
        assertThat(response.shops().get(1).subtotal()).isEqualByComparingTo("36.00");
        assertThat(response.grandTotal()).isEqualByComparingTo("95.98");
    }

    @Test
    void getCartItemsIncludeFullVariantDetails() {
        Long buyerId = 10L;
        Product product = product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg");
        Cart cart = cart(
                buyerId,
                item(1L, variant(101L, product, "M", "Blue", 2), 1)
        );

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl("shirt.jpg");
        image.setDisplayOrder(0);

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(productImageRepository.findByProductIdIn(anyList())).thenReturn(List.of(image));

        CartResponse response = cartService.getCart(buyerId);

        assertThat(response.shops().getFirst().items().getFirst())
                .extracting("size", "color", "productName", "image")
                .containsExactly("M", "Blue", "Linen Shirt", "shirt.jpg");
    }

    @Test
    void updateQuantityValidatesNewQuantityAgainstAvailableStock() {
        Long buyerId = 10L;
        Variant variant = variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 3);
        Cart cart = cart(buyerId, item(1L, variant, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                cartService.updateQuantity(buyerId, 101L, 4)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeItemDeletesCartItem() {
        Long buyerId = 10L;
        Variant variant = variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 3);
        Cart cart = cart(buyerId, item(1L, variant, 1));

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.removeItem(buyerId, 101L);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItemRemovesLastShopGroupFromCartResponse() {
        Long buyerId = 10L;
        Cart cart = cart(
                buyerId,
                item(1L, variant(101L, product(201L, shop(301L, "North Shop"), "Linen Shirt", "29.99", "shirt.jpg"), "M", "Blue", 3), 1)
        );

        when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.removeItem(buyerId, 101L);
        CartResponse response = cartService.getCart(buyerId);

        assertThat(response.shops()).isEmpty();
        assertThat(response.grandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Cart cart(Long buyerId, CartItem... items) {
        Cart cart = new Cart();
        cart.setBuyer(User.builder().id(buyerId).build());
        for (CartItem item : items) {
            item.setCart(cart);
            cart.getItems().add(item);
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

    private Variant variant(Long id, Product product, String size, String color, Integer stock) {
        return Variant.builder()
                .id(id)
                .product(product)
                .size(size)
                .color(color)
                .stock(stock)
                .build();
    }

    private Product product(Long id, Shop shop, String name, String price, String imageUrl) {
        Product product = Product.builder()
                .id(id)
                .shop(shop)
                .name(name)
                .description(name + " description")
                .price(new BigDecimal(price))
                .brand("Trevo")
                .materials("Cotton")
                .status(ProductStatus.ACTIVE)
                .build();
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(imageUrl);
        image.setDisplayOrder(0);
        product.getImages().add(image);
        return product;
    }

    private Shop shop(Long id, String name) {
        return Shop.builder().id(id).name(name).build();
    }
}
