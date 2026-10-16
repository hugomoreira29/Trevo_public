package pt.trevo.trevo_api.perf;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.dto.CreateProductRequest;
import pt.trevo.trevo_api.dto.UpdateProductRequest;
import pt.trevo.trevo_api.persistence.entity.GarmentType;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.persistence.repository.VariantRepository;

@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrudLatencyTest {

    private static final int WARMUP     = 10;
    private static final int ITERATIONS = 100;
    private static final long P95_MS    = 300;

    // Container lifecycle (start/stop) is managed by the Testcontainers JUnit extension.
    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("trevo_perf_crud")
        .withUsername("trevo")
        .withPassword("trevo");

    @Autowired private TestRestTemplate  restTemplate;
    @Autowired private JwtService        jwtService;
    @Autowired private ObjectMapper      objectMapper;
    @Autowired private UserRepository    userRepository;
    @Autowired private ShopRepository    shopRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VariantRepository variantRepository;

    private HttpHeaders ownerHeaders;
    private Long        shopId;
    private Long        productId;
    private String      createBody;
    private String      updateBody;

    // Pre-created variant IDs consumed one-per-iteration by the DELETE test.
    private final List<Long>    variantIds   = new ArrayList<>();
    private final AtomicInteger variantIndex = new AtomicInteger(0);

    // No @AfterAll cleanup — the Testcontainers container is destroyed after the class,
    // taking the entire database with it.

    @BeforeAll
    void setUp() throws Exception {
        User owner = userRepository.save(User.builder()
            .email("perf.crud.owner@test.com")
            .passwordHash("hash")
            .fullName("Perf Crud Owner")
            .role(Role.SHOP_OWNER)
            .status(Status.ACTIVE)
            .build());

        Shop shop = shopRepository.save(Shop.builder()
            .owner(owner)
            .name("Perf Crud Shop")
            .nif("900000098")
            .status(ShopStatus.ACTIVE)
            .build());
        shopId = shop.getId();

        Product product = productRepository.save(Product.builder()
            .shop(shop)
            .name("Perf Product")
            .description("Performance test product")
            .price(new BigDecimal("29.99"))
            .brand("PerfBrand")
            .status(ProductStatus.ACTIVE)
            .build());
        productId = product.getId();

        ownerHeaders = new HttpHeaders();
        ownerHeaders.setBearerAuth(jwtService.generateToken(owner));
        ownerHeaders.setContentType(MediaType.APPLICATION_JSON);

        // Use Spring-configured ObjectMapper so any registered modules (JavaTimeModule, etc.)
        // are always in sync with what the application expects.
        createBody = objectMapper.writeValueAsString(
            new CreateProductRequest("LatencyProduct", "Test", new BigDecimal("19.99"), "Brand", null, GarmentType.UPPER_BODY));
        updateBody = objectMapper.writeValueAsString(
            new UpdateProductRequest("UpdatedName", null, null, null, null, null));

        // Pre-create exactly WARMUP + ITERATIONS unique variants for the DELETE test so
        // each iteration has a fresh variant to delete.
        for (int i = 0; i < WARMUP + ITERATIONS; i++) {
            Variant v = variantRepository.save(Variant.builder()
                .product(product)
                .size("S")
                .color("DelColor" + i)
                .stock(1)
                .build());
            variantIds.add(v.getId());
        }
    }

    @Test
    void getProduct_p95Under300ms() {
        String url = "/api/products/" + productId;
        assertP95("GET " + url,
            () -> restTemplate.getForEntity(url, String.class));
    }

    @Test
    void searchProducts_p95Under300ms() {
        assertP95("GET /api/products/search",
            () -> restTemplate.getForEntity("/api/products/search?q=perf", String.class));
    }

    @Test
    void browseByShop_p95Under300ms() {
        String url = "/api/products?shopId=" + shopId;
        assertP95("GET " + url,
            () -> restTemplate.getForEntity(url, String.class));
    }

    @Test
    void createProduct_p95Under300ms() {
        // Each iteration commits a new product row; rows accumulate (up to 110 including warmup)
        // but the Testcontainers container is destroyed after the class so no cleanup is needed.
        HttpEntity<String> entity = new HttpEntity<>(createBody, ownerHeaders);
        assertP95("POST /api/products",
            () -> restTemplate.postForEntity("/api/products", entity, String.class));
    }

    @Test
    void updateProduct_p95Under300ms() {
        String url = "/api/products/" + productId;
        HttpEntity<String> entity = new HttpEntity<>(updateBody, ownerHeaders);
        assertP95("PUT " + url,
            () -> restTemplate.exchange(url, HttpMethod.PUT, entity, String.class));
    }

    @Test
    void deleteVariant_p95Under300ms() {
        String base = "/api/products/" + productId + "/variants/";
        assertP95("DELETE " + base + "{variantId}", () -> {
            String url = base + variantIds.get(variantIndex.getAndIncrement());
            return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(ownerHeaders), Void.class);
        });
    }

    private void assertP95(String label, Supplier<ResponseEntity<?>> action) {
        // Warmup also validates that each endpoint returns a 2xx status.
        // If an endpoint is broken, the test fails here rather than silently benchmarking errors.
        for (int i = 0; i < WARMUP; i++) {
            ResponseEntity<?> response = action.get();
            assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Warmup [%s] returned unexpected status %s", label, response.getStatusCode())
                .isTrue();
        }

        long[] durations = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            action.get();
            durations[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }

        long p95 = PerfTestUtils.p95(durations);
        assertThat(p95)
            .as("[%s] p95 was %dms, threshold %dms", label, p95, P95_MS)
            .isLessThan(P95_MS);
    }
}
