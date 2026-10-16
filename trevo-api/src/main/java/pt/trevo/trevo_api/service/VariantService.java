package pt.trevo.trevo_api.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.BulkVariantStockUpdateRequest;
import pt.trevo.trevo_api.dto.UpsertVariantRequest;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

@Service
public class VariantService {

    private final VariantRepository variantRepository;
    private final ProductRepository productRepository;

    public VariantService(VariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Variant addVariant(Long callerId, Long productId, UpsertVariantRequest request) {
        if (callerId == null || productId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid add variant request");
        }
        validateAttributes(request.size(), request.color());
        validateStock(request.stock());

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);
        ensureUniqueVariant(productId, request.size(), request.color(), null);

        Variant variant = Variant.builder()
            .product(product)
            .size(request.size())
            .color(request.color())
            .stock(request.stock())
            .build();

        return variantRepository.save(variant);
    }

    @Transactional
    public Variant updateVariant(Long callerId, Long productId, Long variantId, UpsertVariantRequest request) {
        if (callerId == null || productId == null || variantId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid update variant request");
        }
        validateStock(request.stock());

        Variant variant = findManagedVariant(variantId);
        ensureOwner(variant.getProduct(), callerId);
        ensureVariantBelongsToProduct(variant, productId);
        String effectiveSize = request.size() != null ? request.size() : variant.getSize();
        String effectiveColor = request.color() != null ? request.color() : variant.getColor();
        validateAttributes(effectiveSize, effectiveColor);
        ensureUniqueVariant(variant.getProduct().getId(), effectiveSize, effectiveColor, variantId);

        if (request.size() != null) {
            variant.setSize(request.size());
        }
        if (request.color() != null) {
            variant.setColor(request.color());
        }
        variant.setStock(request.stock());

        return variantRepository.save(variant);
    }

    @Transactional
    public void removeVariant(Long callerId, Long productId, Long variantId) {
        if (callerId == null || productId == null || variantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid remove variant request");
        }

        Variant variant = findManagedVariant(variantId);
        ensureOwner(variant.getProduct(), callerId);
        ensureVariantBelongsToProduct(variant, productId);
        variantRepository.delete(variant);
    }

    @Transactional
    public Variant updateStock(Long callerId, Long variantId, Integer stock) {
        if (callerId == null || variantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stock update request");
        }
        validateStock(stock);

        Variant variant = findManagedVariant(variantId);
        ensureOwner(variant.getProduct(), callerId);
        variant.setStock(stock);
        return variantRepository.save(variant);
    }

    @Transactional
    public List<Variant> bulkUpdateStock(Long callerId, Long productId, List<BulkVariantStockUpdateRequest> requests) {
        if (callerId == null || productId == null || requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bulk stock update request");
        }

        Product product = findManagedProduct(productId);
        ensureOwner(product, callerId);
        requests.forEach(request -> validateStock(request.stock()));
        long distinctIds = requests.stream()
            .map(BulkVariantStockUpdateRequest::variantId)
            .distinct()
            .count();
        if (distinctIds != requests.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate variantId in bulk request");
        }

        List<Long> variantIds = requests.stream()
            .map(BulkVariantStockUpdateRequest::variantId)
            .toList();
        List<Variant> variants = variantRepository.findAllById(variantIds);

        if (variants.size() != requests.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more variants were not found");
        }

        Map<Long, Variant> variantsById = variants.stream()
            .collect(Collectors.toMap(Variant::getId, Function.identity()));

        for (BulkVariantStockUpdateRequest request : requests) {
            Variant variant = variantsById.get(request.variantId());
            if (variant == null || !productId.equals(variant.getProduct().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant does not belong to product");
            }
            variant.setStock(request.stock());
        }

        return variantRepository.saveAll(variants);
    }

    @Transactional(readOnly = true)
    public List<Variant> findByProductId(Long productId) {
        return variantRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<Variant> findByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return variantRepository.findByProductIdIn(productIds);
    }

    private Product findManagedProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Variant findManagedVariant(Long variantId) {
        return variantRepository.findById(variantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found"));
    }

    private void ensureOwner(Product product, Long callerId) {
        if (product.getShop() == null || product.getShop().getOwner() == null || !callerId.equals(product.getShop().getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage variants");
        }
    }

    private void ensureVariantBelongsToProduct(Variant variant, Long productId) {
        if (!productId.equals(variant.getProduct().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found for this product");
        }
    }

    private void ensureUniqueVariant(Long productId, String size, String color, Long variantId) {
        boolean exists = variantId == null
            ? variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(productId, size, color)
            : variantRepository.existsByProductIdAndIdNotAndSizeIgnoreCaseAndColorIgnoreCase(productId, variantId, size, color);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Variant already exists for this product");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be less than zero");
        }
    }

    private void validateAttributes(String size, String color) {
        if (size == null || size.isBlank() || color == null || color.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size and color are required");
        }
    }
}
