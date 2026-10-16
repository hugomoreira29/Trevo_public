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
import pt.trevo.trevo_api.dto.CreateShopRequest;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class ShopServiceTest extends BaseUnitTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    void createShopCreatesShopForShopOwner() {
        Long ownerId = 11L;
        CreateShopRequest request = new CreateShopRequest("My Shop", "A description", "123456789", null, null);
        User ownerReference = User.builder().id(ownerId).build();

        when(shopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(shopRepository.findByNif("123456789")).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(ownerId)).thenReturn(ownerReference);
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop created = shopService.createShop(ownerId, request);

        assertThat(created.getOwner()).isNotNull();
        assertThat(created.getOwner().getId()).isEqualTo(ownerId);
        assertThat(created.getName()).isEqualTo("My Shop");
        verify(shopRepository).save(created);
    }

    @Test
    void createShopThrowsConflictWhenOwnerAlreadyHasShop() {
        Long ownerId = 11L;
        CreateShopRequest request = new CreateShopRequest("My Shop", "A description", "123456789", null, null);

        when(shopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(Shop.builder().id(5L).owner(User.builder().id(ownerId).build()).build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.createShop(ownerId, request)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void deactivateShopDeactivatesShopWhenCallerIsOwner() {
        Long shopId = 1L;
        Long ownerId = 11L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop deactivated = shopService.deactivateShop(shopId, ownerId);

        assertThat(deactivated.getStatus()).isEqualTo(ShopStatus.DEACTIVATED);
        verify(shopRepository).save(deactivated);
    }

    @Test
    void deactivateShopThrowsForbiddenWhenCallerIsNotOwner() {
        Long shopId = 1L;
        Long ownerId = 11L;
        Long callerId = 22L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.deactivateShop(shopId, callerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void reactivateShopReactivatesShopWhenCallerIsOwner() {
        Long shopId = 1L;
        Long ownerId = 11L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop reactivated = shopService.reactivateShop(shopId, ownerId);

        assertThat(reactivated.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        verify(shopRepository).save(reactivated);
    }

    @Test
    void reactivateShopThrowsForbiddenWhenCallerIsNotOwner() {
        Long shopId = 1L;
        Long ownerId = 11L;
        Long callerId = 22L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(ownerId).build())
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.reactivateShop(shopId, callerId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void deactivateShopAllowsAdminToManageAnyShop() {
        Long shopId = 1L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(11L).build())
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop deactivated = shopService.deactivateShop(shopId, 99L, true);

        assertThat(deactivated.getStatus()).isEqualTo(ShopStatus.DEACTIVATED);
        verify(shopRepository).save(shop);
    }

    @Test
    void reactivateShopAllowsAdminToManageAnyShop() {
        Long shopId = 1L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(11L).build())
            .status(ShopStatus.DEACTIVATED)
            .build();

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shop reactivated = shopService.reactivateShop(shopId, 99L, true);

        assertThat(reactivated.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        verify(shopRepository).save(shop);
    }
}
