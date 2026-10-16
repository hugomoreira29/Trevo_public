package pt.trevo.trevo_api.service;

import com.stripe.exception.StripeException;
import java.util.Map;

public interface StripePaymentIntentClient {

    StripePaymentIntentResult createPaymentIntent(
        long amountInCents,
        String currency,
        Map<String, String> metadata
    ) throws StripeException;
}
