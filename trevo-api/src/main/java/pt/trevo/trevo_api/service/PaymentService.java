package pt.trevo.trevo_api.service;

import com.stripe.exception.StripeException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.PaymentIntentResponse;
import pt.trevo.trevo_api.persistence.entity.Order;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    private static final String EUR_CURRENCY = "eur";

    private final StripePaymentIntentClient stripePaymentIntentClient;

    public PaymentService(StripePaymentIntentClient stripePaymentIntentClient) {
        this.stripePaymentIntentClient = stripePaymentIntentClient;
    }

    public PaymentIntentResponse createPaymentIntent(Order order) {
        if (order == null || order.getId() == null || order.getTotalAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment intent request");
        }

        long amountInCents = toEurCents(order.getTotalAmount());

        try {
            StripePaymentIntentResult paymentIntent = stripePaymentIntentClient.createPaymentIntent(
                amountInCents,
                EUR_CURRENCY,
                Map.of("orderId", order.getId().toString())
            );

            return new PaymentIntentResponse(
                paymentIntent.id(),
                paymentIntent.clientSecret(),
                paymentIntent.amount(),
                paymentIntent.currency()
            );
        } catch (StripeException ex) {
            LOGGER.error("Failed to create Stripe PaymentIntent for order {}", order.getId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to create payment intent", ex);
        }
    }

    private long toEurCents(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
        }

        return amount
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }
}
