package pt.trevo.trevo_api.service;

import com.stripe.exception.AuthenticationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeSdkSubscriptionClient implements StripeSubscriptionClient {

    private final String stripeSecretKey;

    public StripeSdkSubscriptionClient(@Value("${STRIPE_SECRET_KEY:}") String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
    }

    @Override
    public StripeSubscriptionResult createSubscription(String email, String priceId) throws StripeException {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new AuthenticationException("Stripe secret key is not configured", null, null, 401);
        }

        RequestOptions options = RequestOptions.builder().setApiKey(stripeSecretKey).build();

        Customer customer = Customer.create(
            CustomerCreateParams.builder().setEmail(email).build(),
            options
        );

        Subscription subscription = Subscription.create(
            SubscriptionCreateParams.builder()
                .setCustomer(customer.getId())
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                .build(),
            options
        );

        Instant start = subscription.getStartDate() != null
            ? Instant.ofEpochSecond(subscription.getStartDate())
            : Instant.now();
        Instant end = subscription.getCancelAt() != null
            ? Instant.ofEpochSecond(subscription.getCancelAt())
            : null;

        return new StripeSubscriptionResult(subscription.getId(), subscription.getStatus(), start, end);
    }

    @Override
    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new AuthenticationException("Stripe secret key is not configured", null, null, 401);
        }

        RequestOptions options = RequestOptions.builder().setApiKey(stripeSecretKey).build();
        Subscription subscription = Subscription.retrieve(stripeSubscriptionId, options);
        subscription.cancel(SubscriptionCancelParams.builder().build(), options);
    }
}
