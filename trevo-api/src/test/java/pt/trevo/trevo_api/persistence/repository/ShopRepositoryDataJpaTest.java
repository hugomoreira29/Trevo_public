package pt.trevo.trevo_api.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class ShopRepositoryDataJpaTest {

    private static long userSequence = 0;
    private static long shopSequence = 0;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ShopRepository shopRepository;

    @Test
    void deactivatedShopIsExcludedFromPublicQueries() {
        Shop activeShop = persistShop(ShopStatus.ACTIVE);
        Shop deactivatedShop = persistShop(ShopStatus.DEACTIVATED);

        Page<Shop> activeShops = shopRepository.findAllActive(PageRequest.of(0, 10));

        assertThat(activeShops.getContent()).extracting(Shop::getId).containsExactly(activeShop.getId());
        assertThat(activeShops.getContent()).extracting(Shop::getId).doesNotContain(deactivatedShop.getId());
    }

    @Test
    void deactivatedShopIsExcludedFromFindByIdAndActive() {
        Shop deactivatedShop = persistShop(ShopStatus.DEACTIVATED);

        assertThat(shopRepository.findByIdAndActive(deactivatedShop.getId())).isEmpty();
    }

    @Test
    void activeShopIsIncludedInFindByIdAndActive() {
        Shop activeShop = persistShop(ShopStatus.ACTIVE);

        assertThat(shopRepository.findByIdAndActive(activeShop.getId()))
            .get()
            .extracting(Shop::getId)
            .isEqualTo(activeShop.getId());
    }

    @Test
    void deactivatedShopReappearsInPublicQueriesAfterReactivation() {
        Shop shop = persistShop(ShopStatus.DEACTIVATED);
        shop.setStatus(ShopStatus.ACTIVE);
        entityManager.flush();
        entityManager.clear();

        Page<Shop> activeShops = shopRepository.findAllActive(PageRequest.of(0, 10));

        assertThat(activeShops.getContent()).extracting(Shop::getId).contains(shop.getId());
    }

    private Shop persistShop(ShopStatus status) {
        User owner = new User();
        userSequence++;
        owner.setEmail("owner" + userSequence + "@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Owner " + userSequence);
        owner.setRole(Role.SHOP_OWNER);
        owner.setStatus(Status.ACTIVE);
        entityManager.persist(owner);

        Shop shop = new Shop();
        shopSequence++;
        shop.setOwner(owner);
        shop.setName("Shop " + shopSequence);
        shop.setNif(String.format("%09d", shopSequence));
        shop.setStatus(status);
        entityManager.persist(shop);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Shop.class, shop.getId());
    }
}
