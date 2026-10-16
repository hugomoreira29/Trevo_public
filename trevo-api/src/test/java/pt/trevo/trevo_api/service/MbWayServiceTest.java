package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.MbWayCallbackRequest;
import pt.trevo.trevo_api.dto.MbWayPaymentRequestResponse;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;

class MbWayServiceTest extends BaseUnitTest {

    @Mock
    private MbWayPaymentsClient mbWayPaymentsClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MbWayCallbackVerifier callbackVerifier;

    @Test
    void createPaymentRequestValidatesPhoneAndCreatesRequest() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.995");

        when(mbWayPaymentsClient.createPaymentRequest(
            eq(77L),
            eq(new BigDecimal("20.00")),
            eq("EUR"),
            eq("+351923456789")
        )).thenReturn(new MbWayPaymentRequestResponse("MBWAY-REF-123", "PENDING"));
        when(orderRepository.save(order)).thenReturn(order);

        MbWayPaymentRequestResponse response = mbWayService.createPaymentRequest(order, "923 456 789");

        assertThat(response.reference()).isEqualTo("MBWAY-REF-123");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(order.getPaymentReference()).isEqualTo("MBWAY-REF-123");
        verify(orderRepository).save(order);
    }

    @Test
    void createPaymentRequestRejectsInvalidPhoneNumber() {
        MbWayService mbWayService = service();

        Throwable thrown = catchThrowable(() -> mbWayService.createPaymentRequest(order(77L, "19.99"), "812345678"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Invalid MB WAY phone number");
        verify(mbWayPaymentsClient, never()).createPaymentRequest(any(), any(), any(), any());
    }

    @Test
    void createPaymentRequestRejectsNullOrder() {
        MbWayService mbWayService = service();

        Throwable thrown = catchThrowable(() -> mbWayService.createPaymentRequest(null, "923456789"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Invalid MB WAY payment request");
        verify(mbWayPaymentsClient, never()).createPaymentRequest(any(), any(), any(), any());
    }

    @Test
    void createPaymentRequestRejectsBlankAmount() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.99");
        order.setTotalAmount(null);

        Throwable thrown = catchThrowable(() -> mbWayService.createPaymentRequest(order, "923456789"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Invalid MB WAY payment request");
        verify(mbWayPaymentsClient, never()).createPaymentRequest(any(), any(), any(), any());
    }

    @Test
    void callbackUpdatesOrderToPlacedWhenPaymentSucceeds() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentReference("MBWAY-REF-123");

        when(orderRepository.findByPaymentReference("MBWAY-REF-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", "PAID"),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=valid"
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(callbackVerifier).verify(
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=valid"
        );
        verify(orderRepository).save(order);
    }

    @Test
    void callbackDoesNotMutateCancelledOrder() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.99");
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentReference("MBWAY-REF-123");

        when(orderRepository.findByPaymentReference("MBWAY-REF-123")).thenReturn(Optional.of(order));

        mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", "PAID"),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=valid"
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void callbackIgnoresPendingPaymentWithoutUpdatingOrder() {
        MbWayService mbWayService = service();

        mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", "PENDING"),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PENDING\"}",
            "sha256=valid"
        );

        verify(callbackVerifier).verify(
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PENDING\"}",
            "sha256=valid"
        );
        verify(orderRepository, never()).findByPaymentReference("MBWAY-REF-123");
        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    void callbackThrowsWhenReferenceIsUnknown() {
        MbWayService mbWayService = service();

        when(orderRepository.findByPaymentReference("MBWAY-REF-123")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", "PAID"),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=valid"
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Order not found");
    }

    @Test
    void callbackAcceptsCompletedStatusVariant() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentReference("MBWAY-REF-123");

        when(orderRepository.findByPaymentReference("MBWAY-REF-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", " Completed "),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\" Completed \"}",
            "sha256=valid"
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void callbackAcceptsSuccessStatusVariant() {
        MbWayService mbWayService = service();
        Order order = order(77L, "19.99");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentReference("MBWAY-REF-123");

        when(orderRepository.findByPaymentReference("MBWAY-REF-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        mbWayService.handleCallback(
            new MbWayCallbackRequest("MBWAY-REF-123", "success"),
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"success\"}",
            "sha256=valid"
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    private Order order(Long id, String totalAmount) {
        Order order = new Order();
        order.setId(id);
        order.setTotalAmount(new BigDecimal(totalAmount));
        return order;
    }

    private MbWayService service() {
        return new MbWayService(
            mbWayPaymentsClient,
            orderRepository,
            callbackVerifier,
            TransactionOperations.withoutTransaction()
        );
    }
}
