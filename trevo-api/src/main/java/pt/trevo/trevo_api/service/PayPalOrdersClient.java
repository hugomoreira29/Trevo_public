package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import pt.trevo.trevo_api.dto.PayPalCaptureResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;

public interface PayPalOrdersClient {

    PayPalOrderResponse createOrder(Long orderId, BigDecimal amount, String currency, String requestId);

    PayPalCaptureResponse captureOrder(String paypalOrderId, String requestId);
}
