package pt.trevo.trevo_api.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.trevo.trevo_api.persistence.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
