package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Pattern;
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
import pt.trevo.trevo_api.dto.MbWayCallbackRequest;
import pt.trevo.trevo_api.dto.MbWayPaymentRequestResponse;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;

@Service
public class MbWayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MbWayService.class);
    private static final String EUR_CURRENCY = "EUR";
    private static final Set<String> SUCCESS_STATUSES = Set.of("PAID", "COMPLETED", "SUCCESS");
    private static final Pattern PORTUGUESE_MOBILE_PHONE = Pattern.compile("^(?:\\+351|00351)?9\\d{8}$");

    private final MbWayPaymentsClient mbWayPaymentsClient;
    private final OrderRepository orderRepository;
    private final MbWayCallbackVerifier callbackVerifier;
    private final TransactionOperations transactionOperations;

    @Autowired
    public MbWayService(
        MbWayPaymentsClient mbWayPaymentsClient,
        OrderRepository orderRepository,
        MbWayCallbackVerifier callbackVerifier,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        this(
            mbWayPaymentsClient,
            orderRepository,
            callbackVerifier,
            transactionOperations(transactionManagerProvider)
        );
    }

    MbWayService(
        MbWayPaymentsClient mbWayPaymentsClient,
        OrderRepository orderRepository,
        MbWayCallbackVerifier callbackVerifier,
        TransactionOperations transactionOperations
    ) {
        this.mbWayPaymentsClient = mbWayPaymentsClient;
        this.orderRepository = orderRepository;
        this.callbackVerifier = callbackVerifier;
        this.transactionOperations = transactionOperations;
    }

    public MbWayPaymentRequestResponse createPaymentRequest(Order order, String phoneNumber) {
        if (order == null || order.getId() == null || order.getTotalAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MB WAY payment request");
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MB WAY phone number");
        }

        MbWayPaymentRequestResponse response = mbWayPaymentsClient.createPaymentRequest(
            order.getId(),
            eurAmount(order.getTotalAmount()),
            EUR_CURRENCY,
            normalizePhoneNumber(phoneNumber)
        );

        transactionOperations.executeWithoutResult(status -> {
            order.setPaymentReference(response.reference());
            orderRepository.save(order);
        });

        return response;
    }

    public void handleCallback(MbWayCallbackRequest callback, String rawPayload, String signature) {
        callbackVerifier.verify(rawPayload, signature);
        handleVerifiedCallback(callback);
    }

    void handleVerifiedCallback(MbWayCallbackRequest callback) {
        if (callback == null || callback.reference() == null || callback.reference().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MB WAY callback");
        }

        if (!SUCCESS_STATUSES.contains(normalizeStatus(callback.status()))) {
            return;
        }

        Order order = orderRepository.findByPaymentReference(callback.reference())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            LOGGER.info(
                "Skipping MB WAY callback {} for order {} because status is {}",
                callback.reference(),
                order.getId(),
                order.getStatus()
            );
            return;
        }

        transactionOperations.executeWithoutResult(status -> {
            order.setStatus(OrderStatus.PLACED);
            orderRepository.save(order);
        });
    }

    private BigDecimal eurAmount(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PORTUGUESE_MOBILE_PHONE.matcher(phoneNumber.replace(" ", "")).matches();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String normalized = phoneNumber.replace(" ", "");
        if (normalized.startsWith("00351")) {
            return "+351" + normalized.substring(5);
        }
        if (normalized.startsWith("+351")) {
            return normalized;
        }
        return "+351" + normalized;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
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
