package pt.trevo.trevo_api.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.TryOnImageResponse;
import pt.trevo.trevo_api.dto.TryOnUsageResponse;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.TryOnImage;
import pt.trevo.trevo_api.persistence.entity.TryOnUsage;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnImageRepository;
import pt.trevo.trevo_api.persistence.repository.TryOnUsageRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class TryOnService {

    private static final int FREE_GENERATION_LIMIT = 3;

    private final AiImageGenerationClient aiImageGenerationClient;
    private final ProductRepository productRepository;
    private final TryOnImageRepository tryOnImageRepository;
    private final TryOnUsageRepository tryOnUsageRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public TryOnService(
        AiImageGenerationClient aiImageGenerationClient,
        ProductRepository productRepository,
        TryOnImageRepository tryOnImageRepository,
        TryOnUsageRepository tryOnUsageRepository,
        UserRepository userRepository,
        SubscriptionService subscriptionService
    ) {
        this.aiImageGenerationClient = aiImageGenerationClient;
        this.productRepository = productRepository;
        this.tryOnImageRepository = tryOnImageRepository;
        this.tryOnUsageRepository = tryOnUsageRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public TryOnImageResponse generateTryOn(Long buyerId, Long productId, UserPhotoInput photo) {
        boolean proUser = subscriptionService.isProUser(buyerId);
        User buyer = findBuyer(buyerId);
        Product product = findProduct(productId);
        TryOnUsage usage = findOrCreateUsage(buyer, product);
        String modelIdentifier = "personalized:" + buyerId;

        enforceFreeTierLimit(proUser, buyerId);
        String imageUrl = aiImageGenerationClient.generateTryOn(product, photo);
        incrementUsage(usage);
        persistImage(usage, imageUrl, modelIdentifier);

        return new TryOnImageResponse(imageUrl);
    }

    public TryOnUsageResponse getUsage(Long buyerId) {
        boolean proUser = subscriptionService.isProUser(buyerId);
        int totalGenerations = tryOnUsageRepository.countTotalGenerations(buyerId);
        int generationsLeft = proUser ? Integer.MAX_VALUE : Math.max(0, FREE_GENERATION_LIMIT - totalGenerations);
        return new TryOnUsageResponse(proUser, totalGenerations, generationsLeft);
    }

    private void enforceFreeTierLimit(boolean proUser, Long buyerId) {
        if (!proUser) {
            int totalGenerations = tryOnUsageRepository.countTotalGenerations(buyerId);
            if (totalGenerations >= FREE_GENERATION_LIMIT) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Free users can generate up to " + FREE_GENERATION_LIMIT + " try-on images");
            }
        }
    }

    private User findBuyer(Long buyerId) {
        return userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private TryOnUsage findOrCreateUsage(User buyer, Product product) {
        return tryOnUsageRepository.findByBuyerIdAndProductId(buyer.getId(), product.getId())
            .orElseGet(() -> {
                TryOnUsage usage = new TryOnUsage();
                usage.setBuyer(buyer);
                usage.setProduct(product);
                usage.setGenerationCount(0);
                return usage;
            });
    }

    private void incrementUsage(TryOnUsage usage) {
        usage.setGenerationCount(usage.getGenerationCount() + 1);
        tryOnUsageRepository.save(usage);
    }

    private void persistImage(TryOnUsage usage, String imageUrl, String modelIdentifier) {
        TryOnImage image = new TryOnImage();
        image.setUsage(usage);
        image.setImageUrl(imageUrl);
        image.setModelIdentifier(modelIdentifier);
        image.setGeneratedAt(Instant.now());
        tryOnImageRepository.save(image);
    }
}
