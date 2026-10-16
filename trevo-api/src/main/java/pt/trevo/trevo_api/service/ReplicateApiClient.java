package pt.trevo.trevo_api.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplicateApiClient {

    private static final Logger log = LoggerFactory.getLogger(ReplicateApiClient.class);
    private static final String REPLICATE_API_BASE = "https://api.replicate.com/v1";

    private final String apiToken;
    private final long pollIntervalMs;
    private final long timeoutMs;
    private final RestClient restClient;

    public ReplicateApiClient(
        @Value("${replicate.api-token:}") String apiToken,
        @Value("${replicate.poll-interval-ms:2000}") long pollIntervalMs,
        @Value("${replicate.timeout-ms:240000}") long timeoutMs
    ) {
        this.apiToken = apiToken;
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
        this.restClient = RestClient.builder()
            .baseUrl(REPLICATE_API_BASE)
            .build();
    }

    public String createAndAwaitPrediction(String prompt, List<String> imageUrls) {
        ensureConfigured();
        String predictionUrl = createPrediction(prompt, imageUrls);
        return pollUntilComplete(predictionUrl);
    }

    private void ensureConfigured() {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation API is not configured");
        }
    }

    private String createPrediction(String prompt, List<String> imageUrls) {
        Map<String, Object> input = Map.of(
            "prompt", prompt,
            "image_input", imageUrls
        );

        Map<String, Object> body = Map.of("input", input);

        log.debug("Nano Banana 2 request: prompt='{}', images={}", prompt, imageUrls);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri("/models/google/nano-banana-2/predictions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiToken)
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response == null || !response.containsKey("urls")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Replicate API returned unexpected response");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> urls = (Map<String, String>) response.get("urls");
            return urls.get("get");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Replicate prediction creation failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation API request failed");
        }
    }

    @SuppressWarnings("unchecked")
    private String pollUntilComplete(String predictionUrl) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                Map<String, Object> response = restClient.get()
                    .uri(predictionUrl)
                    .header("Authorization", "Bearer " + apiToken)
                    .retrieve()
                    .body(Map.class);

                if (response == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Replicate API returned empty poll response");
                }

                String status = (String) response.get("status");

                if ("succeeded".equals(status)) {
                    Object output = response.get("output");
                    if (output instanceof List<?> outputList && !outputList.isEmpty()) {
                        return (String) outputList.getFirst();
                    }
                    if (output instanceof String outputStr) {
                        return outputStr;
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Replicate prediction succeeded but returned no output");
                }

                if ("failed".equals(status) || "canceled".equals(status)) {
                    String error = (String) response.getOrDefault("error", "Unknown error");
                    log.error("Replicate prediction {}: {}", status, error);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation failed: " + error);
                }

                Thread.sleep(pollIntervalMs);
            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation interrupted");
            } catch (RestClientException ex) {
                log.error("Replicate poll request failed", ex);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation API request failed");
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation timed out");
    }
}
