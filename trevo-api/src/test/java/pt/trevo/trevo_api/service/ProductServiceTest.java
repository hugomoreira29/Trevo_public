package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CreateProductRequest;
import pt.trevo.trevo_api.dto.UpdateProductRequest;
import pt.trevo.trevo_api.persistence.entity.GarmentType;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

class ProductServiceTest extends BaseUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductStartsWithEmptyImageList() {
        Long ownerId = 11L;
        Shop shop = Shop.builder().id(3L).owner(User.builder().id(ownerId).build()).build();
        CreateProductRequest request = new CreateProductRequest(
            "Linen Shirt",
            "Lightweight shirt",
            new BigDecimal("39.99"),
            "Trevo",
            "Linen",
            GarmentType.UPPER_BODY
        );

        when(shopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.createProduct(ownerId, request);

        assertThat(created.getShop()).isSameAs(shop);
        assertThat(created.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(created.getImages()).isEmpty();
        verify(productRepository).save(created);
    }

    @Test
    void updateModifiesTheCorrectFields() {
        Long ownerId = 11L;
        Long productId = 7L;
        Product product = existingProduct(productId, ownerId);
        UpdateProductRequest request = new UpdateProductRequest(
            "Updated name",
            "Updated description",
            new BigDecimal("59.99"),
            "Updated brand",
            null,
            null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateProduct(ownerId, productId, request);

        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getPrice()).isEqualTo(new BigDecimal("59.99"));
        assertThat(updated.getBrand()).isEqualTo("Updated brand");
        assertThat(updated.getMaterials()).isEqualTo("Cotton");
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        verify(productRepository).save(product);
    }

    @Test
    void archiveSetsStatusToArchivedAndHidesTheProductFromBuyerQueries() {
        Long ownerId = 11L;
        Long shopId = 3L;
        Long productId = 7L;
        Product product = existingProduct(productId, ownerId);
        product.setShop(Shop.builder().id(shopId).owner(User.builder().id(ownerId).build()).build());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByShopIdAndStatus(shopId, ProductStatus.ACTIVE)).thenReturn(List.of());

        Product archived = productService.archiveProduct(ownerId, productId);
        List<Product> buyerProducts = productService.findProductsForBuyer(shopId);

        assertThat(archived.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
        assertThat(buyerProducts).isEmpty();
        verify(productRepository).save(product);
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
}
