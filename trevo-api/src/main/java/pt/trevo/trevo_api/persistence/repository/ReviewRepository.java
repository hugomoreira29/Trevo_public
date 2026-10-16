package pt.trevo.trevo_api.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.trevo.trevo_api.persistence.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBuyerIdAndShopId(Long buyerId, Long shopId);

    List<Review> findAllByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<Review> findAllByShopId(Long shopId, Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.shop.id = :shopId")
    Double findAverageRatingByShopId(Long shopId);
}
