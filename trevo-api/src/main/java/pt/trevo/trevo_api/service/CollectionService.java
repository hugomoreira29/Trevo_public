package pt.trevo.trevo_api.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.Collection;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.repository.CollectionRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ShopRepository shopRepository;

    public CollectionService(CollectionRepository collectionRepository, ShopRepository shopRepository) {
        this.collectionRepository = collectionRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Collection createCollection(Long callerId, Long shopId, String name) {
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        if (!shop.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections");
        }

        int nextOrder = collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId).size();

        Collection collection = Collection.builder()
            .shop(shop)
            .name(name)
            .displayOrder(nextOrder)
            .build();

        return collectionRepository.save(collection);
    }

    @Transactional
    public Collection renameCollection(Long callerId, Long collectionId, String name) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        if (!collection.getShop().getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections");
        }

        collection.setName(name);
        return collectionRepository.save(collection);
    }

    @Transactional
    public List<Collection> reorderCollections(Long callerId, Long shopId, List<Long> collectionIds) {
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        if (!shop.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections");
        }

        List<Collection> collections = collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId);

        if (collections.size() != collectionIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collection IDs list must include all shop collections");
        }

        Map<Long, Collection> byId = collections.stream()
            .collect(Collectors.toMap(Collection::getId, c -> c));

        for (int i = 0; i < collectionIds.size(); i++) {
            Collection c = byId.get(collectionIds.get(i));
            if (c == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Collection " + collectionIds.get(i) + " does not belong to this shop");
            }
            c.setDisplayOrder(i);
        }

        return collectionRepository.saveAll(collections);
    }

    @Transactional
    public void deleteCollection(Long callerId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        if (!collection.getShop().getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage collections");
        }

        // DB ON DELETE CASCADE on product_collections.collection_id handles product-collection links
        collectionRepository.delete(collection);
    }

    @Transactional(readOnly = true)
    public List<Collection> findByShopId(Long shopId) {
        return collectionRepository.findByShopIdOrderByDisplayOrderAsc(shopId);
    }
}
