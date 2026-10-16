package pt.trevo.trevo_api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MbWayCallbackVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String callbackSecret;

    public MbWayCallbackVerifier(@Value("${MBWAY_CALLBACK_SECRET:}") String callbackSecret) {
        this.callbackSecret = callbackSecret;
    }

    public void verify(String rawPayload, String signature) {
        assertConfigured();
        if (rawPayload == null || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MB WAY callback signature");
        }

        String expectedSignature = SIGNATURE_PREFIX + hmacSha256(rawPayload);
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.trim().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MB WAY callback signature");
        }
    }

    private void assertConfigured() {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MB WAY callback secret is not configured");
        }
    }

    private String hmacSha256(String rawPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MB WAY callback signature failed", ex);
        }
    }
}
