package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;

class ProductServiceAssignCollectionsTest extends BaseUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void productAppearsInAssignedCollections() {
        Long ownerId = 11L;
        Long productId = 7L;
        Long shopId = 3L;
        Collection summer = Collection.builder().id(101L).shop(Shop.builder().id(shopId).build()).name("Summer").build();
        Collection basics = Collection.builder().id(102L).shop(Shop.builder().id(shopId).build()).name("Basics").build();
        Product product = existingProduct(productId, ownerId, shopId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(collectionRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(summer, basics));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.assignToCollections(ownerId, productId, List.of(101L, 102L));

        assertThat(updated.getCollections()).extracting(Collection::getId).containsExactlyInAnyOrder(101L, 102L);
        verify(productRepository).save(product);
    }

    @Test
    void removingAProductFromACollectionDoesNotDeleteTheProduct() {
        Long ownerId = 11L;
        Long productId = 7L;
        Long shopId = 3L;
        Collection summer = Collection.builder().id(101L).shop(Shop.builder().id(shopId).build()).name("Summer").build();
        Collection basics = Collection.builder().id(102L).shop(Shop.builder().id(shopId).build()).name("Basics").build();
        Product product = existingProduct(productId, ownerId, shopId);
        product.setCollections(new LinkedHashSet<>(Set.of(summer, basics)));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(collectionRepository.findAllById(List.of(101L))).thenReturn(List.of(summer));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.assignToCollections(ownerId, productId, List.of(101L));

        assertThat(updated.getId()).isEqualTo(productId);
        assertThat(updated.getCollections()).extracting(Collection::getId).containsExactly(101L);
        verify(productRepository, never()).delete(any(Product.class));
    }

    private Product existingProduct(Long productId, Long ownerId, Long shopId) {
        return Product.builder()
            .id(productId)
            .shop(Shop.builder().id(shopId).owner(User.builder().id(ownerId).build()).build())
            .name("Original name")
            .description("Original description")
            .price(new BigDecimal("39.99"))
            .brand("Original brand")
            .materials("Cotton")
            .status(ProductStatus.ACTIVE)
            .collections(new LinkedHashSet<>())
            .build();
    }
}
