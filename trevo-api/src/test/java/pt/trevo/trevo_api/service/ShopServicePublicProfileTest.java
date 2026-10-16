package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.ShopProfileResponse;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ReviewRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class ShopServicePublicProfileTest extends BaseUnitTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    void getPublicProfileReturnsShopInfoWithRatingAndCollections() {
        Long shopId = 1L;
        User owner = User.builder().id(10L).build();
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(owner)
            .name("My Shop")
            .description("A description")
            .logoUrl("https://logo.url")
            .bannerUrl("https://banner.url")
            .status(ShopStatus.ACTIVE)
            .build();

        Collection col1 = Collection.builder().id(1L).shop(shop).name("Summer").displayOrder(0).build();
        Collection col2 = Collection.builder().id(2L).shop(shop).name("Winter").displayOrder(1).build();

        when(shopRepository.findByIdAndActive(shopId)).thenReturn(Optional.of(shop));
        when(reviewRepository.findAverageRatingByShopId(shopId)).thenReturn(4.5);
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId)).thenReturn(List.of(col1, col2));

        ShopProfileResponse response = shopService.getPublicProfile(shopId);

        assertThat(response.id()).isEqualTo(shopId);
        assertThat(response.ownerId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("My Shop");
        assertThat(response.description()).isEqualTo("A description");
        assertThat(response.logoUrl()).isEqualTo("https://logo.url");
        assertThat(response.bannerUrl()).isEqualTo("https://banner.url");
        assertThat(response.status()).isEqualTo(ShopStatus.ACTIVE);
        assertThat(response.averageRating()).isEqualTo(4.5);
        assertThat(response.collections()).hasSize(2);
        assertThat(response.collections().get(0).name()).isEqualTo("Summer");
        assertThat(response.collections().get(1).name()).isEqualTo("Winter");
    }

    @Test
    void getPublicProfileReturnsNullRatingWhenNoReviews() {
        Long shopId = 1L;
        Shop shop = Shop.builder()
            .id(shopId)
            .owner(User.builder().id(10L).build())
            .name("My Shop")
            .status(ShopStatus.ACTIVE)
            .build();

        when(shopRepository.findByIdAndActive(shopId)).thenReturn(Optional.of(shop));
        when(reviewRepository.findAverageRatingByShopId(shopId)).thenReturn(null);
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId)).thenReturn(List.of());

        ShopProfileResponse response = shopService.getPublicProfile(shopId);

        assertThat(response.averageRating()).isNull();
        assertThat(response.collections()).isEmpty();
    }

    @Test
    void getPublicProfileThrowsNotFoundWhenShopIsDeactivated() {
        Long shopId = 1L;
        when(shopRepository.findByIdAndActive(shopId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.getPublicProfile(shopId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPublicProfileThrowsNotFoundWhenShopDoesNotExist() {
        when(shopRepository.findByIdAndActive(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            shopService.getPublicProfile(999L)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
