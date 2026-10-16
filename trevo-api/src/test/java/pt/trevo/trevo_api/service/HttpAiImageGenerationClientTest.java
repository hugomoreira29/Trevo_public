package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;

class HttpAiImageGenerationClientTest extends BaseUnitTest {

    @Mock private ReplicateApiClient replicateApiClient;

    private HttpAiImageGenerationClient client;

    @BeforeEach
    void setUp() {
        client = new HttpAiImageGenerationClient(replicateApiClient);
    }

    @Test
    void generateTryOnUsesPhotoUrlAsHumanImage() {
        Product product = productWithImage("https://cdn.example.com/shirt.jpg");
        UserPhotoInput photo = testPhoto("https://cloudinary.example.com/user.jpg");
        when(replicateApiClient.createAndAwaitPrediction(
            "https://cdn.example.com/shirt.jpg",
            "https://cloudinary.example.com/user.jpg",
            "Linen Shirt",
            "upper_body"
        )).thenReturn("https://replicate.delivery/result.png");

        String result = client.generateTryOn(product, photo);

        assertThat(result).isEqualTo("https://replicate.delivery/result.png");
    }

    @Test
    void generateTryOnThrows400WhenProductHasNoImages() {
        Product product = productWithoutImages();
        UserPhotoInput photo = testPhoto("https://cloudinary.example.com/user.jpg");

        Throwable thrown = catchThrowable(() -> client.generateTryOn(product, photo));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ResponseStatusException) thrown).getReason()).isEqualTo("Product has no images");
    }

    private UserPhotoInput testPhoto(String temporaryUrl) {
        return new UserPhotoInput() {
            @Override public String filename() { return "photo.jpg"; }
            @Override public String contentType() { return "image/jpeg"; }
            @Override public long size() { return 100; }
            @Override public byte[] bytes() { return new byte[] {1, 2, 3}; }
            @Override public String temporaryUrl() { return temporaryUrl; }
        };
    }

    private Product productWithImage(String imageUrl) {
        ProductImage image = new ProductImage();
        image.setImageUrl(imageUrl);
        Product product = Product.builder()
            .id(1L)
            .name("Linen Shirt")
            .price(new BigDecimal("49.99"))
            .status(ProductStatus.ACTIVE)
            .build();
        product.setImages(List.of(image));
        return product;
    }

    private Product productWithoutImages() {
        Product product = Product.builder()
            .id(2L)
            .name("Empty Product")
            .price(new BigDecimal("10.00"))
            .status(ProductStatus.ACTIVE)
            .build();
        product.setImages(List.of());
        return product;
    }
}
