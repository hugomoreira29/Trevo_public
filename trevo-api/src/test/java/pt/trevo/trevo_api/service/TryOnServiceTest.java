package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.TryOnImageResponse;
import pt.trevo.trevo_api.dto.TryOnUsageResponse;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.TryOnImage;
import pt.trevo.trevo_api.persistence.entity.TryOnUsage;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnImageRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnUsageRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class TryOnServiceTest extends BaseUnitTest {

    @Mock private AiImageGenerationClient aiImageGenerationClient;
    @Mock private ProductRepository productRepository;
    @Mock private TryOnImageRepository tryOnImageRepository;
    @Mock private TryOnUsageRepository tryOnUsageRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionService subscriptionService;

    @Test
    void generateTryOnAllowsFreeUserAndStoresResult() {
        User buyer = buyer(10L);
        Product product = product(20L);
        UserPhotoInput photo = testPhoto();
        when(subscriptionService.isProUser(10L)).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(tryOnUsageRepository.findByBuyerIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
        when(tryOnUsageRepository.countTotalGenerations(10L)).thenReturn(0);
        when(aiImageGenerationClient.generateTryOn(product, photo)).thenReturn("https://ai.example/result.png");
        when(tryOnUsageRepository.save(any(TryOnUsage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tryOnImageRepository.save(any(TryOnImage.class))).thenAnswer(inv -> inv.getArgument(0));

        TryOnImageResponse response = service().generateTryOn(10L, 20L, photo);

        assertThat(response.imageUrl()).isEqualTo("https://ai.example/result.png");
        ArgumentCaptor<TryOnUsage> usageCaptor = ArgumentCaptor.forClass(TryOnUsage.class);
        verify(tryOnUsageRepository).save(usageCaptor.capture());
        assertThat(usageCaptor.getValue().getGenerationCount()).isEqualTo(1);
    }

    @Test
    void generateTryOnRejectsFreeUserAfterThreeGenerations() {
        User buyer = buyer(10L);
        Product product = product(20L);
        UserPhotoInput photo = testPhoto();
        when(subscriptionService.isProUser(10L)).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(tryOnUsageRepository.findByBuyerIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
        when(tryOnUsageRepository.countTotalGenerations(10L)).thenReturn(3);

        Throwable thrown = catchThrowable(() -> service().generateTryOn(10L, 20L, photo));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException ex = (ResponseStatusException) thrown;
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        verify(aiImageGenerationClient, never()).generateTryOn(any(), any());
    }

    @Test
    void generateTryOnAllowsProUserUnlimitedGenerations() {
        User buyer = buyer(10L);
        Product product = product(20L);
        UserPhotoInput photo = testPhoto();
        when(subscriptionService.isProUser(10L)).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(tryOnUsageRepository.findByBuyerIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
        when(aiImageGenerationClient.generateTryOn(product, photo)).thenReturn("https://ai.example/pro.png");
        when(tryOnUsageRepository.save(any(TryOnUsage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tryOnImageRepository.save(any(TryOnImage.class))).thenAnswer(inv -> inv.getArgument(0));

        TryOnImageResponse response = service().generateTryOn(10L, 20L, photo);

        assertThat(response.imageUrl()).isEqualTo("https://ai.example/pro.png");
    }

    @Test
    void generateTryOnReturnsCachedResultWithoutCallingAi() {
        User buyer = buyer(10L);
        Product product = product(20L);
        TryOnUsage usage = usage(buyer, product, 1);
        TryOnImage cachedImage = new TryOnImage();
        cachedImage.setImageUrl("https://ai.example/cached.png");
        cachedImage.setModelIdentifier("personalized:10");
        when(subscriptionService.isProUser(10L)).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(tryOnUsageRepository.findByBuyerIdAndProductId(10L, 20L)).thenReturn(Optional.of(usage));
        when(tryOnImageRepository.findFirstCachedForUsage(30L, "personalized:10")).thenReturn(Optional.of(cachedImage));

        TryOnImageResponse response = service().generateTryOn(10L, 20L, testPhoto());

        assertThat(response.imageUrl()).isEqualTo("https://ai.example/cached.png");
        verify(aiImageGenerationClient, never()).generateTryOn(any(), any());
        verify(tryOnUsageRepository, never()).save(any());
    }

    @Test
    void getUsageReturnsFreeUserStats() {
        when(subscriptionService.isProUser(10L)).thenReturn(false);
        when(tryOnUsageRepository.countTotalGenerations(10L)).thenReturn(2);

        TryOnUsageResponse usage = service().getUsage(10L);

        assertThat(usage.pro()).isFalse();
        assertThat(usage.generationsUsed()).isEqualTo(2);
        assertThat(usage.generationsLeft()).isEqualTo(1);
    }

    @Test
    void getUsageReturnsProUserStats() {
        when(subscriptionService.isProUser(10L)).thenReturn(true);
        when(tryOnUsageRepository.countTotalGenerations(10L)).thenReturn(50);

        TryOnUsageResponse usage = service().getUsage(10L);

        assertThat(usage.pro()).isTrue();
        assertThat(usage.generationsUsed()).isEqualTo(50);
        assertThat(usage.generationsLeft()).isEqualTo(Integer.MAX_VALUE);
    }

    private TryOnService service() {
        return new TryOnService(
            aiImageGenerationClient,
            productRepository,
            tryOnImageRepository,
            tryOnUsageRepository,
            userRepository,
            subscriptionService
        );
    }

    private User buyer(Long id) {
        return User.builder().id(id).email("buyer@example.com").build();
    }

    private Product product(Long id) {
        return Product.builder()
            .id(id)
            .name("Linen Shirt")
            .price(new BigDecimal("49.99"))
            .status(ProductStatus.ACTIVE)
            .build();
    }

    private TryOnUsage usage(User buyer, Product product, int generationCount) {
        TryOnUsage usage = new TryOnUsage();
        usage.setId(30L);
        usage.setBuyer(buyer);
        usage.setProduct(product);
        usage.setGenerationCount(generationCount);
        return usage;
    }

    private UserPhotoInput testPhoto() {
        return new UserPhotoInput() {
            @Override public String filename() { return "photo.jpg"; }
            @Override public String contentType() { return "image/jpeg"; }
            @Override public long size() { return 100; }
            @Override public byte[] bytes() { return new byte[] {1, 2, 3}; }
            @Override public String temporaryUrl() { return "https://cloudinary.example.com/photo.jpg"; }
        };
    }
}
