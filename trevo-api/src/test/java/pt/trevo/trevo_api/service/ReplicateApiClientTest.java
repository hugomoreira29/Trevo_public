package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReplicateApiClientTest {

    @Test
    void throwsBadGatewayWhenApiTokenIsBlank() {
        ReplicateApiClient client = new ReplicateApiClient("", "version-hash", 100, 500);

        Throwable thrown = catchThrowable(() ->
            client.createAndAwaitPrediction("garment.jpg", "human.jpg", "desc", "upper_body")
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException ex = (ResponseStatusException) thrown;
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getReason()).isEqualTo("AI image generation API is not configured");
    }

    @Test
    void throwsBadGatewayWhenApiTokenIsNull() {
        ReplicateApiClient client = new ReplicateApiClient(null, "version-hash", 100, 500);

        Throwable thrown = catchThrowable(() ->
            client.createAndAwaitPrediction("garment.jpg", "human.jpg", "desc", "upper_body")
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException ex = (ResponseStatusException) thrown;
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getReason()).isEqualTo("AI image generation API is not configured");
    }
}
