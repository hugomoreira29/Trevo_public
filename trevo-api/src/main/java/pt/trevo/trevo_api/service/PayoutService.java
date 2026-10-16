package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.persistence.entity.PayoutStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.repository.PayoutRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final ShopRepository shopRepository;

    public PayoutService(PayoutRepository payoutRepository, ShopRepository shopRepository) {
        this.payoutRepository = payoutRepository;
        this.shopRepository = shopRepository;
    }

    public Payout createPayout(Order order) {
        BigDecimal payoutAmount = order.getTotalAmount().subtract(order.getCommissionAmount());
        Payout payout = new Payout();
        payout.setOrder(order);
        payout.setStatus(PayoutStatus.PENDING);
        payout.setPayoutAmount(payoutAmount);
        payout.setCreatedAt(Instant.now());
        return payoutRepository.save(payout);
    }

    public BigDecimal getPendingBalance(Long shopId) {
        BigDecimal sum = payoutRepository.sumByOrderShopIdAndStatus(shopId, PayoutStatus.PENDING);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public BigDecimal getPendingBalanceForOwner(Long ownerId) {
        Shop shop = requireShopByOwnerId(ownerId);
        return getPendingBalance(shop.getId());
    }

    public Page<Payout> getPayoutHistory(Long shopId, Pageable pageable) {
        return payoutRepository.findByOrderShopIdAndStatus(shopId, PayoutStatus.RELEASED, pageable);
    }

    public Page<Payout> getPayoutHistoryForOwner(Long ownerId, Pageable pageable) {
        Shop shop = requireShopByOwnerId(ownerId);
        return getPayoutHistory(shop.getId(), pageable);
    }

    private Shop requireShopByOwnerId(Long ownerId) {
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid owner id");
        }

        return shopRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
    }
}
