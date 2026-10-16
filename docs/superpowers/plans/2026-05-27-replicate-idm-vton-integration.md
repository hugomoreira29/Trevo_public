# Replicate IDM-VTON Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the existing virtual try-on backend to Replicate's IDM-VTON model so image generation works end-to-end, and connect the frontend to the real backend endpoints.

**Architecture:** Replace the generic `HttpAiImageGenerationClient` with a Replicate-specific implementation that creates predictions via `POST /v1/predictions` and polls for completion. The `AiImageGenerationClient` interface, `TryOnService`, `TryOnController`, and database schema stay unchanged. The frontend stubs in `ProductDetail.jsx` get replaced with real API calls.

**Tech Stack:** Spring Boot 4, Java 21, RestClient, React 19, Axios

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `trevo-api/src/main/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClient.java` | Replace generic REST client with Replicate-delegating implementation |
| Create | `trevo-api/src/main/java/pt/trevo/trevo_api/service/ReplicateApiClient.java` | Replicate prediction API calls (create + poll) |
| Create | `trevo-api/src/main/java/pt/trevo/trevo_api/config/TryOnConfig.java` | `@ConfigurationProperties` for model photo URL map |
| Modify | `trevo-api/src/main/resources/application.properties` | Add Replicate config properties |
| Modify | `trevo-api/src/main/java/pt/trevo/trevo_api/service/UserPhotoInput.java` | Add `temporaryUrl()` method |
| Modify | `trevo-api/src/main/java/pt/trevo/trevo_api/service/MultipartUserPhotoInput.java` | Implement `temporaryUrl()` via Cloudinary upload |
| Modify | `trevo-api/src/main/java/pt/trevo/trevo_api/controller/TryOnController.java` | Inject `CloudinaryService` for personalized try-on |
| Create | `trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java` | Unit tests for rewritten client |
| Create | `trevo-api/src/test/java/pt/trevo/trevo_api/service/ReplicateApiClientTest.java` | Unit tests for missing token guard |
| Modify | `trevo-web/src/services/tryOnService.js` | Add `generateTryOn` and `generatePersonalizedTryOn` API calls |
| Modify | `trevo-web/src/pages/ProductDetail.jsx` | Replace try-on stubs with real API calls + error handling |

---

### Task 1: Add Replicate config properties

**Files:**
- Modify: `trevo-api/src/main/resources/application.properties`

- [ ] **Step 1: Add Replicate properties to application.properties**

Append the following after the existing `app.ai.image-generation.*` lines (around line 17):

```properties
# Replicate AI
replicate.api-token=${REPLICATE_API_TOKEN:}
replicate.model-version=${REPLICATE_MODEL_VERSION:}
replicate.poll-interval-ms=${REPLICATE_POLL_INTERVAL_MS:2000}
replicate.timeout-ms=${REPLICATE_TIMEOUT_MS:60000}

# Model reference photo URLs (body types)
app.ai.try-on.model-photos.slim=${AI_TRYON_MODEL_PHOTO_SLIM:}
app.ai.try-on.model-photos.athletic=${AI_TRYON_MODEL_PHOTO_ATHLETIC:}
app.ai.try-on.model-photos.regular=${AI_TRYON_MODEL_PHOTO_REGULAR:}
app.ai.try-on.model-photos.plusSize=${AI_TRYON_MODEL_PHOTO_PLUS_SIZE:}
```

- [ ] **Step 2: Commit**

```bash
git add trevo-api/src/main/resources/application.properties
git commit -m "feat: add Replicate IDM-VTON config properties"
```

---

### Task 2: Rewrite HttpAiImageGenerationClient — test create-prediction

**Files:**
- Create: `trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java`
- Modify: `trevo-api/src/main/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClient.java`

- [ ] **Step 1: Write the failing test — successful prediction flow**

Create `trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java`:

