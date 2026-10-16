package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.AssignCollectionsRequest;
import pt.trevo.trevo_api.dto.BulkVariantStockUpdateRequest;
import pt.trevo.trevo_api.dto.CreateProductRequest;
import pt.trevo.trevo_api.dto.ProductResponse;
import pt.trevo.trevo_api.dto.UpdateProductRequest;
import pt.trevo.trevo_api.dto.UpdateStockRequest;
import pt.trevo.trevo_api.dto.UpsertVariantRequest;
import pt.trevo.trevo_api.dto.VariantResponse;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.ProductService;
import pt.trevo.trevo_api.service.VariantService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final VariantService variantService;
    private final SecurityUtils securityUtils;

    public ProductController(
        ProductService productService,
        VariantService variantService,
        SecurityUtils securityUtils
    ) {
        this.productService = productService;
        this.variantService = variantService;
        this.securityUtils = securityUtils;
    }

    // ── Owner products ───────────────────────────────────────────────────────

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public List<ProductResponse> getMyProducts(Authentication authentication) {
        Long callerId = securityUtils.requireUserId(authentication);
        List<Product> products = productService.findProductsByOwner(callerId);
        Map<Long, List<Variant>> variantsByProductId = variantService.findByProductIds(
            products.stream().map(Product::getId).toList()
        ).stream().collect(Collectors.groupingBy(v -> v.getProduct().getId()));
        return products.stream()
            .map(p -> toProductResponse(p, variantsByProductId.getOrDefault(p.getId(), List.of())))
            .toList();
    }

    // ── Search (public) ──────────────────────────────────────────────────────

    @GetMapping("/search")
    public Page<ProductResponse> searchProducts(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(name = "variantSize", required = false) String size,
        @RequestParam(required = false) String color,
        Pageable pageable
    ) {
        return toProductResponsePage(productService.searchProducts(q, minPrice, maxPrice, size, color, pageable));
    }

    // ── Browse (public) ──────────────────────────────────────────────────────

    @GetMapping
    public Page<ProductResponse> browse(
        @RequestParam(required = false) Long shopId,
        @RequestParam(required = false) Long collectionId,
        Pageable pageable
    ) {
        if (shopId != null) {
            return toProductResponsePage(productService.findActiveByShop(shopId, pageable));
        }
        if (collectionId != null) {
            return toProductResponsePage(productService.findActiveByCollection(collectionId, pageable));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shopId or collectionId is required");
    }

    // ── Single product (public) ───────────────────────────────────────────────

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        Product product = productService.findProductById(id);
        List<Variant> variants = variantService.findByProductId(id);
        return toProductResponse(product, variants);
    }

    // ── Product CRUD (SHOP_OWNER) ────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
        @Valid @RequestBody CreateProductRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Product product = productService.createProduct(callerId, request);
        return toProductResponse(product, List.of());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ProductResponse updateProduct(
        @PathVariable Long id,
        @RequestBody UpdateProductRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Product product = productService.updateProduct(callerId, id, request);
        List<Variant> variants = variantService.findByProductId(id);
        return toProductResponse(product, variants);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ProductResponse archiveProduct(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Product product = productService.archiveProduct(callerId, id);
        List<Variant> variants = variantService.findByProductId(id);
        return toProductResponse(product, variants);
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ProductResponse unarchiveProduct(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Product product = productService.unarchiveProduct(callerId, id);
        List<Variant> variants = variantService.findByProductId(id);
        return toProductResponse(product, variants);
    }

    @PutMapping("/{id}/collections")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ProductResponse assignToCollections(
        @PathVariable Long id,
        @Valid @RequestBody AssignCollectionsRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Product product = productService.assignToCollections(callerId, id, request.collectionIds());
        List<Variant> variants = variantService.findByProductId(id);
        return toProductResponse(product, variants);
    }

    // ── Variant management (SHOP_OWNER) ──────────────────────────────────────

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse addVariant(
        @PathVariable Long productId,
        @RequestBody UpsertVariantRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Variant variant = variantService.addVariant(callerId, productId, request);
        return toVariantResponse(variant);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public VariantResponse updateVariant(
        @PathVariable Long productId,
        @PathVariable Long variantId,
        @RequestBody UpsertVariantRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Variant variant = variantService.updateVariant(callerId, productId, variantId, request);
        return toVariantResponse(variant);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVariant(
        @PathVariable Long productId,
        @PathVariable Long variantId,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        variantService.removeVariant(callerId, productId, variantId);
    }

    @PatchMapping("/{productId}/variants/{variantId}/stock")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public VariantResponse updateStock(
        @PathVariable Long productId,
        @PathVariable Long variantId,
        @Valid @RequestBody UpdateStockRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Variant variant = variantService.updateStock(callerId, variantId, request.stock());
        return toVariantResponse(variant);
    }

    @PostMapping("/{productId}/variants/bulk-stock")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public List<VariantResponse> bulkUpdateStock(
        @PathVariable Long productId,
        @RequestBody List<BulkVariantStockUpdateRequest> requests,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        List<Variant> variants = variantService.bulkUpdateStock(callerId, productId, requests);
        return variants.stream().map(this::toVariantResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Page<ProductResponse> toProductResponsePage(Page<Product> products) {
        List<Long> ids = products.map(Product::getId).toList();
        Map<Long, List<Variant>> variantsByProductId = variantService.findByProductIds(ids)
            .stream()
            .collect(Collectors.groupingBy(v -> v.getProduct().getId()));
        return products.map(p -> toProductResponse(p, variantsByProductId.getOrDefault(p.getId(), List.of())));
    }

    private ProductResponse toProductResponse(Product product, List<Variant> variants) {
        List<ProductResponse.ImageResponse> images = product.getImages().stream()
            .map(img -> new ProductResponse.ImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
            .toList();
        return new ProductResponse(
            product.getId(),
            product.getShop().getId(),
            product.getShop().getName(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getBrand(),
            product.getMaterials(),
            product.getGarmentType(),
            product.getStatus(),
            variants.stream().map(this::toVariantResponse).toList(),
            images,
            product.getCreatedAt()
        );
    }

    private VariantResponse toVariantResponse(Variant v) {
        return new VariantResponse(v.getId(), v.getProduct().getId(), v.getSize(), v.getColor(), v.getStock());
    }
}
