package pt.trevo.trevo_api.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.GarmentType;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;

@Service
public class HttpAiImageGenerationClient implements AiImageGenerationClient {

    private final ReplicateApiClient replicateApiClient;

    public HttpAiImageGenerationClient(ReplicateApiClient replicateApiClient) {
        this.replicateApiClient = replicateApiClient;
    }

    @Override
    public String generateTryOn(Product product, UserPhotoInput photo) {
        String garmentImageUrl = firstProductImageUrl(product);
        String humanImageUrl = photo.temporaryUrl();
        String prompt = buildTryOnPrompt(product);
        return replicateApiClient.createAndAwaitPrediction(prompt, List.of(garmentImageUrl, humanImageUrl));
    }

    private String buildTryOnPrompt(Product product) {
        if (product.getGarmentType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product must have a garment type for virtual try-on");
        }
        String garmentLabel = switch (product.getGarmentType()) {
            case UPPER_BODY -> "upper body clothing item (shirt, jacket, or top)";
            case LOWER_BODY -> "lower body clothing item (pants, skirt, or shorts)";
            case DRESSES -> "dress or full-length garment";
        };
        return "The first image shows a " + garmentLabel + " called \"" + product.getName() + "\". " +
               "The second image shows a person. " +
               "Dress the person from the second image in the clothing item from the first image. " +
               "Keep the person's face, hair, body shape, skin tone, pose, and background exactly the same. " +
               "Only replace the clothing with the garment from the first image. " +
               "The result must look photorealistic with natural lighting and realistic fabric draping.";
    }

    private String firstProductImageUrl(Product product) {
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product has no images");
        }
        return images.getFirst().getImageUrl();
    }
}
