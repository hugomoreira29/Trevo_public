package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class ShopServiceDeactivateReactivateTest extends BaseUnitTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ShopService shopService;

    @Test
    void deactivateShopSetsStatusToDeactivated() {
        Long shopId = 1L;
        Long callerId = 11L;
        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(callerId).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop deactivated = shopService.deactivateShop(shopId, callerId);

        assertThat(deactivated.getStatus()).isEqualTo(ShopStatus.DEACTIVATED);
        verify(shopRepository).save(existingShop);
    }

    @Test
    void reactivateShopSetsStatusToActive() {
        Long shopId = 1L;
        Long callerId = 11L;
        Shop existingShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(callerId).build())
            .name("My Shop")
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop reactivated = shopService.reactivateShop(shopId, callerId);

        assertThat(reactivated.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        verify(shopRepository).save(existingShop);
    }

    @Test
    void deactivateShopThrowsNotFoundWhenShopDoesNotExist() {
        Long shopId = 999L;
        Long callerId = 11L;

        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.deactivateShop(shopId, callerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void reactivateShopThrowsNotFoundWhenShopDoesNotExist() {
        Long shopId = 999L;
        Long callerId = 11L;

        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.reactivateShop(shopId, callerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void deactivatedShopReappersInPublicQueriesAfterReactivation() {
        Long shopId = 1L;
        Long callerId = 11L;
        Shop deactivatedShop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(callerId).build())
            .name("Reactivated Shop")
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(deactivatedShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop reactivated = shopService.reactivateShop(shopId, callerId);

        assertThat(reactivated.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        verify(shopRepository).save(any(Shop.class));
    }
}
