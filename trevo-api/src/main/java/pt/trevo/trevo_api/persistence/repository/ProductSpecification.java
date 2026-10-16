package pt.trevo.trevo_api.persistence.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Variant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> activeShopOnly() {
        return (root, query, cb) -> {
            Join<Product, Shop> shopJoin = root.join("shop", JoinType.INNER);
            return cb.equal(shopJoin.get("status"), ShopStatus.ACTIVE);
        };
    }

    public static Specification<Product> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            String escaped = keyword.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\')
            );
        };
    }

    public static Specification<Product> minPrice(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> maxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasVariantMatching(String size, String color) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Variant> varRoot = sub.from(Variant.class);
            sub.select(varRoot.get("product").get("id"));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(varRoot.get("product").get("id"), root.get("id")));
            if (size != null && !size.isBlank()) {
                predicates.add(cb.equal(cb.lower(varRoot.get("size")), size.toLowerCase()));
            }
            if (color != null && !color.isBlank()) {
                predicates.add(cb.equal(cb.lower(varRoot.get("color")), color.toLowerCase()));
            }
            sub.where(predicates.toArray(new Predicate[0]));
            return cb.exists(sub);
        };
    }

    public static Specification<Product> belongsToShop(Long shopId) {
        return (root, query, cb) -> cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<Product> belongsToCollection(Long collectionId) {
        return (root, query, cb) -> {
            Join<Product, Collection> colJoin = root.join("collections", JoinType.INNER);
            return cb.equal(colJoin.get("id"), collectionId);
        };
    }
}
