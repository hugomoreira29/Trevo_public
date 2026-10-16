package pt.trevo.trevo_api.service;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;

@Service
public class StripeWebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookService.class);
    private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String ORDER_ID_METADATA_KEY = "orderId";

    private final OrderRepository orderRepository;
    private final String webhookSecret;

    public StripeWebhookService(
        OrderRepository orderRepository,
        @Value("${STRIPE_WEBHOOK_SECRET:}") String webhookSecret
    ) {
        this.orderRepository = orderRepository;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe webhook secret is not configured");
        }

        Event event = constructEvent(payload, signatureHeader);
        if (!PAYMENT_INTENT_SUCCEEDED.equals(event.getType())) {
            return;
        }

        PaymentIntent paymentIntent = paymentIntentFrom(event);
        Long orderId = orderIdFrom(paymentIntent.getMetadata());

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            LOGGER.info(
                "Skipping Stripe PaymentIntent {} for order {} because status is {}",
                paymentIntent.getId(),
                order.getId(),
                order.getStatus()
            );
            return;
        }

        order.setStatus(OrderStatus.PLACED);
        order.setPaymentReference(paymentIntent.getId());
        orderRepository.save(order);
    }

    private Event constructEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature", ex);
        }
    }

    private PaymentIntent paymentIntentFrom(Event event) {
        StripeObject stripeObject = deserializeStripeObject(event);

        if (stripeObject instanceof PaymentIntent paymentIntent) {
            return paymentIntent;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe event payload is not a payment intent");
    }

    private StripeObject deserializeStripeObject(Event event) {
        try {
            return event.getDataObjectDeserializer()
                .getObject()
                .orElseGet(() -> deserializeUnsafe(event));
        } catch (IllegalStateException | NullPointerException ex) {
            return deserializeUnsafe(event);
        }
    }

    private StripeObject deserializeUnsafe(Event event) {
        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to deserialize Stripe event payload", ex);
        }
    }

    private Long orderIdFrom(Map<String, String> metadata) {
        if (metadata == null || !metadata.containsKey(ORDER_ID_METADATA_KEY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment intent is missing orderId metadata");
        }

        try {
            return Long.valueOf(metadata.get(ORDER_ID_METADATA_KEY));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment intent has invalid orderId metadata", ex);
        }
    }
}
