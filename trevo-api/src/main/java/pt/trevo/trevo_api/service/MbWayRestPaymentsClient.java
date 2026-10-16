package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.MbWayPaymentRequestResponse;

@Component
public class MbWayRestPaymentsClient implements MbWayPaymentsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MbWayRestPaymentsClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public MbWayRestPaymentsClient(
        @Value("${MBWAY_BASE_URL:https://api.example-mbway-provider.test}") String baseUrl,
        @Value("${MBWAY_API_KEY:}") String apiKey
    ) {
        this(mbWayRestClientBuilder(), baseUrl, apiKey);
    }

    MbWayRestPaymentsClient(RestClient.Builder restClientBuilder, String baseUrl, String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public MbWayPaymentRequestResponse createPaymentRequest(
        Long orderId,
        BigDecimal amount,
        String currency,
        String phoneNumber
    ) {
        assertConfigured();

        try {
            MbWayPaymentRequestResponse response = restClient.post()
                .uri("/payments/mbway")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MbWayPaymentRequest(orderId.toString(), amount.toPlainString(), currency, phoneNumber))
                .retrieve()
                .body(MbWayPaymentRequestResponse.class);

            if (response == null || response.reference() == null || response.reference().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MB WAY payment request failed");
            }

            return response;
        } catch (RestClientException ex) {
            LOGGER.warn("MB WAY payment request failed for order {}: {}", orderId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MB WAY payment request failed", ex);
        }
    }

    private void assertConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MB WAY API key is not configured");
        }
    }

    private static RestClient.Builder mbWayRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private record MbWayPaymentRequest(
        String orderId,
        String amount,
        String currency,
        String phoneNumber
    ) {
    }
}
