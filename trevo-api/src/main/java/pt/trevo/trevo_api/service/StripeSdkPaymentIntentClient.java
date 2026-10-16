package pt.trevo.trevo_api.service;

import com.stripe.exception.AuthenticationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeSdkPaymentIntentClient implements StripePaymentIntentClient {

    private final String stripeSecretKey;

    public StripeSdkPaymentIntentClient(@Value("${STRIPE_SECRET_KEY:}") String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
    }

    @Override
    public StripePaymentIntentResult createPaymentIntent(
        long amountInCents,
        String currency,
        Map<String, String> metadata
    ) throws StripeException {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new AuthenticationException("Stripe secret key is not configured", null, null, 401);
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency(currency)
            .putAllMetadata(metadata)
            .build();

        PaymentIntent paymentIntent = PaymentIntent.create(
            params,
            RequestOptions.builder().setApiKey(stripeSecretKey).build()
        );

        return new StripePaymentIntentResult(
            paymentIntent.getId(),
            paymentIntent.getClientSecret(),
            paymentIntent.getAmount(),
            paymentIntent.getCurrency()
        );
    }
}