```java
package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;

class HttpAiImageGenerationClientTest extends BaseUnitTest {

    @Mock private ReplicateApiClient replicateApiClient;

    private HttpAiImageGenerationClient client;

    @BeforeEach
    void setUp() {
        Map<String, String> modelPhotos = Map.of(
            "slim", "https://example.com/slim.jpg",
            "athletic", "https://example.com/athletic.jpg",
            "regular", "https://example.com/regular.jpg",
            "plusSize", "https://example.com/plus-size.jpg"
        );
        client = new HttpAiImageGenerationClient(replicateApiClient, modelPhotos);
    }

    @Test
    void generateTryOnReturnsImageUrlOnSuccessfulPrediction() {
        Product product = productWithImage("https://cdn.example.com/shirt.jpg");
        when(replicateApiClient.createAndAwaitPrediction(
            "https://cdn.example.com/shirt.jpg",
            "https://example.com/slim.jpg",
            "Linen Shirt"
        )).thenReturn("https://replicate.delivery/result.png");

        String result = client.generateTryOn(product, "slim");

        assertThat(result).isEqualTo("https://replicate.delivery/result.png");
    }

    @Test
    void generateTryOnThrows400WhenProductHasNoImages() {
        Product product = productWithoutImages();

        Throwable thrown = catchThrowable(() -> client.generateTryOn(product, "slim"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ResponseStatusException) thrown).getReason()).isEqualTo("Product has no images");
    }

    @Test
    void generateTryOnThrows400WhenModelIdentifierHasNoPhoto() {
        Product product = productWithImage("https://cdn.example.com/shirt.jpg");

        Throwable thrown = catchThrowable(() -> client.generateTryOn(product, "unknown-type"));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ResponseStatusException) thrown).getReason()).contains("No reference photo configured");
    }

    private Product productWithImage(String imageUrl) {
        ProductImage image = new ProductImage();
        image.setImageUrl(imageUrl);
        Product product = Product.builder()
            .id(1L)
            .name("Linen Shirt")
            .price(new BigDecimal("49.99"))
            .status(ProductStatus.ACTIVE)
            .build();
        product.setImages(List.of(image));
        return product;
    }

    private Product productWithoutImages() {
        Product product = Product.builder()
            .id(2L)
            .name("Empty Product")
            .price(new BigDecimal("10.00"))
            .status(ProductStatus.ACTIVE)
            .build();
        product.setImages(List.of());
        return product;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="HttpAiImageGenerationClientTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL — `ReplicateApiClient` class does not exist yet.

- [ ] **Step 3: Create ReplicateApiClient and rewrite HttpAiImageGenerationClient**

First, create `trevo-api/src/main/java/pt/trevo/trevo_api/service/ReplicateApiClient.java`:

```java
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
    private final String modelVersion;
    private final long pollIntervalMs;
    private final long timeoutMs;
    private final RestClient restClient;

    public ReplicateApiClient(
        @Value("${replicate.api-token:}") String apiToken,
        @Value("${replicate.model-version:}") String modelVersion,
        @Value("${replicate.poll-interval-ms:2000}") long pollIntervalMs,
        @Value("${replicate.timeout-ms:60000}") long timeoutMs
    ) {
        this.apiToken = apiToken;
        this.modelVersion = modelVersion;
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
        this.restClient = RestClient.builder()
            .baseUrl(REPLICATE_API_BASE)
            .build();
    }

    public String createAndAwaitPrediction(String garmentImageUrl, String humanImageUrl, String garmentDescription) {
        ensureConfigured();
        String predictionUrl = createPrediction(garmentImageUrl, humanImageUrl, garmentDescription);
        return pollUntilComplete(predictionUrl);
    }

    private void ensureConfigured() {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI image generation API is not configured");
        }
    }

    private String createPrediction(String garmentImageUrl, String humanImageUrl, String garmentDescription) {
        Map<String, Object> input = Map.of(
            "garm_img", garmentImageUrl,
            "human_img", humanImageUrl,
            "garment_des", garmentDescription,
            "category", "upper_body"
        );

        Map<String, Object> body;
        if (modelVersion != null && !modelVersion.isBlank()) {
            body = Map.of("version", modelVersion, "input", input);
        } else {
            body = Map.of("input", input);
        }

        String uri = (modelVersion != null && !modelVersion.isBlank())
            ? "/predictions"
            : "/models/cuuupid/idm-vton/predictions";

        try {
            Map<String, Object> response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiToken)
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response == null || !response.containsKey("urls")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Replicate API returned unexpected response");
            }

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
```

Then create the config class `trevo-api/src/main/java/pt/trevo/trevo_api/config/TryOnConfig.java`:

```java
package pt.trevo.trevo_api.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai.try-on")
public record TryOnConfig(Map<String, String> modelPhotos) {

