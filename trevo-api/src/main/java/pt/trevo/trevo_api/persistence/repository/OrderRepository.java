package pt.trevo.trevo_api.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByBuyerIdAndShopIdAndStatus(Long buyerId, Long shopId, OrderStatus status);

    List<Order> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);

    List<Order> findAllByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<Order> findAllByShopId(Long shopId, Pageable pageable);

    List<Order> findAllByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, OrderStatus status);

    Optional<Order> findByPaymentReference(String paymentReference);
}
