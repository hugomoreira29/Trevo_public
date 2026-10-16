package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

class CollectionServiceTest extends BaseUnitTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private CollectionService collectionService;

    // ── createCollection ─────────────────────────────────────────────────────

    @Test
    void createCollectionAssignsDisplayOrderEqualToExistingCount() {
        Long ownerId = 1L;
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, ownerId);

        List<Collection> existing = List.of(
            collection(1L, shop, "Summer", 0),
            collection(2L, shop, "Winter", 1)
        );
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId)).thenReturn(existing);
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));

        Collection created = collectionService.createCollection(ownerId, shopId, "Spring");

        assertThat(created.getName()).isEqualTo("Spring");
        assertThat(created.getDisplayOrder()).isEqualTo(2);
        verify(collectionRepository).save(created);
    }

    @Test
    void createCollectionAssignsZeroDisplayOrderWhenNoExistingCollections() {
        Long ownerId = 1L;
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, ownerId);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId)).thenReturn(List.of());
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));

        Collection created = collectionService.createCollection(ownerId, shopId, "First");

        assertThat(created.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void createCollectionThrowsNotFoundWhenShopMissing() {
        when(shopRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.createCollection(1L, 99L, "New")
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(collectionRepository, never()).save(any());
    }

    @Test
    void createCollectionThrowsForbiddenWhenCallerIsNotOwner() {
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, 1L);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.createCollection(99L, shopId, "New")
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(collectionRepository, never()).save(any());
    }

    // ── renameCollection ─────────────────────────────────────────────────────

    @Test
    void renameCollectionUpdatesName() {
        Long ownerId = 1L;
        Long collectionId = 5L;
        Shop shop = shopWithOwner(10L, ownerId);
        Collection col = collection(collectionId, shop, "Old Name", 0);

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(col));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));

        Collection renamed = collectionService.renameCollection(ownerId, collectionId, "New Name");

        assertThat(renamed.getName()).isEqualTo("New Name");
        verify(collectionRepository).save(col);
    }

    @Test
    void renameCollectionThrowsNotFoundWhenCollectionMissing() {
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.renameCollection(1L, 99L, "Name")
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameCollectionThrowsForbiddenWhenCallerIsNotOwner() {
        Long collectionId = 5L;
        Shop shop = shopWithOwner(10L, 1L);
        Collection col = collection(collectionId, shop, "Old Name", 0);

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(col));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.renameCollection(99L, collectionId, "New Name")
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(collectionRepository, never()).save(any());
    }

    // ── reorderCollections ───────────────────────────────────────────────────

    @Test
    void reorderCollectionsUpdatesDisplayOrderByListIndex() {
        Long ownerId = 1L;
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, ownerId);

        Collection col1 = collection(1L, shop, "A", 0);
        Collection col2 = collection(2L, shop, "B", 1);
        Collection col3 = collection(3L, shop, "C", 2);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId))
            .thenReturn(List.of(col1, col2, col3));
        when(collectionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Collection> reordered = collectionService.reorderCollections(
            ownerId, shopId, List.of(3L, 1L, 2L)
        );

        assertThat(col3.getDisplayOrder()).isEqualTo(0);
        assertThat(col1.getDisplayOrder()).isEqualTo(1);
        assertThat(col2.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void reorderCollectionsThrowsBadRequestWhenCountMismatches() {
        Long ownerId = 1L;
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, ownerId);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId))
            .thenReturn(List.of(collection(1L, shop, "A", 0), collection(2L, shop, "B", 1)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.reorderCollections(ownerId, shopId, List.of(1L))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reorderCollectionsThrowsBadRequestWhenIdDoesNotBelongToShop() {
        Long ownerId = 1L;
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, ownerId);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId))
            .thenReturn(List.of(collection(1L, shop, "A", 0)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.reorderCollections(ownerId, shopId, List.of(999L))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reorderCollectionsThrowsNotFoundWhenShopMissing() {
        when(shopRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.reorderCollections(1L, 99L, List.of(1L))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reorderCollectionsThrowsForbiddenWhenCallerIsNotOwner() {
        Long shopId = 10L;
        Shop shop = shopWithOwner(shopId, 1L);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.reorderCollections(99L, shopId, List.of(1L))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── deleteCollection ─────────────────────────────────────────────────────

    @Test
    void deleteCollectionCallsRepositoryDelete() {
        // DB ON DELETE CASCADE on product_collections.collection_id removes join rows automatically
        Long ownerId = 1L;
        Long collectionId = 5L;
        Shop shop = shopWithOwner(10L, ownerId);
        Collection col = collection(collectionId, shop, "Summer", 0);

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(col));

        collectionService.deleteCollection(ownerId, collectionId);

        verify(collectionRepository).delete(col);
    }

    @Test
    void deleteCollectionThrowsNotFoundWhenCollectionMissing() {
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.deleteCollection(1L, 99L)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCollectionThrowsForbiddenWhenCallerIsNotOwner() {
        Long collectionId = 5L;
        Shop shop = shopWithOwner(10L, 1L);
        Collection col = collection(collectionId, shop, "Summer", 0);

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(col));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            collectionService.deleteCollection(99L, collectionId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(collectionRepository, never()).delete(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Shop shopWithOwner(Long shopId, Long ownerId) {
        User owner = User.builder().id(ownerId).build();
        return Shop.builder().id(shopId).owner(owner).build();
    }

    private Collection collection(Long id, Shop shop, String name, int displayOrder) {
        return Collection.builder().id(id).shop(shop).name(name).displayOrder(displayOrder).build();
    }
}
