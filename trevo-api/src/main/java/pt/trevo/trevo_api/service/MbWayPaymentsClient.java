package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import pt.trevo.trevo_api.dto.MbWayPaymentRequestResponse;

public interface MbWayPaymentsClient {

    MbWayPaymentRequestResponse createPaymentRequest(Long orderId, BigDecimal amount, String currency, String phoneNumber);
}
