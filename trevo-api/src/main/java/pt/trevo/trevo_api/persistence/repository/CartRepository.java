package pt.trevo.trevo_api.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.trevo.trevo_api.persistence.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {


    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.variant v " +
            "LEFT JOIN FETCH v.product p " +
            "LEFT JOIN FETCH p.shop " +
            "WHERE c.buyer.id = :buyerId")
    Optional<Cart> findByBuyerIdWithItems(@Param("buyerId") Long buyerId);
}
