package pt.trevo.trevo_api.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;
import pt.trevo.trevo_api.dto.CollectionResponse;
import pt.trevo.trevo_api.dto.CreateShopRequest;
import pt.trevo.trevo_api.dto.ShopDashboardResponse;
import pt.trevo.trevo_api.dto.ShopProfileResponse;
import pt.trevo.trevo_api.dto.ShopResponse;
import pt.trevo.trevo_api.dto.UpdateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.ReviewRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ShopService(
        ShopRepository shopRepository,
        UserRepository userRepository,
        CloudinaryService cloudinaryService,
        ReviewRepository reviewRepository,
        CollectionRepository collectionRepository,
        OrderRepository orderRepository,
        ProductRepository productRepository
    ) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ShopDashboardResponse getDashboard(Long callerId) {
        Shop shop = shopRepository.findByOwnerId(callerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        List<Order> orders = orderRepository.findAllByShopIdOrderByCreatedAtDesc(shop.getId());

        List<ShopDashboardResponse.DashboardOrder> dashboardOrders = orders.stream()
            .map(o -> new ShopDashboardResponse.DashboardOrder(
                o.getId(),
                o.getStatus().name(),
                o.getBuyer().getFullName()
            ))
            .toList();

        BigDecimal total = orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        ShopResponse shopResponse = new ShopResponse(
            shop.getId(),
            shop.getOwner().getId(),
            shop.getName(),
            shop.getDescription(),
            shop.getNif(),
            shop.getLogoUrl(),
            shop.getBannerUrl(),
            shop.getStatus()
        );

        int productCount = productRepository.findByShopIdAndStatus(shop.getId(), ProductStatus.ACTIVE).size();

        return new ShopDashboardResponse(
            shopResponse,
            dashboardOrders,
            new ShopDashboardResponse.Revenue(total, "EUR"),
            productCount
        );
    }

    @Transactional
    public Shop createShop(Long callerId, CreateShopRequest request) {
        if (callerId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid create shop request");
        }

        if (shopRepository.findByOwnerId(callerId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner already has a shop");
        }

        // Optional: enforce unique NIF
        if (request.nif() != null && shopRepository.findByNif(request.nif()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NIF already in use");
        }

        Shop shop = Shop.builder()
            .owner(userRepository.getReferenceById(callerId))
            .name(request.name())
            .description(request.description())
            .nif(request.nif())
            .logoUrl(request.logoUrl())
            .bannerUrl(request.bannerUrl())
            .status(ShopStatus.PENDING)
            .build();

        return shopRepository.save(shop);
    }

    @Transactional
    public Shop updateShop(Long callerId, Long shopId, UpdateShopRequest request) throws IOException {
        if (callerId == null || shopId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid update shop request");
        }

        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        if (!shop.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can update this shop");
        }

        if (request.description() != null) {
            shop.setDescription(request.description());
        }

        if (request.logo() != null && !request.logo().isEmpty()) {
            CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(request.logo());
            shop.setLogoUrl(uploadResult.url());
        }

        if (request.banner() != null && !request.banner().isEmpty()) {
            CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(request.banner());
            shop.setBannerUrl(uploadResult.url());
        }

        return shopRepository.save(shop);
    }

    @Transactional(readOnly = true)
    public Optional<Shop> findByIdAndActive(Long shopId) {
        if (shopId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop id");
        }

        return shopRepository.findByIdAndActive(shopId);
    }

    @Transactional(readOnly = true)
    public Page<Shop> findAllActive(Pageable pageable) {
        if (pageable == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination is required");
        }

        return shopRepository.findAllActive(pageable);
    }

    @Transactional
    public Shop deactivateShop(Long shopId, Long callerId) {
        return deactivateShop(shopId, callerId, false);
    }

    @Transactional
    public Shop deactivateShop(Long shopId, Long callerId, boolean isAdmin) {
        if (shopId == null || callerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop id or caller id");
        }

        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        if (!isAdmin && !shop.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can deactivate this shop");
        }

        shop.setStatus(ShopStatus.DEACTIVATED);
        return shopRepository.save(shop);
    }

    @Transactional
    public Shop reactivateShop(Long shopId, Long callerId) {
        return reactivateShop(shopId, callerId, false);
    }

    @Transactional
    public Shop reactivateShop(Long shopId, Long callerId, boolean isAdmin) {
        if (shopId == null || callerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shop id or caller id");
        }

        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        if (!isAdmin && !shop.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can reactivate this shop");
        }

        shop.setStatus(ShopStatus.ACTIVE);
        return shopRepository.save(shop);
    }

    @Transactional(readOnly = true)
    public ShopProfileResponse getPublicProfile(Long shopId) {
        Shop shop = shopRepository.findByIdAndActive(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        Double averageRating = reviewRepository.findAverageRatingByShopId(shopId);
        List<Collection> collections = collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId);

        List<CollectionResponse> collectionResponses = collections.stream()
            .map(c -> new CollectionResponse(c.getId(), shopId, c.getName(), c.getDisplayOrder()))
            .toList();

        return new ShopProfileResponse(
            shop.getId(),
            shop.getOwner().getId(),
            shop.getName(),
            shop.getDescription(),
            shop.getLogoUrl(),
            shop.getBannerUrl(),
            shop.getStatus(),
            averageRating,
            collectionResponses
        );
    }
}
