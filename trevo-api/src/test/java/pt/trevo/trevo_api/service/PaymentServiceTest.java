package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stripe.exception.ApiConnectionException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.PaymentIntentResponse;
import pt.trevo.trevo_api.persistence.entity.Order;

@ExtendWith(OutputCaptureExtension.class)
class PaymentServiceTest extends BaseUnitTest {

    @Mock
    private StripePaymentIntentClient stripePaymentIntentClient;

    @Test
    void createPaymentIntentCreatesStripeIntentWithAmountInEurCents() throws Exception {
        PaymentService paymentService = new PaymentService(stripePaymentIntentClient);
        Order order = order(77L, "29.99");

        when(stripePaymentIntentClient.createPaymentIntent(
            eq(2999L),
            eq("eur"),
            eq(Map.of("orderId", "77"))
        )).thenReturn(new StripePaymentIntentResult(
            "pi_123",
            "pi_123_secret_abc",
            2999L,
            "eur"
        ));

        PaymentIntentResponse response = paymentService.createPaymentIntent(order);

        assertThat(response.id()).isEqualTo("pi_123");
        assertThat(response.clientSecret()).isEqualTo("pi_123_secret_abc");
        assertThat(response.amount()).isEqualTo(2999L);
        assertThat(response.currency()).isEqualTo("eur");
        verify(stripePaymentIntentClient).createPaymentIntent(
            2999L,
            "eur",
            Map.of("orderId", "77")
        );
    }

    @Test
    void createPaymentIntentRoundsAmountToNearestCent() throws Exception {
        PaymentService paymentService = new PaymentService(stripePaymentIntentClient);
        Order order = order(77L, "10.235");

        when(stripePaymentIntentClient.createPaymentIntent(
            eq(1024L),
            eq("eur"),
            eq(Map.of("orderId", "77"))
        )).thenReturn(new StripePaymentIntentResult("pi_123", "secret", 1024L, "eur"));

        paymentService.createPaymentIntent(order);

        verify(stripePaymentIntentClient).createPaymentIntent(1024L, "eur", Map.of("orderId", "77"));
    }

    @Test
    void createPaymentIntentWrapsStripeFailures(CapturedOutput output) throws Exception {
        PaymentService paymentService = new PaymentService(stripePaymentIntentClient);
        Order order = order(77L, "29.99");

        when(stripePaymentIntentClient.createPaymentIntent(
            eq(2999L),
            eq("eur"),
            eq(Map.of("orderId", "77"))
        )).thenThrow(new ApiConnectionException("stripe unavailable"));

        Throwable thrown = catchThrowable(() -> paymentService.createPaymentIntent(order));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("Failed to create payment intent");
        assertThat(output).contains("Failed to create Stripe PaymentIntent for order 77");
    }

    @Test
    void createPaymentIntentRejectsInvalidAmount() {
        PaymentService paymentService = new PaymentService(stripePaymentIntentClient);
        Order order = order(77L, "0.00");

        Throwable thrown = catchThrowable(() -> paymentService.createPaymentIntent(order));

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
}
