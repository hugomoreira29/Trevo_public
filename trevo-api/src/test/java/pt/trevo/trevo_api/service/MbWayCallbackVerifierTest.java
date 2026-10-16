package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MbWayCallbackVerifierTest {

    @Test
    void verifyAcceptsValidHmacSignature() {
        MbWayCallbackVerifier verifier = new MbWayCallbackVerifier("callback-secret");

        Throwable thrown = catchThrowable(() -> verifier.verify(
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=ea14798a9e6b182c0d47a5d31ed7e02609dfb5181a82b287f8c714fae483cb6b"
        ));

        assertThat(thrown).isNull();
    }

    @Test
    void verifyRejectsInvalidSignature() {
        MbWayCallbackVerifier verifier = new MbWayCallbackVerifier("callback-secret");

        Throwable thrown = catchThrowable(() -> verifier.verify(
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=bad"
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Invalid MB WAY callback signature");
    }

    @Test
    void verifyRejectsMissingSecret() {
        MbWayCallbackVerifier verifier = new MbWayCallbackVerifier("");

        Throwable thrown = catchThrowable(() -> verifier.verify(
            "{\"reference\":\"MBWAY-REF-123\",\"status\":\"PAID\"}",
            "sha256=ea14798a9e6b182c0d47a5d31ed7e02609dfb5181a82b287f8c714fae483cb6b"
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getReason()).isEqualTo("MB WAY callback secret is not configured");
    }
}
