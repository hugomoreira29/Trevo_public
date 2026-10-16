package pt.trevo.trevo_api.persistence.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.trevo.trevo_api.persistence.entity.ProductImage;


public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query("""
        SELECT pi FROM ProductImage pi
        JOIN FETCH pi.product p
        WHERE p.id IN :productIds
        """)
    List<ProductImage> findByProductIdIn(@Param("productIds") Collection<Long> productIds);
}
