package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.MbWayPaymentRequestResponse;

class MbWayRestPaymentsClientTest {

    private static final String BASE_URL = "https://mbway-provider.test";
    private static final String API_KEY = "api-key";

    @Test
    void createPaymentRequestPostsPaymentAndReturnsReference() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MbWayRestPaymentsClient client = client(builder);

        server.expect(once(), requestTo(BASE_URL + "/payments/mbway"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bearer " + API_KEY))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "orderId": "77",
                  "amount": "29.99",
                  "currency": "EUR",
                  "phoneNumber": "+351923456789"
                }
                """))
            .andRespond(withSuccess("""
                {
                  "reference": "MBWAY-REF-123",
                  "status": "PENDING"
                }
                """, MediaType.APPLICATION_JSON));

        MbWayPaymentRequestResponse response = client.createPaymentRequest(
            77L,
            new BigDecimal("29.99"),
            "EUR",
            "+351923456789"
        );

        assertThat(response.reference()).isEqualTo("MBWAY-REF-123");
        assertThat(response.status()).isEqualTo("PENDING");
        server.verify();
    }

    @Test
    void createPaymentRequestThrowsWhenReferenceIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MbWayRestPaymentsClient client = client(builder);

        server.expect(once(), requestTo(BASE_URL + "/payments/mbway"))
            .andExpect(method(POST))
            .andRespond(withSuccess("""
                {
                  "reference": " ",
                  "status": "PENDING"
                }
                """, MediaType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() -> client.createPaymentRequest(
            77L,
            new BigDecimal("29.99"),
            "EUR",
            "+351923456789"
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("MB WAY payment request failed");
        server.verify();
    }

    @Test
    void createPaymentRequestWrapsTransportFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MbWayRestPaymentsClient client = client(builder);

        server.expect(once(), requestTo(BASE_URL + "/payments/mbway"))
            .andExpect(method(POST))
            .andRespond(withServerError());

        Throwable thrown = catchThrowable(() -> client.createPaymentRequest(
            77L,
            new BigDecimal("29.99"),
            "EUR",
            "+351923456789"
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getReason()).isEqualTo("MB WAY payment request failed");
        server.verify();
    }

    private MbWayRestPaymentsClient client(RestClient.Builder builder) {
        return new MbWayRestPaymentsClient(builder, BASE_URL, API_KEY);
    }
}
