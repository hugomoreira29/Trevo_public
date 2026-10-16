package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.controller.ProductController;
import pt.trevo.trevo_api.dto.AssignCollectionsRequest;
import pt.trevo.trevo_api.dto.CreateProductRequest;
import pt.trevo.trevo_api.dto.UpdateProductRequest;
import pt.trevo.trevo_api.dto.UpdateStockRequest;
import pt.trevo.trevo_api.dto.UpsertVariantRequest;
import pt.trevo.trevo_api.persistence.entity.GarmentType;
import pt.trevo.trevo_api.persistence.entity.Product;
import pt.trevo.trevo_api.persistence.entity.ProductStatus;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.Variant;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class ProductControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ProductService productService;
    @MockitoBean private VariantService variantService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ShopRepository shopRepository;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Search ───────────────────────────────────────────────────────────────

    @Test
    void searchProducts_returnsListWithCorrectDtoFields() throws Exception {
        Product product = activeProduct(1L, 10L, "Red Dress", "50.00");
        Variant variant = variant(1L, product, "M", "Red", 5);

        when(productService.searchProducts(eq("dress"), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product)));
        when(variantService.findByProductIds(any())).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/products/search").param("q", "dress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].shopId").value(10))
            .andExpect(jsonPath("$.content[0].name").value("Red Dress"))
            .andExpect(jsonPath("$.content[0].price").value(50.00))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].variants[0].size").value("M"))
            .andExpect(jsonPath("$.content[0].variants[0].color").value("Red"))
            .andExpect(jsonPath("$.content[0].variants[0].stock").value(5));
    }

    @Test
    void searchProducts_isAccessibleWithoutAuthentication() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products/search"))
            .andExpect(status().isOk());
    }

    @Test
    void searchProducts_withAllFilters_passesThemToService() throws Exception {
        when(productService.searchProducts(eq("shirt"), eq(new BigDecimal("10.00")), eq(new BigDecimal("50.00")), eq("M"), eq("White"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products/search")
                .param("q", "shirt")
                .param("minPrice", "10.00")
                .param("maxPrice", "50.00")
                .param("size", "M")
                .param("color", "White"))
            .andExpect(status().isOk());

        verify(productService).searchProducts(eq("shirt"), eq(new BigDecimal("10.00")), eq(new BigDecimal("50.00")), eq("M"), eq("White"), any(Pageable.class));
    }

    // ── Browse ───────────────────────────────────────────────────────────────

    @Test
    void browseByShop_returnsPaginatedResults() throws Exception {
        Product product = activeProduct(1L, 10L, "Red Dress", "50.00");
        PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productService.findActiveByShop(eq(10L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("shopId", "10").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Red Dress"))
            .andExpect(jsonPath("$.content[0].shopId").value(10));
    }

    @Test
    void browseByShop_sortsByName() throws Exception {
        Product p1 = activeProduct(1L, 10L, "Alpha Shirt", "20.00");
        Product p2 = activeProduct(2L, 10L, "Zeta Coat", "80.00");
        PageRequest sortedByName = PageRequest.of(0, 10, Sort.by("name").ascending());
        PageImpl<Product> page = new PageImpl<>(List.of(p1, p2), sortedByName, 2);

        when(productService.findActiveByShop(eq(10L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products")
                .param("shopId", "10")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Alpha Shirt"))
            .andExpect(jsonPath("$.content[1].name").value("Zeta Coat"));
    }

    @Test
    void browseByShop_sortsByPrice() throws Exception {
        Product cheap = activeProduct(1L, 10L, "Cheap Item", "10.00");
        Product expensive = activeProduct(2L, 10L, "Expensive Item", "200.00");
        PageRequest sortedByPrice = PageRequest.of(0, 10, Sort.by("price").ascending());
        PageImpl<Product> page = new PageImpl<>(List.of(cheap, expensive), sortedByPrice, 2);

        when(productService.findActiveByShop(eq(10L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products")
                .param("shopId", "10")
                .param("sort", "price,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].price").value(10.00))
            .andExpect(jsonPath("$.content[1].price").value(200.00));
    }

    @Test
    void browseByShop_sortsByDate() throws Exception {
        LocalDateTime older = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime newer = LocalDateTime.of(2025, 1, 1, 0, 0);
        Product oldProduct = activeProductWithDate(1L, 10L, "Old Product", "30.00", older);
        Product newProduct = activeProductWithDate(2L, 10L, "New Product", "30.00", newer);
        PageRequest sortedByDate = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        PageImpl<Product> page = new PageImpl<>(List.of(newProduct, oldProduct), sortedByDate, 2);

        when(productService.findActiveByShop(eq(10L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products")
                .param("shopId", "10")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("New Product"))
            .andExpect(jsonPath("$.content[1].name").value("Old Product"));
    }

    @Test
    void browseByCollection_returnsPaginatedResults() throws Exception {
        Product product = activeProduct(1L, 10L, "Summer Dress", "60.00");
        PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productService.findActiveByCollection(eq(5L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("collectionId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Summer Dress"));
    }

    @Test
    void browse_returnsBadRequestWhenNeitherShopIdNorCollectionId() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void browse_isAccessibleWithoutAuthentication() throws Exception {
        Product product = activeProduct(1L, 10L, "Any Product", "25.00");
        PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productService.findActiveByShop(eq(10L), any(Pageable.class))).thenReturn(page);
        when(variantService.findByProductIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("shopId", "10"))
            .andExpect(status().isOk());
    }

    // ── Single product GET ────────────────────────────────────────────────────

    @Test
    void getProduct_returnsProductWithVariants() throws Exception {
        Product product = activeProduct(7L, 10L, "Blue Jeans", "35.00");
        Variant variant = variant(3L, product, "L", "Blue", 8);

        when(productService.findProductById(7L)).thenReturn(product);
        when(variantService.findByProductId(7L)).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/products/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.name").value("Blue Jeans"))
            .andExpect(jsonPath("$.shopId").value(10))
            .andExpect(jsonPath("$.variants[0].id").value(3))
            .andExpect(jsonPath("$.variants[0].size").value("L"))
            .andExpect(jsonPath("$.variants[0].color").value("Blue"))
            .andExpect(jsonPath("$.variants[0].stock").value(8));
    }

    @Test
    void getProduct_returnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.findProductById(999L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getProduct_isAccessibleWithoutAuthentication() throws Exception {
        Product product = activeProduct(1L, 10L, "Public Product", "20.00");

        when(productService.findProductById(1L)).thenReturn(product);
        when(variantService.findByProductId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk());
    }

    // ── Create product ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void createProduct_returnsCreatedProductWhenAuthorized() throws Exception {
        CreateProductRequest request = new CreateProductRequest("New Coat", "Warm coat", new BigDecimal("89.99"), "Brand", "Wool", GarmentType.UPPER_BODY);
        Product saved = activeProduct(5L, 10L, "New Coat", "89.99");

        when(productService.createProduct(eq(11L), any(CreateProductRequest.class))).thenReturn(saved);
        when(variantService.findByProductId(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("New Coat"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "22", roles = "BUYER")
    void createProduct_returnsForbiddenForBuyer() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Coat", null, new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_returnsUnauthorizedWithoutAuthentication() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Coat", null, new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // ── Update product ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateProduct_returnsUpdatedProduct() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest("Updated Name", null, null, null, null, null);
        Product updated = activeProduct(3L, 10L, "Updated Name", "50.00");

        when(productService.updateProduct(eq(11L), eq(3L), any(UpdateProductRequest.class))).thenReturn(updated);
        when(variantService.findByProductId(3L)).thenReturn(List.of());

        mockMvc.perform(put("/api/products/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "99", roles = "SHOP_OWNER")
    void updateProduct_returnsForbiddenWhenCallerIsNotOwner() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest("Name", null, null, null, null, null);

        when(productService.updateProduct(eq(99L), eq(3L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only shop owner can manage this product"));

        mockMvc.perform(put("/api/products/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ── Archive product ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void archiveProduct_returnsArchivedStatus() throws Exception {
        Product archived = Product.builder()
            .id(3L)
            .shop(Shop.builder().id(10L).owner(User.builder().id(11L).build()).build())
            .name("Archived Product")
            .price(new BigDecimal("30.00"))
            .status(ProductStatus.ARCHIVED)
            .build();

        when(productService.archiveProduct(eq(11L), eq(3L))).thenReturn(archived);
        when(variantService.findByProductId(3L)).thenReturn(List.of());

        mockMvc.perform(post("/api/products/3/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    // ── Assign collections ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void assignToCollections_returnsProductWithCollectionsAssigned() throws Exception {
        AssignCollectionsRequest request = new AssignCollectionsRequest(List.of(1L, 2L));
        Product product = activeProduct(3L, 10L, "Dress", "50.00");

        when(productService.assignToCollections(eq(11L), eq(3L), eq(List.of(1L, 2L)))).thenReturn(product);
        when(variantService.findByProductId(3L)).thenReturn(List.of());

        mockMvc.perform(put("/api/products/3/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3));
    }

    // ── Add variant ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void addVariant_returnsCreatedVariant() throws Exception {
        UpsertVariantRequest request = new UpsertVariantRequest("S", "Black", 10);
        Product product = activeProduct(3L, 10L, "Dress", "50.00");
        Variant saved = variant(7L, product, "S", "Black", 10);

        when(variantService.addVariant(eq(11L), eq(3L), any(UpsertVariantRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/products/3/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.productId").value(3))
            .andExpect(jsonPath("$.size").value("S"))
            .andExpect(jsonPath("$.color").value("Black"))
            .andExpect(jsonPath("$.stock").value(10));
    }

    // ── Update variant ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateVariant_returnsUpdatedVariant() throws Exception {
        UpsertVariantRequest request = new UpsertVariantRequest("M", "Black", 5);
        Product product = activeProduct(3L, 10L, "Dress", "50.00");
        Variant updated = variant(7L, product, "M", "Black", 5);

        when(variantService.updateVariant(eq(11L), eq(3L), eq(7L), any(UpsertVariantRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/products/3/variants/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value("M"))
            .andExpect(jsonPath("$.stock").value(5));
    }

    // ── Delete variant ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void removeVariant_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/3/variants/7"))
            .andExpect(status().isNoContent());

        verify(variantService).removeVariant(11L, 3L, 7L);
    }

    // ── Update stock ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "11", roles = "SHOP_OWNER")
    void updateStock_returnsUpdatedVariant() throws Exception {
        UpdateStockRequest request = new UpdateStockRequest(20);
        Product product = activeProduct(3L, 10L, "Dress", "50.00");
        Variant updated = variant(7L, product, "M", "Red", 20);

        when(variantService.updateStock(eq(11L), eq(7L), eq(20))).thenReturn(updated);

        mockMvc.perform(patch("/api/products/3/variants/7/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stock").value(20));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Product activeProduct(Long id, Long shopId, String name, String price) {
        return activeProductWithDate(id, shopId, name, price, null);
    }

    private Product activeProductWithDate(Long id, Long shopId, String name, String price, LocalDateTime createdAt) {
        return Product.builder()
            .id(id)
            .shop(Shop.builder().id(shopId).owner(User.builder().id(11L).build()).build())
            .name(name)
            .price(new BigDecimal(price))
            .status(ProductStatus.ACTIVE)
            .createdAt(createdAt)
            .build();
    }

    private Variant variant(Long id, Product product, String size, String color, Integer stock) {
        return Variant.builder()
            .id(id)
            .product(product)
            .size(size)
            .color(color)
            .stock(stock)
            .build();
    }
}