    public TryOnConfig {
        if (modelPhotos == null) {
            modelPhotos = Map.of();
        }
    }
}
```

Enable it by adding `@ConfigurationPropertiesScan` to the main application class if not already present, or add `@EnableConfigurationProperties(TryOnConfig.class)` to `HttpAiImageGenerationClient`.

Then rewrite `trevo-api/src/main/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClient.java`:

```java
package pt.trevo.trevo_api.service;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.config.TryOnConfig;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductImage;

@Service
@EnableConfigurationProperties(TryOnConfig.class)
public class HttpAiImageGenerationClient implements AiImageGenerationClient {

    private final ReplicateApiClient replicateApiClient;
    private final Map<String, String> modelPhotos;

    public HttpAiImageGenerationClient(
        ReplicateApiClient replicateApiClient,
        TryOnConfig tryOnConfig
    ) {
        this.replicateApiClient = replicateApiClient;
        this.modelPhotos = tryOnConfig.modelPhotos();
    }

    @Override
    public String generateTryOn(Product product, String modelIdentifier) {
        String garmentImageUrl = firstProductImageUrl(product);
        String humanImageUrl = resolveModelPhoto(modelIdentifier);
        return replicateApiClient.createAndAwaitPrediction(garmentImageUrl, humanImageUrl, product.getName());
    }

    @Override
    public String generatePersonalizedTryOn(Product product, List<UserPhotoInput> photos) {
        String garmentImageUrl = firstProductImageUrl(product);
        String humanImageUrl = photos.getFirst().temporaryUrl();
        return replicateApiClient.createAndAwaitPrediction(garmentImageUrl, humanImageUrl, product.getName());
    }

    private String firstProductImageUrl(Product product) {
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product has no images");
        }
        return images.getFirst().getImageUrl();
    }

    private String resolveModelPhoto(String modelIdentifier) {
        String url = modelPhotos.get(modelIdentifier);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No reference photo configured for model: " + modelIdentifier);
        }
        return url;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="HttpAiImageGenerationClientTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add trevo-api/src/main/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClient.java \
       trevo-api/src/main/java/pt/trevo/trevo_api/service/ReplicateApiClient.java \
       trevo-api/src/main/java/pt/trevo/trevo_api/config/TryOnConfig.java \
       trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java
git commit -m "feat: rewrite HttpAiImageGenerationClient to call Replicate IDM-VTON"
```

---

### Task 3: Test ReplicateApiClient — missing token, timeout, failure

**Files:**
- Create: `trevo-api/src/test/java/pt/trevo/trevo_api/service/ReplicateApiClientTest.java`

- [ ] **Step 1: Write failing tests for error cases**

Create `trevo-api/src/test/java/pt/trevo/trevo_api/service/ReplicateApiClientTest.java`:

```java
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
            client.createAndAwaitPrediction("garment.jpg", "human.jpg", "desc")
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
            client.createAndAwaitPrediction("garment.jpg", "human.jpg", "desc")
        );

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException ex = (ResponseStatusException) thrown;
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getReason()).isEqualTo("AI image generation API is not configured");
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

These tests exercise the `ensureConfigured()` guard which already exists in the implementation from Task 2.

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="ReplicateApiClientTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS (2 tests)

- [ ] **Step 3: Commit**

```bash
git add trevo-api/src/test/java/pt/trevo/trevo_api/service/ReplicateApiClientTest.java
git commit -m "test: add ReplicateApiClient unit tests for missing token"
```

---

