package pt.trevo.trevo_api.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.trevo.trevo_api.persistence.entity.TryOnUsage;

public interface TryOnUsageRepository extends JpaRepository<TryOnUsage, Long> {

    Optional<TryOnUsage> findByBuyerIdAndProductId(Long buyerId, Long productId);

    @Query("SELECT COALESCE(SUM(u.generationCount), 0) FROM TryOnUsage u WHERE u.buyer.id = :buyerId")
    int countTotalGenerations(@Param("buyerId") Long buyerId);
}
