package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.PayPalCaptureResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;

@Service
public class PayPalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayPalService.class);
    private static final String EUR_CURRENCY = "EUR";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final PayPalOrdersClient payPalOrdersClient;
    private final OrderRepository orderRepository;
    private final TransactionOperations transactionOperations;

    @Autowired
    public PayPalService(
        PayPalOrdersClient payPalOrdersClient,
        OrderRepository orderRepository,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        this(payPalOrdersClient, orderRepository, transactionOperations(transactionManagerProvider));
    }

    PayPalService(
        PayPalOrdersClient payPalOrdersClient,
        OrderRepository orderRepository,
        TransactionOperations transactionOperations
    ) {
        this.payPalOrdersClient = payPalOrdersClient;
        this.orderRepository = orderRepository;
        this.transactionOperations = transactionOperations;
    }

    public PayPalOrderResponse createOrder(Order order) {
        if (order == null || order.getId() == null || order.getTotalAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PayPal order request");
        }

        BigDecimal amount = eurAmount(order.getTotalAmount());
        PayPalOrderResponse payPalOrder = payPalOrdersClient.createOrder(
            order.getId(),
            amount,
            EUR_CURRENCY,
            "trevo-order-create-" + order.getId()
        );

        transactionOperations.executeWithoutResult(status -> {
            order.setPaymentReference(payPalOrder.id());
            orderRepository.save(order);
        });

        return payPalOrder;
    }

    public PayPalCaptureResponse capturePayment(String paypalOrderId) {
        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PayPal capture request");
        }

        Order order = orderRepository.findByPaymentReference(paypalOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            LOGGER.info(
                "Skipping PayPal capture {} for order {} because status is {}",
                paypalOrderId,
                order.getId(),
                order.getStatus()
            );
            return new PayPalCaptureResponse(paypalOrderId, "SKIPPED");
        }

        PayPalCaptureResponse capture = payPalOrdersClient.captureOrder(
            paypalOrderId,
            "trevo-order-capture-" + order.getId()
        );
        if (!COMPLETED_STATUS.equals(capture.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal capture failed");
        }

        transactionOperations.executeWithoutResult(status -> {
            Order orderToUpdate = orderRepository.findByPaymentReference(paypalOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            if (orderToUpdate.getStatus() != OrderStatus.PENDING_PAYMENT) {
                LOGGER.info(
                    "Skipping PayPal capture {} for order {} because status changed to {}",
                    paypalOrderId,
                    orderToUpdate.getId(),
                    orderToUpdate.getStatus()
                );
                return;
            }

            orderToUpdate.setStatus(OrderStatus.PLACED);
            orderRepository.save(orderToUpdate);
        });

        return capture;
    }

    private BigDecimal eurAmount(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static TransactionOperations transactionOperations(
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();
        if (transactionManager == null) {
            return TransactionOperations.withoutTransaction();
        }

        return new TransactionTemplate(transactionManager);
    }
}
