package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.Payout;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;
import pt.trevo.trevo_api.persistence.entity.PayoutStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.PayoutRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;

class PayoutServiceTest extends BaseUnitTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private PayoutService payoutService;

    // PAY-B05: createPayout

    @Test
    void createPayoutSavesRecordWithAmountEqualToTotalMinusCommission() {
        Order order = deliveredOrder(1L, "100.00", "10.00");
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        Payout payout = payoutService.createPayout(order);

        assertThat(payout.getPayoutAmount()).isEqualByComparingTo("90.00");
        assertThat(payout.getOrder()).isSameAs(order);
        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PENDING);
        verify(payoutRepository).save(payout);
    }

    @Test
    void createPayoutSetsStatusToPending() {
        Order order = deliveredOrder(2L, "50.00", "5.00");
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        Payout payout = payoutService.createPayout(order);

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.getReleasedAt()).isNull();
    }

    @Test
    void payoutAmountUsesStoredCommissionNotRecalculated() {
        Order order = deliveredOrder(3L, "200.00", "25.00");
        when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> inv.getArgument(0));

        Payout payout = payoutService.createPayout(order);

        assertThat(payout.getPayoutAmount()).isEqualByComparingTo("175.00");
    }

    // PAY-B06: getPendingBalance

    @Test
    void getPendingBalanceReturnsSumOfPendingPayoutsForShop() {
        Long shopId = 301L;
        when(payoutRepository.sumByOrderShopIdAndStatus(shopId, PayoutStatus.PENDING))
            .thenReturn(new BigDecimal("270.00"));

        BigDecimal balance = payoutService.getPendingBalance(shopId);

        assertThat(balance).isEqualByComparingTo("270.00");
        verify(payoutRepository).sumByOrderShopIdAndStatus(shopId, PayoutStatus.PENDING);
    }

    @Test
    void getPendingBalanceReturnsZeroWhenNoPayoutsExist() {
        Long shopId = 302L;
        when(payoutRepository.sumByOrderShopIdAndStatus(shopId, PayoutStatus.PENDING))
            .thenReturn(null);

        BigDecimal balance = payoutService.getPendingBalance(shopId);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getPendingBalanceForOwnerUsesTheOwnersShop() {
        Shop shop = Shop.builder().id(301L).owner(User.builder().id(901L).build()).build();
        when(shopRepository.findByOwnerId(901L)).thenReturn(Optional.of(shop));
        when(payoutRepository.sumByOrderShopIdAndStatus(301L, PayoutStatus.PENDING))
            .thenReturn(new BigDecimal("120.00"));

        BigDecimal balance = payoutService.getPendingBalanceForOwner(901L);

        assertThat(balance).isEqualByComparingTo("120.00");
        verify(shopRepository).findByOwnerId(901L);
        verify(payoutRepository).sumByOrderShopIdAndStatus(301L, PayoutStatus.PENDING);
    }

    // PAY-B06: getPayoutHistory

    @Test
    void getPayoutHistoryReturnsReleasedPayoutsForShopOrderedByReleasedAtDesc() {
        Long shopId = 301L;
        Instant t1 = Instant.parse("2026-05-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-05-10T10:00:00Z");
        Payout older = payout(1L, "90.00", t1);
        Payout newer = payout(2L, "135.00", t2);
        PageRequest pageable = PageRequest.of(0, 20);
        when(payoutRepository.findByOrderShopIdAndStatus(shopId, PayoutStatus.RELEASED, pageable))
            .thenReturn(new PageImpl<>(List.of(newer, older), pageable, 2));

        Page<Payout> history = payoutService.getPayoutHistory(shopId, pageable);

        assertThat(history.getContent()).hasSize(2);
        assertThat(history.getContent().get(0).getReleasedAt()).isEqualTo(t2);
        assertThat(history.getContent().get(1).getReleasedAt()).isEqualTo(t1);
        verify(payoutRepository).findByOrderShopIdAndStatus(shopId, PayoutStatus.RELEASED, pageable);
    }

    @Test
    void getPayoutHistoryReturnsEmptyListWhenNoReleasedPayouts() {
        Long shopId = 303L;
        PageRequest pageable = PageRequest.of(0, 20);
        when(payoutRepository.findByOrderShopIdAndStatus(eq(shopId), eq(PayoutStatus.RELEASED), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        Page<Payout> history = payoutService.getPayoutHistory(shopId, pageable);

        assertThat(history.getContent()).isEmpty();
    }

    @Test
    void getPayoutHistoryForOwnerUsesTheOwnersShop() {
        Shop shop = Shop.builder().id(301L).owner(User.builder().id(901L).build()).build();
        Payout payout = payout(1L, "90.00", Instant.parse("2026-05-10T10:00:00Z"));
        PageRequest pageable = PageRequest.of(0, 20);
        when(shopRepository.findByOwnerId(901L)).thenReturn(Optional.of(shop));
        when(payoutRepository.findByOrderShopIdAndStatus(301L, PayoutStatus.RELEASED, pageable))
            .thenReturn(new PageImpl<>(List.of(payout), pageable, 1));

        Page<Payout> history = payoutService.getPayoutHistoryForOwner(901L, pageable);

        assertThat(history.getContent()).containsExactly(payout);
        verify(shopRepository).findByOwnerId(901L);
        verify(payoutRepository).findByOrderShopIdAndStatus(301L, PayoutStatus.RELEASED, pageable);
    }

    @Test
    void payoutOwnerQueriesReturnNotFoundWhenOwnerHasNoShop() {
        when(shopRepository.findByOwnerId(901L)).thenReturn(Optional.empty());

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
            () -> payoutService.getPayoutHistoryForOwner(901L, PageRequest.of(0, 20))
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Shop not found");
    }

    // Helpers

    private Order deliveredOrder(Long id, String total, String commission) {
        Order order = new Order();
        order.setId(id);
        order.setBuyer(User.builder().id(10L).build());
        order.setShop(Shop.builder().id(301L).owner(User.builder().id(901L).build()).name("Test Shop").build());
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.MBWAY);
        order.setTotalAmount(new BigDecimal(total));
        order.setCommissionAmount(new BigDecimal(commission));
        order.setCreatedAt(Instant.now());
        return order;
    }

    private Payout payout(Long id, String amount, Instant releasedAt) {
        Payout p = new Payout();
        p.setId(id);
        p.setPayoutAmount(new BigDecimal(amount));
        p.setStatus(PayoutStatus.RELEASED);
        p.setCreatedAt(releasedAt.minusSeconds(3600));
        p.setReleasedAt(releasedAt);
        return p;
    }
}