### Task 4: Add personalized try-on test and UserPhotoInput.temporaryUrl()

**Files:**
- Modify: `trevo-api/src/main/java/pt/trevo/trevo_api/service/UserPhotoInput.java`
- Modify: `trevo-api/src/main/java/pt/trevo/trevo_api/service/MultipartUserPhotoInput.java`
- Modify: `trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java`

The personalized try-on flow needs Replicate to receive a URL for the user's photo. Since `UserPhotoInput` currently only provides raw bytes, we need to add a `temporaryUrl()` method. For the MVP, `MultipartUserPhotoInput` can upload to Cloudinary via the existing `CloudinaryService` and return the URL.

- [ ] **Step 1: Write failing test for personalized try-on**

Add to `HttpAiImageGenerationClientTest.java`:

```java
@Test
void generatePersonalizedTryOnUsesFirstPhotoUrlAsHumanImage() {
    Product product = productWithImage("https://cdn.example.com/shirt.jpg");
    UserPhotoInput photo1 = () -> "https://cloudinary.example.com/user-photo-1.jpg";
    UserPhotoInput photo2 = () -> "https://cloudinary.example.com/user-photo-2.jpg";
    UserPhotoInput photo3 = () -> "https://cloudinary.example.com/user-photo-3.jpg";
    when(replicateApiClient.createAndAwaitPrediction(
        "https://cdn.example.com/shirt.jpg",
        "https://cloudinary.example.com/user-photo-1.jpg",
        "Linen Shirt"
    )).thenReturn("https://replicate.delivery/personalized.png");

    String result = client.generatePersonalizedTryOn(product, List.of(photo1, photo2, photo3));

    assertThat(result).isEqualTo("https://replicate.delivery/personalized.png");
}
```

This test won't compile yet because `UserPhotoInput` doesn't have a `temporaryUrl()` method, and the mock lambdas won't match the interface.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="HttpAiImageGenerationClientTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL — compilation error, `temporaryUrl()` not defined on `UserPhotoInput`.

- [ ] **Step 3: Add temporaryUrl() to UserPhotoInput**

Add the method to `UserPhotoInput.java`:

```java
package pt.trevo.trevo_api.service;

public interface UserPhotoInput {

    String filename();

    String contentType();

    long size();

    byte[] bytes();

    String temporaryUrl();
}
```

Update `MultipartUserPhotoInput.java` to implement it by uploading to Cloudinary:

```java
package pt.trevo.trevo_api.service;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

public class MultipartUserPhotoInput implements UserPhotoInput {

    private final MultipartFile file;
    private final CloudinaryService cloudinaryService;
    private String cachedUrl;

    public MultipartUserPhotoInput(MultipartFile file) {
        this(file, null);
    }

    public MultipartUserPhotoInput(MultipartFile file, CloudinaryService cloudinaryService) {
        this.file = file;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public String filename() {
        return file.getOriginalFilename();
    }

    @Override
    public String contentType() {
        return file.getContentType();
    }

    @Override
    public long size() {
        return file.getSize();
    }

    @Override
    public byte[] bytes() {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read photo upload", exception);
        }
    }

    @Override
    public String temporaryUrl() {
        if (cachedUrl != null) {
            return cachedUrl;
        }
        if (cloudinaryService == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "CloudinaryService is required for temporary URL generation");
        }
        CloudinaryUploadResult result = cloudinaryService.uploadImage(bytes(), filename(), contentType());
        cachedUrl = result.imageUrl();
        return cachedUrl;
    }
}
```

Update test mock lambdas — since `UserPhotoInput` now has 5 methods, use an anonymous class or a simple test helper instead:

Replace the personalized test's photo mocks with:

