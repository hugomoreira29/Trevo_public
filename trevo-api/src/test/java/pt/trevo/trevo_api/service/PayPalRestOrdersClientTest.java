package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.PayPalCaptureResponse;
import pt.trevo.trevo_api.dto.PayPalOrderResponse;

class PayPalRestOrdersClientTest {

    private static final String BASE_URL = "https://paypal.test";
    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createOrderFetchesTokenAndParsesApproveLink() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        expectToken(server, "access-token");
        server.expect(once(), requestTo(BASE_URL + "/v2/checkout/orders"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bearer access-token"))
            .andExpect(header("PayPal-Request-Id", "trevo-order-create-77"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "intent": "CAPTURE",
                  "purchase_units": [
                    {
                      "reference_id": "77",
                      "amount": {
                        "currency_code": "EUR",
                        "value": "29.99"
                      }
                    }
                  ]
                }
                """))
            .andRespond(withCreatedEntity(URI.create(BASE_URL + "/v2/checkout/orders/PAYPAL-ORDER-123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                    {
                      "id": "PAYPAL-ORDER-123",
                      "status": "CREATED",
                      "links": [
                        {"href": "https://paypal.test/approve", "rel": "approve"}
                      ]
                    }
                    """));

        PayPalOrderResponse response = client.createOrder(
            77L,
            new BigDecimal("29.99"),
            "EUR",
            "trevo-order-create-77"
        );

        assertThat(response.id()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.approveUrl()).isEqualTo("https://paypal.test/approve");
        server.verify();
    }

    @Test
    void createOrderReturnsNullApproveUrlWhenApproveLinkIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        expectToken(server, "access-token");
        server.expect(once(), requestTo(BASE_URL + "/v2/checkout/orders"))
            .andExpect(method(POST))
            .andRespond(withCreatedEntity(URI.create(BASE_URL + "/v2/checkout/orders/PAYPAL-ORDER-123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                    {
                      "id": "PAYPAL-ORDER-123",
                      "status": "CREATED",
                      "links": [
                        {"href": "https://paypal.test/self", "rel": "self"}
                      ]
                    }
                    """));

        PayPalOrderResponse response = client.createOrder(
            77L,
            new BigDecimal("29.99"),
            "EUR",
            "trevo-order-create-77"
        );

        assertThat(response.approveUrl()).isNull();
        server.verify();
    }

    @Test
    void tokenFetchFailureThrowsBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        server.expect(once(), requestTo(BASE_URL + "/v1/oauth2/token"))
            .andExpect(method(POST))
            .andRespond(withBadRequest());

        Throwable thrown = catchThrowable(() ->
            client.createOrder(77L, new BigDecimal("29.99"), "EUR", "trevo-order-create-77")
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("PayPal access token request failed");
        server.verify();
    }

    @Test
    void createOrderFailureThrowsBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        expectToken(server, "access-token");
        server.expect(once(), requestTo(BASE_URL + "/v2/checkout/orders"))
            .andExpect(method(POST))
            .andRespond(withServerError());

        Throwable thrown = catchThrowable(() ->
            client.createOrder(77L, new BigDecimal("29.99"), "EUR", "trevo-order-create-77")
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("PayPal create order failed");
        server.verify();
    }

    @Test
    void captureOrderFetchesTokenAndSendsIdempotencyKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        expectToken(server, "access-token");
        server.expect(once(), requestTo(BASE_URL + "/v2/checkout/orders/PAYPAL-ORDER-123/capture"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bearer access-token"))
            .andExpect(header("PayPal-Request-Id", "trevo-order-capture-77"))
            .andRespond(withSuccess("""
                {
                  "id": "PAYPAL-ORDER-123",
                  "status": "COMPLETED"
                }
                """, MediaType.APPLICATION_JSON));

        PayPalCaptureResponse response = client.captureOrder(
            "PAYPAL-ORDER-123",
            "trevo-order-capture-77"
        );

        assertThat(response.id()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(response.status()).isEqualTo("COMPLETED");
        server.verify();
    }

    @Test
    void accessTokenIsCachedAcrossRequests() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PayPalRestOrdersClient client = client(builder);

        expectToken(server, "access-token");
        server.expect(times(2), requestTo(BASE_URL + "/v2/checkout/orders"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bearer access-token"))
            .andRespond(withCreatedEntity(URI.create(BASE_URL + "/v2/checkout/orders/PAYPAL-ORDER-123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                    {
                      "id": "PAYPAL-ORDER-123",
                      "status": "CREATED",
                      "links": []
                    }
                    """));

        client.createOrder(77L, new BigDecimal("29.99"), "EUR", "trevo-order-create-77");
        client.createOrder(78L, new BigDecimal("19.99"), "EUR", "trevo-order-create-78");

        server.verify();
    }

    private PayPalRestOrdersClient client(RestClient.Builder builder) {
        return new PayPalRestOrdersClient(builder, BASE_URL, CLIENT_ID, CLIENT_SECRET, CLOCK);
    }

    private void expectToken(MockRestServiceServer server, String accessToken) {
        server.expect(once(), requestTo(BASE_URL + "/v1/oauth2/token"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, basicAuth()))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string("grant_type=client_credentials"))
            .andRespond(withSuccess("""
                {
                  "access_token": "%s",
                  "expires_in": 32400
                }
                """.formatted(accessToken), MediaType.APPLICATION_JSON));
    }

    private String basicAuth() {
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
