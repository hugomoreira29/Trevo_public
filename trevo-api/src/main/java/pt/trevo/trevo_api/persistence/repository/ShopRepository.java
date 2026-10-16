package pt.trevo.trevo_api.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByNif(String nif);

    Optional<Shop> findByOwnerId(Long ownerId);

    Optional<Shop> findByIdAndStatus(Long id, ShopStatus status);

    Page<Shop> findAllByStatus(ShopStatus status, Pageable pageable);

    default Optional<Shop> findByIdAndActive(Long id) {
        return findByIdAndStatus(id, ShopStatus.ACTIVE);
    }

    default Page<Shop> findAllActive(Pageable pageable) {
        return findAllByStatus(ShopStatus.ACTIVE, pageable);
    }
}