```java
@Test
void generatePersonalizedTryOnUsesFirstPhotoUrlAsHumanImage() {
    Product product = productWithImage("https://cdn.example.com/shirt.jpg");
    UserPhotoInput photo1 = testPhoto("https://cloudinary.example.com/user-photo-1.jpg");
    UserPhotoInput photo2 = testPhoto("https://cloudinary.example.com/user-photo-2.jpg");
    UserPhotoInput photo3 = testPhoto("https://cloudinary.example.com/user-photo-3.jpg");
    when(replicateApiClient.createAndAwaitPrediction(
        "https://cdn.example.com/shirt.jpg",
        "https://cloudinary.example.com/user-photo-1.jpg",
        "Linen Shirt"
    )).thenReturn("https://replicate.delivery/personalized.png");

    String result = client.generatePersonalizedTryOn(product, List.of(photo1, photo2, photo3));

    assertThat(result).isEqualTo("https://replicate.delivery/personalized.png");
}

private UserPhotoInput testPhoto(String temporaryUrl) {
    return new UserPhotoInput() {
        @Override public String filename() { return "photo.jpg"; }
        @Override public String contentType() { return "image/jpeg"; }
        @Override public long size() { return 100; }
        @Override public byte[] bytes() { return new byte[] {1, 2, 3}; }
        @Override public String temporaryUrl() { return temporaryUrl; }
    };
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="HttpAiImageGenerationClientTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS (4 tests)

- [ ] **Step 5: Fix TryOnController to pass CloudinaryService to MultipartUserPhotoInput**

Update `TryOnController.java` — add `CloudinaryService` to the constructor and use it when creating photo inputs. The full updated class:

```java
package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pt.trevo.trevo_api.dto.TryOnImageResponse;
import pt.trevo.trevo_api.dto.TryOnRequest;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CloudinaryService;
import pt.trevo.trevo_api.service.MultipartUserPhotoInput;
import pt.trevo.trevo_api.service.TryOnService;
import pt.trevo.trevo_api.service.UserPhotoInput;

@RestController
@RequestMapping("/api/buyer/try-on")
@PreAuthorize("hasRole('BUYER')")
public class TryOnController {

    private static final int PERSONALIZED_PHOTO_COUNT = 3;

    private final TryOnService tryOnService;
    private final ImageUploadValidator imageUploadValidator;
    private final SecurityUtils securityUtils;
    private final CloudinaryService cloudinaryService;

