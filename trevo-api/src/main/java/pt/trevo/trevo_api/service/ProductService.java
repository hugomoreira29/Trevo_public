package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CreateProductRequest;
import pt.trevo.trevo_api.dto.UpdateProductRequest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.ProductSpecification;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CollectionRepository collectionRepository;
    private final ShopRepository shopRepository;

    public ProductService(
        ProductRepository productRepository,
        CollectionRepository collectionRepository,
        ShopRepository shopRepository
    ) {
        this.productRepository = productRepository;
        this.collectionRepository = collectionRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Product createProduct(Long callerId, CreateProductRequest request) {
        if (callerId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid create product request");
        }

        Shop shop = shopRepository.findByOwnerId(callerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found for this owner"));

        Product product = Product.builder()
            .shop(shop)
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .brand(request.brand())
            .materials(request.materials())
            .garmentType(request.garmentType())
            .status(ProductStatus.ACTIVE)
            .build();

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product findProductById(Long productId) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product id");
        }
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product updateProduct(Long callerId, Long productId, UpdateProductRequest request) {
        if (callerId == null || productId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid update product request");
        }

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.brand() != null) {
            product.setBrand(request.brand());
        }
        if (request.materials() != null) {
            product.setMaterials(request.materials());
        }
        if (request.garmentType() != null) {
            product.setGarmentType(request.garmentType());
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product archiveProduct(Long callerId, Long productId) {
        if (callerId == null || productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid archive product request");
        }

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);
        product.setStatus(ProductStatus.ARCHIVED);
        return productRepository.save(product);
    }

    @Transactional
    public Product unarchiveProduct(Long callerId, Long productId) {
        if (callerId == null || productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid unarchive product request");
        }

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);
        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByOwner(Long callerId) {
        Shop shop = shopRepository.findByOwnerId(callerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found for this owner"));
        return productRepository.findByShopId(shop.getId());
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsForBuyer(Long shopId) {
        if (shopId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop id");
        }

        return productRepository.findByShopIdAndStatus(shopId, ProductStatus.ACTIVE);
    }

    @Transactional
    public Product assignToCollections(Long callerId, Long productId, List<Long> collectionIds) {
        if (callerId == null || productId == null || collectionIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assign collections request");
        }

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);

        List<Collection> collections = collectionRepository.findAllById(collectionIds);
        if (collections.size() != collectionIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more collections were not found");
        }

        Long shopId = product.getShop().getId();
        boolean hasForeignCollection = collections.stream()
            .anyMatch(collection -> collection.getShop() == null || !shopId.equals(collection.getShop().getId()));
        if (hasForeignCollection) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collection does not belong to product shop");
        }

        product.setCollections(new LinkedHashSet<>(collections));
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(
        String q,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String size,
        String color,
        Pageable pageable
    ) {
        Specification<Product> spec = ProductSpecification.activeOnly()
            .and(ProductSpecification.activeShopOnly());

        if (q != null && !q.isBlank()) {
            spec = spec.and(ProductSpecification.keywordMatches(q));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.minPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.maxPrice(maxPrice));
        }
        if ((size != null && !size.isBlank()) || (color != null && !color.isBlank())) {
            spec = spec.and(ProductSpecification.hasVariantMatching(size, color));
        }

        return productRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> findActiveByShop(Long shopId, Pageable pageable) {
        if (shopId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop id");
        }
        Specification<Product> spec = ProductSpecification.activeOnly()
            .and(ProductSpecification.activeShopOnly())
            .and(ProductSpecification.belongsToShop(shopId));
        return productRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> findActiveByCollection(Long collectionId, Pageable pageable) {
        if (collectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid collection id");
        }
        Specification<Product> spec = ProductSpecification.activeOnly()
            .and(ProductSpecification.activeShopOnly())
            .and(ProductSpecification.belongsToCollection(collectionId));
        return productRepository.findAll(spec, pageable);
    }

    private Product findManagedProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void ensureOwner(Product product, Long callerId) {
        if (product.getShop() == null || product.getShop().getOwner() == null || !callerId.equals(product.getShop().getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage this product");
        }
    }
}
