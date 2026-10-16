package pt.trevo.trevo_api.service;

import com.stripe.exception.StripeException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.SubscriptionResponse;
import pt.trevo.trevo_api.persistence.entity.Subscription;
import pt.trevo.trevo_api.persistence.entity.SubscriptionStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.SubscriptionRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final StripeSubscriptionClient stripeSubscriptionClient;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final String stripePriceId;

    public SubscriptionService(
        StripeSubscriptionClient stripeSubscriptionClient,
        SubscriptionRepository subscriptionRepository,
        UserRepository userRepository,
        @Value("${STRIPE_PRO_PRICE_ID:}") String stripePriceId
    ) {
        this.stripeSubscriptionClient = stripeSubscriptionClient;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.stripePriceId = stripePriceId;
    }

    @Transactional
    public SubscriptionResponse createSubscription(Long buyerId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(buyerId)
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .ifPresent(s -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has an active subscription"); });

        StripeSubscriptionResult result;
        try {
            result = stripeSubscriptionClient.createSubscription(buyer.getEmail(), stripePriceId);
        } catch (StripeException e) {
            log.error("Failed to create Stripe subscription for user {}", buyerId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to create subscription");
        }

        Subscription subscription = new Subscription();
        subscription.setBuyer(buyer);
        subscription.setStripeSubscriptionId(result.id());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(result.currentPeriodStart());
        subscription.setEndDate(result.currentPeriodEnd());
        subscription.setCreatedAt(Instant.now());

        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void cancelSubscription(Long buyerId) {
        Subscription subscription = subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(buyerId)
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active subscription found"));

        try {
            stripeSubscriptionClient.cancelSubscription(subscription.getStripeSubscriptionId());
        } catch (StripeException e) {
            log.error("Failed to cancel Stripe subscription for user {}", buyerId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to cancel subscription");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }

    public boolean isProUser(Long buyerId) {
        return subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(buyerId)
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(s -> s.getEndDate() == null || s.getEndDate().isAfter(Instant.now()))
            .isPresent();
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
            s.getId(),
            s.getStatus(),
            s.getStripeSubscriptionId(),
            s.getStartDate(),
            s.getEndDate(),
            s.getCreatedAt()
        );
    }
}
