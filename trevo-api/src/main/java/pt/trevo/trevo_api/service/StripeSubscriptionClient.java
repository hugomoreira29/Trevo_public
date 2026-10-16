package pt.trevo.trevo_api.service;

import com.stripe.exception.StripeException;

public interface StripeSubscriptionClient {

    StripeSubscriptionResult createSubscription(String email, String priceId) throws StripeException;

    void cancelSubscription(String stripeSubscriptionId) throws StripeException;
}
