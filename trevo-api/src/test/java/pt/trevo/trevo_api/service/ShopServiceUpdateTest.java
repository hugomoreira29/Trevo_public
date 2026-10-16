package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;
import pt.trevo.trevo_api.dto.UpdateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class ShopServiceUpdateTest extends BaseUnitTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ShopService shopService;

    @Test
    void updateShopUpdatesDescriptionWhenOwner() throws IOException {
        Long ownerId = 11L;
        Long shopId = 1L;
        UpdateShopRequest request = new UpdateShopRequest("Updated description", null, null);

        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .name("My Shop")
            .description("Old description")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop updated = shopService.updateShop(ownerId, shopId, request);

        assertThat(updated.getDescription()).isEqualTo("Updated description");
        verify(shopRepository).save(existingShop);
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    void updateShopUploadsLogoWhenProvided() throws IOException {
        Long ownerId = 11L;
        Long shopId = 1L;
        MultipartFile logoFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(logoFile.isEmpty()).thenReturn(false);

        UpdateShopRequest request = new UpdateShopRequest(null, logoFile, null);

        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(cloudinaryService.uploadImage(logoFile)).thenReturn(new CloudinaryUploadResult("logo-id", "https://cloudinary.com/logo.jpg"));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop updated = shopService.updateShop(ownerId, shopId, request);

        assertThat(updated.getLogoUrl()).isEqualTo("https://cloudinary.com/logo.jpg");
        verify(cloudinaryService).uploadImage(logoFile);
        verify(shopRepository).save(existingShop);
    }

    @Test
    void updateShopUploadsBannerWhenProvided() throws IOException {
        Long ownerId = 11L;
        Long shopId = 1L;
        MultipartFile bannerFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(bannerFile.isEmpty()).thenReturn(false);

        UpdateShopRequest request = new UpdateShopRequest(null, null, bannerFile);

        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(cloudinaryService.uploadImage(bannerFile)).thenReturn(new CloudinaryUploadResult("banner-id", "https://cloudinary.com/banner.jpg"));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop updated = shopService.updateShop(ownerId, shopId, request);

        assertThat(updated.getBannerUrl()).isEqualTo("https://cloudinary.com/banner.jpg");
        verify(cloudinaryService).uploadImage(bannerFile);
        verify(shopRepository).save(existingShop);
    }

    @Test
    void updateShopThrowsForbiddenWhenNotOwner() throws IOException {
        Long callerId = 22L;
        Long shopId = 1L;
        Long ownerId = 11L;
        UpdateShopRequest request = new UpdateShopRequest("Updated description", null, null);

        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.updateShop(callerId, shopId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(shopRepository, never()).save(any(Shop.class));
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    void updateShopThrowsNotFoundWhenShopDoesNotExist() throws IOException {
        Long ownerId = 11L;
        Long shopId = 999L;
        UpdateShopRequest request = new UpdateShopRequest("Updated description", null, null);

        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.updateShop(ownerId, shopId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void updateShopSkipsEmptyFiles() throws IOException {
        Long ownerId = 11L;
        Long shopId = 1L;
        MultipartFile emptyLogo = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile emptyBanner = org.mockito.Mockito.mock(MultipartFile.class);
        when(emptyLogo.isEmpty()).thenReturn(true);
        when(emptyBanner.isEmpty()).thenReturn(true);

        UpdateShopRequest request = new UpdateShopRequest("Updated description", emptyLogo, emptyBanner);

        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .name("My Shop")
            .description("Old description")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop updated = shopService.updateShop(ownerId, shopId, request);

        assertThat(updated.getDescription()).isEqualTo("Updated description");
        verify(cloudinaryService, never()).uploadImage(any());
        verify(shopRepository).save(existingShop);
    }
}