    public TryOnController(
        TryOnService tryOnService,
        ImageUploadValidator imageUploadValidator,
        SecurityUtils securityUtils,
        CloudinaryService cloudinaryService
    ) {
        this.tryOnService = tryOnService;
        this.imageUploadValidator = imageUploadValidator;
        this.securityUtils = securityUtils;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public TryOnImageResponse generateTryOn(
        Authentication authentication,
        @Valid @RequestBody TryOnRequest request
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return tryOnService.generateTryOn(buyerId, request.productId(), request.modelIdentifier());
    }

    @PostMapping(path = "/personalized", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TryOnImageResponse generatePersonalizedTryOn(
        Authentication authentication,
        @RequestParam Long productId,
        @RequestParam("photos") List<MultipartFile> photos
    ) {
        if (photos == null || photos.size() != PERSONALIZED_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 photos are required");
        }
        photos.forEach(photo -> imageUploadValidator.validateOptionalImage(photo, "photos"));
        List<UserPhotoInput> photoInputs = photos.stream()
            .map(photo -> new MultipartUserPhotoInput(photo, cloudinaryService))
            .map(UserPhotoInput.class::cast)
            .toList();
        Long buyerId = securityUtils.requireUserId(authentication);
        return tryOnService.generatePersonalizedTryOn(buyerId, productId, photoInputs);
    }
}
```

- [ ] **Step 6: Run all TryOn-related tests**

Run: `cd trevo-api && ./mvnw test -pl . -Dtest="HttpAiImageGenerationClientTest,TryOnServiceTest,TryOnControllerTest" -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add trevo-api/src/main/java/pt/trevo/trevo_api/service/UserPhotoInput.java \
       trevo-api/src/main/java/pt/trevo/trevo_api/service/MultipartUserPhotoInput.java \
       trevo-api/src/main/java/pt/trevo/trevo_api/controller/TryOnController.java \
       trevo-api/src/test/java/pt/trevo/trevo_api/service/HttpAiImageGenerationClientTest.java
git commit -m "feat: add temporaryUrl() to UserPhotoInput for personalized try-on"
```

---

### Task 5: Remove old config properties

**Files:**
- Modify: `trevo-api/src/main/resources/application.properties`

The old `app.ai.image-generation.*` properties are no longer used. Remove them.

- [ ] **Step 1: Remove old properties**

Remove these three lines from `application.properties`:

```properties
app.ai.image-generation.base-url=${AI_IMAGE_GENERATION_BASE_URL:}
app.ai.image-generation.try-on-path=${AI_IMAGE_GENERATION_TRY_ON_PATH:/try-on}
app.ai.image-generation.personalized-try-on-path=${AI_IMAGE_GENERATION_PERSONALIZED_TRY_ON_PATH:/try-on/personalized}
```

- [ ] **Step 2: Run full backend test suite**

Run: `cd trevo-api && ./mvnw test`

Expected: PASS — no code references these properties anymore.

- [ ] **Step 3: Commit**

```bash
git add trevo-api/src/main/resources/application.properties
git commit -m "chore: remove unused AI image generation config properties"
```

---

### Task 6: Wire frontend — tryOnService.js

**Files:**
- Modify: `trevo-web/src/services/tryOnService.js`

- [ ] **Step 1: Add generateTryOn and generatePersonalizedTryOn functions**

Replace the contents of `trevo-web/src/services/tryOnService.js`:

```js
import apiClient from "../lib/apiClient";

export async function getTryOnUsage(productId) {
  const { data } = await apiClient.get(`/buyer/try-on/usage/${productId}`);
  return data;
}

export async function generateTryOn(productId, modelIdentifier) {
  const { data } = await apiClient.post("/buyer/try-on", {
    productId,
    modelIdentifier,
  });
  return data;
}

export async function generatePersonalizedTryOn(productId, photos) {
  const formData = new FormData();
  formData.append("productId", productId);
  photos.forEach((file) => {
    formData.append("photos", file);
  });

  const { data } = await apiClient.post("/buyer/try-on/personalized", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}
```

- [ ] **Step 2: Commit**

```bash
git add trevo-web/src/services/tryOnService.js
git commit -m "feat: add generateTryOn and generatePersonalizedTryOn to tryOnService"
```

---

### Task 7: Wire frontend — ProductDetail.jsx standard try-on

**Files:**
- Modify: `trevo-web/src/pages/ProductDetail.jsx`

- [ ] **Step 1: Add import for generateTryOn**

Update the import from `tryOnService` (line 11) to also include `generateTryOn`:

```js
import { generateTryOn, getTryOnUsage } from "../services/tryOnService";
```

- [ ] **Step 2: Add tryOnError state**

Near line 89 (after the `generatedTryOnImage` state), add:

```js
const [tryOnError, setTryOnError] = useState("");
```

- [ ] **Step 3: Replace handleGenerateTryOn stub with real API call**

Replace the existing `handleGenerateTryOn` function (lines 236-246) with:

```js
  async function handleGenerateTryOn() {
    setGeneratedTryOnImage("");
    setTryOnError("");
    setIsGeneratingTryOn(true);

    try {
      const result = await generateTryOn(Number(productId), selectedBodyType);
      setGeneratedTryOnImage(result.imageUrl);

      if (isPro === false && !tryOnUsed) {
        setTryOnUsed(true);
      }
    } catch (err) {
      setTryOnError(getErrorMessage(err, t("tryOnFailed")));
    } finally {
      setIsGeneratingTryOn(false);
    }
  }
```

- [ ] **Step 4: Add error display in the try-on modal**

After the generate button (after line 615 — the closing `</button>`), add an error message block:

```jsx
            {tryOnError && (
              <p className="mt-3 text-sm text-red-600" role="alert">
                {tryOnError}
              </p>
            )}
```

- [ ] **Step 5: Add tryOnFailed translation key**

In `trevo-web/src/i18n/locales/pt-PT/browse.json`, add:

```json
"tryOnFailed": "Falha ao gerar imagem. Tenta novamente."
```

- [ ] **Step 6: Commit**

```bash
git add trevo-web/src/pages/ProductDetail.jsx \
       trevo-web/src/i18n/locales/pt-PT/browse.json
git commit -m "feat: wire standard try-on to real backend API"
```

---

### Task 8: Wire frontend — ProductDetail.jsx personalized try-on ("See it on yourself")

**Files:**
- Modify: `trevo-web/src/pages/ProductDetail.jsx`

- [ ] **Step 1: Add import for generatePersonalizedTryOn**

Update the import from `tryOnService`:

```js
import { generatePersonalizedTryOn, generateTryOn, getTryOnUsage } from "../services/tryOnService";
```

- [ ] **Step 2: Replace handleSeeItSubmit with real API call**

Replace the existing `handleSeeItSubmit` function (lines 296-318) with:

```js
  async function handleSeeItSubmit(event) {
    event.preventDefault();
    setSeeItError("");
    setSeeItSuccess("");
    setSeeItResultImage("");

    if (seeItPhotos.length !== REQUIRED_TRY_ON_PHOTOS) {
      setSeeItError("Seleciona exatamente 3 imagens.");
      return;
    }

    setIsUploadingSeeIt(true);

    try {
      const result = await generatePersonalizedTryOn(
        Number(productId),
        seeItPhotos.map((photo) => photo.file)
      );
      setSeeItResultImage(result.imageUrl);
      setSeeItSuccess(t("tryOnGenerated"));
    } catch (uploadError) {
      setSeeItError(getErrorMessage(uploadError, t("tryOnFailed")));
    } finally {
      setIsUploadingSeeIt(false);
    }
  }
```

- [ ] **Step 3: Add tryOnGenerated translation key**

In `trevo-web/src/i18n/locales/pt-PT/browse.json`, add:

```json
"tryOnGenerated": "Imagem gerada com sucesso!"
```

- [ ] **Step 4: Remove unused uploadBuyerPhotos import**

If `uploadBuyerPhotos` is no longer used anywhere in `ProductDetail.jsx`, remove the import:

```js
import { uploadBuyerPhotos } from "../services/userPhotoService";
```

Check first: `grep -n "uploadBuyerPhotos" trevo-web/src/pages/ProductDetail.jsx`. If only the import line matches, remove it.

- [ ] **Step 5: Commit**

```bash
git add trevo-web/src/pages/ProductDetail.jsx \
       trevo-web/src/i18n/locales/pt-PT/browse.json
git commit -m "feat: wire personalized try-on to real backend API"
```

---

### Task 9: Verify full backend test suite passes

**Files:** None (verification only)

- [ ] **Step 1: Run full backend test suite**

Run: `cd trevo-api && ./mvnw test`

Expected: All tests PASS. If any existing integration tests fail because they can't autowire the new `ReplicateApiClient` bean (due to missing token), add a `@MockBean ReplicateApiClient replicateApiClient` to the failing test class or to a shared test config.

- [ ] **Step 2: If tests fail, fix and commit**

Common fix: if `@SpringBootTest` integration tests fail because `ReplicateApiClient` tries to create a `RestClient` with a real URL but has no token, add `@MockitoBean` for `ReplicateApiClient` in the base integration test class or individual failing tests.

```bash
git add -A
git commit -m "fix: add mock beans for ReplicateApiClient in integration tests"
```

---

### Task 10: Final verification and cleanup

**Files:** None (verification only)

- [ ] **Step 1: Run frontend dev server and verify try-on modal**

Run: `cd trevo-web && npm run dev`

Open a product detail page in the browser. Click "Virtual Try-On":
- Verify the modal opens with body type selection
- Without `REPLICATE_API_TOKEN` set, clicking generate should show an error message (502 from backend)
- Verify no console errors in the browser

- [ ] **Step 2: Run frontend linter/tests if available**

Run: `cd trevo-web && npm test` (if tests exist)

- [ ] **Step 3: Verify git status is clean**

Run: `git status`

Expected: No uncommitted changes.
