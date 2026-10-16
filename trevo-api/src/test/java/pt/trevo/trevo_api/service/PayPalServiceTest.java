package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.PayPalCaptureResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;

class PayPalServiceTest extends BaseUnitTest {

    @Mock
    private PayPalOrdersClient payPalOrdersClient;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void createOrderCreatesPayPalOrderAndStoresReference() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());
        Order order = order(77L, "29.995");

        when(payPalOrdersClient.createOrder(
            eq(77L),
            eq(new BigDecimal("30.00")),
            eq("EUR"),
            eq("trevo-order-create-77")
        )).thenReturn(new PayPalOrderResponse(
            "PAYPAL-ORDER-123",
            "CREATED",
            "https://paypal.example/checkoutnow?token=PAYPAL-ORDER-123"
        ));
        when(orderRepository.save(order)).thenReturn(order);

        PayPalOrderResponse response = payPalService.createOrder(order);

        assertThat(response.id()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.approveUrl()).isEqualTo("https://paypal.example/checkoutnow?token=PAYPAL-ORDER-123");
        assertThat(order.getPaymentReference()).isEqualTo("PAYPAL-ORDER-123");
        verify(orderRepository).save(order);
    }

    @Test
    void capturePaymentCompletesFlowAndMarksOrderPlaced() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());
        Order order = order(77L, "29.99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentReference("PAYPAL-ORDER-123");

        when(payPalOrdersClient.captureOrder("PAYPAL-ORDER-123", "trevo-order-capture-77"))
            .thenReturn(new PayPalCaptureResponse("PAYPAL-ORDER-123", "COMPLETED"));
        when(orderRepository.findByPaymentReference("PAYPAL-ORDER-123")).thenReturn(Optional.of(order), Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PayPalCaptureResponse response = payPalService.capturePayment("PAYPAL-ORDER-123");

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(orderRepository).save(order);
    }

    @Test
    void capturePaymentDoesNotMutateCancelledOrder() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());
        Order order = order(77L, "29.99");
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentReference("PAYPAL-ORDER-123");

        when(orderRepository.findByPaymentReference("PAYPAL-ORDER-123")).thenReturn(Optional.of(order));

        PayPalCaptureResponse response = payPalService.capturePayment("PAYPAL-ORDER-123");

        assertThat(response.id()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(response.status()).isEqualTo("SKIPPED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(payPalOrdersClient, never()).captureOrder(
            eq("PAYPAL-ORDER-123"),
            eq("trevo-order-capture-77")
        );
        verify(orderRepository, never()).save(order);
    }

    @Test
    void failedCaptureThrows() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());
        Order order = order(77L, "29.99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentReference("PAYPAL-ORDER-123");

        when(orderRepository.findByPaymentReference("PAYPAL-ORDER-123")).thenReturn(Optional.of(order));
        when(payPalOrdersClient.captureOrder("PAYPAL-ORDER-123", "trevo-order-capture-77"))
            .thenReturn(new PayPalCaptureResponse("PAYPAL-ORDER-123", "DECLINED"));

        Throwable thrown = catchThrowable(() -> payPalService.capturePayment("PAYPAL-ORDER-123"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("PayPal capture failed");
    }

    @Test
    void capturePaymentThrowsWhenLocalOrderIsNotFound() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());

        when(orderRepository.findByPaymentReference("PAYPAL-ORDER-123")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> payPalService.capturePayment("PAYPAL-ORDER-123"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Order not found");
        verify(payPalOrdersClient, never()).captureOrder(
            eq("PAYPAL-ORDER-123"),
            eq("trevo-order-capture-77")
        );
    }

    @Test
    void createOrderRejectsInvalidAmount() {
        PayPalService payPalService = new PayPalService(payPalOrdersClient, orderRepository, testTransactions());

        Throwable thrown = catchThrowable(() -> payPalService.createOrder(order(77L, "0.00")));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Payment amount must be positive");
    }

    private Order order(Long id, String totalAmount) {
        Order order = new Order();
        order.setId(id);
        order.setTotalAmount(new BigDecimal(totalAmount));
        return order;
    }

    private TransactionOperations testTransactions() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }
}
