package pt.trevo.trevo_api.persistence.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.persistence.entity.PayoutStatus;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByOrderId(Long orderId);

    @Query("SELECT SUM(p.payoutAmount) FROM Payout p WHERE p.order.shop.id = :shopId AND p.status = :status")
    BigDecimal sumByOrderShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") PayoutStatus status);

    @Query("SELECT p FROM Payout p WHERE p.order.shop.id = :shopId AND p.status = :status")
    Page<Payout> findByOrderShopIdAndStatus(
        @Param("shopId") Long shopId,
        @Param("status") PayoutStatus status,
        Pageable pageable
    );
}
