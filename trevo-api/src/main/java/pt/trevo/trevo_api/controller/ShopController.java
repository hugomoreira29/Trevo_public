package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.multipart.MultipartFile;
import pt.trevo.trevo_api.dto.CollectionResponse;
import pt.trevo.trevo_api.dto.CreateCollectionRequest;
import pt.trevo.trevo_api.dto.CreateShopRequest;
import pt.trevo.trevo_api.dto.RenameCollectionRequest;
import pt.trevo.trevo_api.dto.ReorderCollectionsRequest;
import pt.trevo.trevo_api.dto.ShopDashboardResponse;
import pt.trevo.trevo_api.dto.ShopProfileResponse;
import pt.trevo.trevo_api.dto.ShopResponse;
import pt.trevo.trevo_api.dto.UpdateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CollectionService;
import pt.trevo.trevo_api.service.ShopService;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;
    private final CollectionService collectionService;
    private final SecurityUtils securityUtils;
    private final ImageUploadValidator imageUploadValidator;

    public ShopController(
        ShopService shopService,
        CollectionService collectionService,
        SecurityUtils securityUtils,
        ImageUploadValidator imageUploadValidator
    ) {
        this.shopService = shopService;
        this.collectionService = collectionService;
        this.securityUtils = securityUtils;
        this.imageUploadValidator = imageUploadValidator;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ShopDashboardResponse getDashboard(Authentication authentication) {
        Long callerId = securityUtils.requireUserId(authentication);
        return shopService.getDashboard(callerId);
    }

    // ── Shop CRUD ────────────────────────────────────────────────────────────

    @GetMapping("/{shopId}")
    public ShopProfileResponse getShopProfile(@PathVariable Long shopId) {
        return shopService.getPublicProfile(shopId);
    }

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse createShop(
        @Valid @RequestBody CreateShopRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Shop shop = shopService.createShop(callerId, request);
        return toShopResponse(shop);
    }

    @PutMapping("/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ShopResponse updateShop(
        @PathVariable Long shopId,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) MultipartFile logo,
        @RequestParam(required = false) MultipartFile banner,
        Authentication authentication
    ) throws IOException {
        imageUploadValidator.validateOptionalImage(logo, "logo");
        imageUploadValidator.validateOptionalImage(banner, "banner");

        Long callerId = securityUtils.requireUserId(authentication);
        UpdateShopRequest request = new UpdateShopRequest(description, logo, banner);
        Shop shop = shopService.updateShop(callerId, shopId, request);
        return toShopResponse(shop);
    }

    @PostMapping("/{shopId}/deactivate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('ADMIN')")
    public ShopResponse deactivateShop(@PathVariable Long shopId, Authentication authentication) {
        Long callerId = securityUtils.requireUserId(authentication);
        boolean isAdmin = securityUtils.hasRole(authentication, Role.ADMIN);
        Shop shop = shopService.deactivateShop(shopId, callerId, isAdmin);
        return toShopResponse(shop);
    }

    @PostMapping("/{shopId}/reactivate")
    @PreAuthorize("hasRole('SHOP_OWNER') or hasRole('ADMIN')")
    public ShopResponse reactivateShop(@PathVariable Long shopId, Authentication authentication) {
        Long callerId = securityUtils.requireUserId(authentication);
        boolean isAdmin = securityUtils.hasRole(authentication, Role.ADMIN);
        Shop shop = shopService.reactivateShop(shopId, callerId, isAdmin);
        return toShopResponse(shop);
    }

    // ── Collection CRUD ──────────────────────────────────────────────────────

    @PostMapping("/{shopId}/collections")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse createCollection(
        @PathVariable Long shopId,
        @Valid @RequestBody CreateCollectionRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Collection collection = collectionService.createCollection(callerId, shopId, request.name());
        return toCollectionResponse(collection);
    }

    @PatchMapping("/{shopId}/collections/{collectionId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public CollectionResponse renameCollection(
        @PathVariable Long shopId,
        @PathVariable Long collectionId,
        @Valid @RequestBody RenameCollectionRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        Collection collection = collectionService.renameCollection(callerId, collectionId, request.name());
        return toCollectionResponse(collection);
    }

    @PutMapping("/{shopId}/collections/reorder")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public List<CollectionResponse> reorderCollections(
        @PathVariable Long shopId,
        @Valid @RequestBody ReorderCollectionsRequest request,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        List<Collection> collections = collectionService.reorderCollections(callerId, shopId, request.collectionIds());
        return collections.stream().map(this::toCollectionResponse).toList();
    }

    @DeleteMapping("/{shopId}/collections/{collectionId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
        @PathVariable Long shopId,
        @PathVariable Long collectionId,
        Authentication authentication
    ) {
        Long callerId = securityUtils.requireUserId(authentication);
        collectionService.deleteCollection(callerId, collectionId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ShopResponse toShopResponse(Shop shop) {
        return new ShopResponse(
            shop.getId(),
            shop.getOwner().getId(),
            shop.getName(),
            shop.getDescription(),
            shop.getNif(),
            shop.getLogoUrl(),
            shop.getBannerUrl(),
            shop.getStatus()
        );
    }

    private CollectionResponse toCollectionResponse(Collection collection) {
        return new CollectionResponse(
            collection.getId(),
            collection.getShop().getId(),
            collection.getName(),
            collection.getDisplayOrder()
        );
    }
}
