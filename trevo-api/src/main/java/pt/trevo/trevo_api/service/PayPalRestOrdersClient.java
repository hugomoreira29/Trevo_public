package pt.trevo.trevo_api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import pt.trevo.trevo_api.dto.PayPalCaptureResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;

@Component
public class PayPalRestOrdersClient implements PayPalOrdersClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayPalRestOrdersClient.class);
    private static final String CAPTURE_INTENT = "CAPTURE";
    private static final String APPROVE_REL = "approve";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TOKEN_EXPIRY_SAFETY_WINDOW = Duration.ofMinutes(5);
    private static final Duration MAX_TOKEN_TTL = Duration.ofHours(8);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String cachedAccessToken;
    private Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    private final String returnUrl;
    private final String cancelUrl;

    @Autowired
    public PayPalRestOrdersClient(
        @Value("${PAYPAL_BASE_URL:https://api-m.sandbox.paypal.com}") String baseUrl,
        @Value("${PAYPAL_CLIENT_ID:}") String clientId,
        @Value("${PAYPAL_CLIENT_SECRET:}") String clientSecret,
        @Value("${PAYPAL_RETURN_URL:http://localhost:5173/checkout/paypal/return}") String returnUrl,
        @Value("${PAYPAL_CANCEL_URL:http://localhost:5173/checkout}") String cancelUrl
    ) {
        this(payPalRestClientBuilder(), baseUrl, clientId, clientSecret, Clock.systemUTC(), returnUrl, cancelUrl);
    }

    PayPalRestOrdersClient(
        RestClient.Builder restClientBuilder,
        String baseUrl,
        String clientId,
        String clientSecret,
        Clock clock
    ) {
        this(restClientBuilder, baseUrl, clientId, clientSecret, clock,
            "http://localhost:5173/checkout/paypal/return", "http://localhost:5173/checkout");
    }

    PayPalRestOrdersClient(
        RestClient.Builder restClientBuilder,
        String baseUrl,
        String clientId,
        String clientSecret,
        Clock clock,
        String returnUrl,
        String cancelUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public PayPalOrderResponse createOrder(Long orderId, BigDecimal amount, String currency, String requestId) {
        assertConfigured();

        try {
            PayPalCreateOrderApiResponse response = restClient.post()
                .uri("/v2/checkout/orders")
                .headers(headers -> {
                    headers.setBearerAuth(accessToken());
                    headers.set("PayPal-Request-Id", requestId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PayPalCreateOrderRequest(
                    CAPTURE_INTENT,
                    List.of(new PayPalPurchaseUnit(
                        orderId.toString(),
                        new PayPalAmount(currency, amount.toPlainString())
                    )),
                    new PayPalApplicationContext(returnUrl, cancelUrl)
                ))
                .retrieve()
                .body(PayPalCreateOrderApiResponse.class);

            if (response == null || response.id() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal create order failed");
            }

            return new PayPalOrderResponse(response.id(), response.status(), approveUrl(response.links()));
        } catch (RestClientException ex) {
            LOGGER.warn("PayPal create order failed for local order {}: {}", orderId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal create order failed", ex);
        }
    }

    @Override
    public PayPalCaptureResponse captureOrder(String paypalOrderId, String requestId) {
        assertConfigured();

        try {
            PayPalCaptureApiResponse response = restClient.post()
                .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                .headers(headers -> {
                    headers.setBearerAuth(accessToken());
                    headers.set("PayPal-Request-Id", requestId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .body(PayPalCaptureApiResponse.class);

            if (response == null || response.id() == null || response.status() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal capture failed");
            }

            return new PayPalCaptureResponse(response.id(), response.status());
        } catch (RestClientException ex) {
            LOGGER.warn("PayPal capture failed for PayPal order {}: {}", paypalOrderId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal capture failed", ex);
        }
    }

    private synchronized String accessToken() {
        Instant now = Instant.now(clock);
        if (cachedAccessToken != null && now.isBefore(cachedAccessTokenExpiresAt)) {
            return cachedAccessToken;
        }

        PayPalAccessTokenResponse response;
        try {
            response = restClient.post()
                .uri("/v1/oauth2/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(PayPalAccessTokenResponse.class);
        } catch (RestClientException ex) {
            LOGGER.warn("PayPal access token request failed: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal access token request failed", ex);
        }

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayPal access token request failed");
        }

        cachedAccessToken = response.accessToken();
        cachedAccessTokenExpiresAt = now.plus(tokenTtl(response.expiresIn()));
        return cachedAccessToken;
    }

    private void assertConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal credentials are not configured");
        }
    }

    private Duration tokenTtl(Long expiresIn) {
        long seconds = expiresIn == null ? MAX_TOKEN_TTL.toSeconds() : expiresIn;
        Duration ttl = Duration.ofSeconds(seconds)
            .minus(TOKEN_EXPIRY_SAFETY_WINDOW);

        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofMinutes(1);
        }

        return ttl.compareTo(MAX_TOKEN_TTL) > 0 ? MAX_TOKEN_TTL : ttl;
    }

    private static RestClient.Builder payPalRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private String approveUrl(List<PayPalLink> links) {
        if (links == null) {
            return null;
        }

        return links.stream()
            .filter(link -> APPROVE_REL.equals(link.rel()))
            .map(PayPalLink::href)
            .findFirst()
            .orElse(null);
    }

    private record PayPalAccessTokenResponse(
        @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in") Long expiresIn
    ) {
    }

    private record PayPalCreateOrderRequest(
        String intent,
        @com.fasterxml.jackson.annotation.JsonProperty("purchase_units") List<PayPalPurchaseUnit> purchaseUnits,
        @com.fasterxml.jackson.annotation.JsonProperty("application_context") PayPalApplicationContext applicationContext
    ) {
    }

    private record PayPalApplicationContext(
        @com.fasterxml.jackson.annotation.JsonProperty("return_url") String returnUrl,
        @com.fasterxml.jackson.annotation.JsonProperty("cancel_url") String cancelUrl
    ) {
    }

    private record PayPalPurchaseUnit(
        @com.fasterxml.jackson.annotation.JsonProperty("reference_id") String referenceId,
        PayPalAmount amount
    ) {
    }

    private record PayPalAmount(
        @com.fasterxml.jackson.annotation.JsonProperty("currency_code") String currencyCode,
        String value
    ) {
    }

    private record PayPalCreateOrderApiResponse(String id, String status, List<PayPalLink> links) {
    }

    private record PayPalCaptureApiResponse(String id, String status) {
    }

    private record PayPalLink(String href, String rel) {
    }
}
