package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stripe.exception.ApiConnectionException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.SubscriptionResponse;
import pt.trevo.trevo_api.persistence.entity.Subscription;
import pt.trevo.trevo_api.persistence.entity.SubscriptionStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.SubscriptionRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@ExtendWith(OutputCaptureExtension.class)
class SubscriptionServiceTest extends BaseUnitTest {

    @Mock private StripeSubscriptionClient stripeSubscriptionClient;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;

    private static final String PRICE_ID = "price_test_123";

    private SubscriptionService service() {
        return new SubscriptionService(stripeSubscriptionClient, subscriptionRepository, userRepository, PRICE_ID);
    }

    @Test
    void createSubscriptionSavesActiveSubscription() throws Exception {
        User buyer = user(1L, "buyer@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        Instant start = Instant.parse("2026-05-14T00:00:00Z");
        Instant end = Instant.parse("2026-06-14T00:00:00Z");
        when(stripeSubscriptionClient.createSubscription("buyer@example.com", PRICE_ID))
            .thenReturn(new StripeSubscriptionResult("sub_abc", "active", start, end));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse result = service().createSubscription(1L);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.stripeSubscriptionId()).isEqualTo("sub_abc");
        assertThat(result.startDate()).isEqualTo(start);
        assertThat(result.endDate()).isEqualTo(end);
        verify(stripeSubscriptionClient).createSubscription("buyer@example.com", PRICE_ID);
    }

    @Test
    void createSubscriptionRejectsAlreadyActiveSubscription() {
        User buyer = user(1L, "buyer@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(activeSubscription()));

        Throwable thrown = catchThrowable(() -> service().createSubscription(1L));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createSubscriptionWrapsStripeFailure(CapturedOutput output) throws Exception {
        User buyer = user(1L, "buyer@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(stripeSubscriptionClient.createSubscription(any(), any()))
            .thenThrow(new ApiConnectionException("stripe down"));

        Throwable thrown = catchThrowable(() -> service().createSubscription(1L));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(output).contains("Failed to create Stripe subscription for user 1");
    }

    @Test
    void cancelSubscriptionCancelsOnStripeAndUpdatesStatus() throws Exception {
        Subscription sub = activeSubscription();
        sub.setStripeSubscriptionId("sub_abc");
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().cancelSubscription(1L);

        verify(stripeSubscriptionClient).cancelSubscription("sub_abc");
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void cancelSubscriptionThrowsWhenNoActiveSubscription() {
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service().cancelSubscription(1L));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void isProUserReturnsTrueForActiveSubscription() {
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(activeSubscription()));

        assertThat(service().isProUser(1L)).isTrue();
    }

    @Test
    void isProUserReturnsFalseWhenEndDateHasPassed() {
        Subscription sub = new Subscription();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setEndDate(Instant.now().minusSeconds(3600));
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(sub));

        assertThat(service().isProUser(1L)).isFalse();
    }

    @Test
    void isProUserReturnsFalseForExpiredSubscription() {
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(subscription(SubscriptionStatus.EXPIRED)));

        assertThat(service().isProUser(1L)).isFalse();
    }

    @Test
    void isProUserReturnsFalseForCancelledSubscription() {
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(subscription(SubscriptionStatus.CANCELLED)));

        assertThat(service().isProUser(1L)).isFalse();
    }

    @Test
    void isProUserReturnsFalseWhenNoSubscription() {
        when(subscriptionRepository.findFirstByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        assertThat(service().isProUser(1L)).isFalse();
    }

    private User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Subscription activeSubscription() {
        return subscription(SubscriptionStatus.ACTIVE);
    }

    private Subscription subscription(SubscriptionStatus status) {
        Subscription s = new Subscription();
        s.setStatus(status);
        s.setEndDate(Instant.now().plusSeconds(3600));
        return s;
    }
}
