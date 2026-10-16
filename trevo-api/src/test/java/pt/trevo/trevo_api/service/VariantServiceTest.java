package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.BulkVariantStockUpdateRequest;
import pt.trevo.trevo_api.dto.UpsertVariantRequest;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

class VariantServiceTest extends BaseUnitTest {

    @Mock
    private VariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private VariantService variantService;

    @Test
    void stockUpdateRejectsValuesBelowZero() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            variantService.updateStock(11L, 5L, -1)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(variantRepository, never()).save(any(Variant.class));
    }

    @Test
    void duplicateProductSizeColorThrows409() {
        Long ownerId = 11L;
        Long productId = 7L;
        UpsertVariantRequest request = new UpsertVariantRequest("M", "Black", 5);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct(productId, ownerId)));
        when(variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(productId, "M", "Black")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            variantService.addVariant(ownerId, productId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(variantRepository, never()).save(any(Variant.class));
    }

    @Test
    void updateVariantWithNullSizeAndConflictingColorThrows409() {
        Long ownerId = 11L;
        Long productId = 7L;
        Long variantId = 101L;
        Variant variant = existingVariant(variantId, productId, ownerId, "S", "Black", 4);
        UpsertVariantRequest request = new UpsertVariantRequest(null, "Red", 6);

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByProductIdAndIdNotAndSizeIgnoreCaseAndColorIgnoreCase(productId, variantId, "S", "Red")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            variantService.updateVariant(ownerId, productId, variantId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(variantRepository, never()).save(any(Variant.class));
    }

    @Test
    void addVariantWithNullSizeOrColorThrows400() {
        Long ownerId = 11L;
        Long productId = 7L;
        UpsertVariantRequest request = new UpsertVariantRequest(null, null, 0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            variantService.addVariant(ownerId, productId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(productRepository, never()).findById(any());
        verify(variantRepository, never()).save(any(Variant.class));
    }

    @Test
    void bulkStockUpdateModifiesMultipleVariantsInASingleCall() {
        Long ownerId = 11L;
        Long productId = 7L;
        Variant first = existingVariant(101L, productId, ownerId, "S", "Black", 4);
        Variant second = existingVariant(102L, productId, ownerId, "M", "White", 6);
        List<BulkVariantStockUpdateRequest> request = List.of(
            new BulkVariantStockUpdateRequest(101L, 8),
            new BulkVariantStockUpdateRequest(102L, 2)
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct(productId, ownerId)));
        when(variantRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(first, second));
        when(variantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Variant> updated = variantService.bulkUpdateStock(ownerId, productId, request);

        assertThat(updated).extracting(Variant::getStock).containsExactly(8, 2);
        verify(variantRepository).saveAll(List.of(first, second));
    }

    @Test
    void bulkStockUpdateWithDuplicateVariantIdsThrows400() {
        Long ownerId = 11L;
        Long productId = 7L;
        List<BulkVariantStockUpdateRequest> request = List.of(
            new BulkVariantStockUpdateRequest(101L, 8),
            new BulkVariantStockUpdateRequest(101L, 2)
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct(productId, ownerId)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            variantService.bulkUpdateStock(ownerId, productId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(variantRepository, never()).findAllById(any());
        verify(variantRepository, never()).saveAll(any());
    }

    private Product existingProduct(Long productId, Long ownerId) {
        return Product.builder()
            .id(productId)
            .shop(Shop.builder().id(3L).owner(User.builder().id(ownerId).build()).build())
            .name("Original name")
            .description("Original description")
            .price(new BigDecimal("39.99"))
            .brand("Original brand")
            .materials("Cotton")
            .status(ProductStatus.ACTIVE)
            .build();
    }

    private Variant existingVariant(Long variantId, Long productId, Long ownerId, String size, String color, Integer stock) {
        return Variant.builder()
            .id(variantId)
            .product(existingProduct(productId, ownerId))
            .size(size)
            .color(color)
            .stock(stock)
            .build();
    }
}
