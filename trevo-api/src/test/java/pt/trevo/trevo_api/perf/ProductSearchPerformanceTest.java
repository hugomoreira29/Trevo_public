package pt.trevo.trevo_api.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.ShopStatus;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ProductRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.service.ProductService;

@Tag("performance")
@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductSearchPerformanceTest {

    private static final int PRODUCT_COUNT = 10_000;
    private static final int WARMUP        = 10;
    private static final int ITERATIONS    = 100;
    private static final long P95_MS       = 500;

    // Container lifecycle (start/stop) is managed by the Testcontainers JUnit extension.
    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("trevo_perf_search")
        .withUsername("trevo")
        .withPassword("trevo");

    @Autowired private ProductService    productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository    shopRepository;
    @Autowired private UserRepository    userRepository;

    private Long shopId;

    @BeforeAll
    void seedTenThousandProducts() {
        User owner = userRepository.save(User.builder()
            .email("perf.search.owner@test.com")
            .passwordHash("hash")
            .fullName("Perf Search Owner")
            .role(Role.SHOP_OWNER)
            .status(Status.ACTIVE)
            .build());

        Shop shop = shopRepository.save(Shop.builder()
            .owner(owner)
            .name("Perf Search Shop")
            .nif("900000099")
            .status(ShopStatus.ACTIVE)
            .build());
        shopId = shop.getId();

        List<Product> batch = new ArrayList<>(500);
        for (int i = 0; i < PRODUCT_COUNT; i++) {
            batch.add(Product.builder()
                .shop(shop)
                .name("Product " + i + (i % 100 == 0 ? " dress" : ""))
                .description("Description for item " + i + (i % 200 == 0 ? " elegant" : ""))
                .price(BigDecimal.valueOf(10 + (i % 490)))
                .brand("Brand" + (i % 20))
                .status(ProductStatus.ACTIVE)
                .build());
            if (batch.size() == 500) {
                productRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            productRepository.saveAll(batch);
        }
    }

    // No @AfterAll cleanup — the Testcontainers container is destroyed after the class,
    // taking the entire database with it.

    @Test
    void keywordSearch_p95Under500ms() {
        for (int i = 0; i < WARMUP; i++) {
            productService.searchProducts("dress", null, null, null, null, PageRequest.of(0, 20));
        }

        long[] durations = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            productService.searchProducts("dress", null, null, null, null, PageRequest.of(0, 20));
            durations[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }

        long p95 = p95(durations);
        assertThat(p95)
            .as("keyword search p95 was %dms, threshold %dms", p95, P95_MS)
            .isLessThan(P95_MS);
    }

    @Test
    void priceRangeSearch_p95Under500ms() {
        BigDecimal min = new BigDecimal("20.00");
        BigDecimal max = new BigDecimal("100.00");

        for (int i = 0; i < WARMUP; i++) {
            productService.searchProducts(null, min, max, null, null, PageRequest.of(0, 20));
        }

        long[] durations = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            productService.searchProducts(null, min, max, null, null, PageRequest.of(0, 20));
            durations[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }

        long p95 = p95(durations);
        assertThat(p95)
            .as("price range search p95 was %dms, threshold %dms", p95, P95_MS)
            .isLessThan(P95_MS);
    }

    @Test
    void combinedKeywordAndPriceSearch_p95Under500ms() {
        BigDecimal min = new BigDecimal("20.00");
        BigDecimal max = new BigDecimal("200.00");

        for (int i = 0; i < WARMUP; i++) {
            productService.searchProducts("dress", min, max, null, null, PageRequest.of(0, 20));
        }

        long[] durations = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            productService.searchProducts("dress", min, max, null, null, PageRequest.of(0, 20));
            durations[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }

        long p95 = p95(durations);
        assertThat(p95)
            .as("combined keyword+price search p95 was %dms, threshold %dms", p95, P95_MS)
            .isLessThan(P95_MS);
    }

    private static long p95(long[] durations) {
        long[] sorted = Arrays.copyOf(durations, durations.length);
        Arrays.sort(sorted);
        return sorted[(int)(sorted.length * 0.95)];
    }
}
