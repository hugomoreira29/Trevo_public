package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.ReceiptData;
import pt.trevo.trevo_api.dto.ReceiptLineItem;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderItem;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;

class ReceiptServiceTest extends BaseUnitTest {

    private final ReceiptService receiptService = new ReceiptService();

    // buildReceipt

    @Test
    void buildReceiptContainsOrderId() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        assertThat(receipt.orderId()).isEqualTo(1L);
    }

    @Test
    void buildReceiptContainsBuyerInfo() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        assertThat(receipt.buyerName()).isEqualTo("Ana Costa");
        assertThat(receipt.buyerEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void buildReceiptIssuedAtMatchesOrderCreatedAt() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        assertThat(receipt.issuedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void buildReceiptContainsAllItems() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        assertThat(receipt.items()).hasSize(2);
    }

    @Test
    void buildReceiptItemHasProductNameSizeAndColor() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);
        ReceiptLineItem first = receipt.items().get(0);

        assertThat(first.productName()).isEqualTo("Linen Shirt");
        assertThat(first.size()).isEqualTo("M");
        assertThat(first.color()).isEqualTo("Blue");
    }

    @Test
    void buildReceiptItemLineTotalIsQuantityTimesUnitPrice() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        for (ReceiptLineItem item : receipt.items()) {
            BigDecimal expected = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            assertThat(item.lineTotal()).isEqualByComparingTo(expected);
        }
    }

    @Test
    void buildReceiptTotalMatchesOrderTotalAmount() {
        Order order = buildOrder();

        ReceiptData receipt = receiptService.buildReceipt(order);

        assertThat(receipt.total()).isEqualByComparingTo(order.getTotalAmount());
    }

    // generatePdf

    @Test
    void generatePdfReturnsByteArrayWithoutThrowing() {
        ReceiptData receipt = receiptService.buildReceipt(buildOrder());

        byte[] pdf = assertDoesNotThrow(() -> receiptService.generatePdf(receipt));

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void generatedPdfStartsWithPdfSignature() {
        ReceiptData receipt = receiptService.buildReceipt(buildOrder());

        byte[] pdf = receiptService.generatePdf(receipt);

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    // Helpers

    private Order buildOrder() {
        User buyer = User.builder()
            .id(10L)
            .fullName("Ana Costa")
            .email("ana@example.com")
            .passwordHash("hash")
            .build();

        Shop shop = Shop.builder()
            .id(301L)
            .owner(User.builder().id(901L).build())
            .name("North Shop")
            .build();

        Product shirt = Product.builder()
            .id(201L).shop(shop).name("Linen Shirt")
            .description("desc").price(new BigDecimal("29.99"))
            .brand("Trevo").materials("Cotton").status(ProductStatus.ACTIVE)
            .build();

        Product scarf = Product.builder()
            .id(202L).shop(shop).name("Wool Scarf")
            .description("desc").price(new BigDecimal("12.00"))
            .brand("Trevo").materials("Wool").status(ProductStatus.ACTIVE)
            .build();

        Variant v1 = Variant.builder().id(101L).product(shirt).size("M").color("Blue").stock(5).build();
        Variant v2 = Variant.builder().id(102L).product(scarf).size("S").color("Green").stock(3).build();

        Order order = new Order();
        order.setId(1L);
        order.setBuyer(buyer);
        order.setShop(shop);
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.MBWAY);
        order.setTotalAmount(new BigDecimal("53.98"));
        order.setCommissionAmount(new BigDecimal("5.40"));
        order.setCreatedAt(Instant.parse("2026-05-14T10:00:00Z"));
        order.setItems(new ArrayList<>());

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setVariant(v1);
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("29.99"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setVariant(v2);
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("12.00"));

        order.getItems().add(item1);
        order.getItems().add(item2);

        return order;
    }
}
