package pt.trevo.trevo_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import pt.trevo.trevo_api.dto.CreateReviewRequest;
import pt.trevo.trevo_api.dto.RespondToReviewRequest;
import pt.trevo.trevo_api.dto.ReviewPageResponse;
import pt.trevo.trevo_api.dto.ReviewResponse;
import pt.trevo.trevo_api.dto.UpdateReviewRequest;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.ReviewService;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class ReviewControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ReviewResponse sampleReview(Long id, String shopOwnerResponse) {
        return new ReviewResponse(
            id, 1L, "João Silva", 5L,
            4, "Great shop!", shopOwnerResponse,
            Instant.parse("2025-06-01T10:00:00Z"), Instant.parse("2025-06-01T10:00:00Z")
        );
    }

    private ReviewPageResponse samplePage(List<ReviewResponse> reviews, Double avg) {
        return new ReviewPageResponse(avg, reviews.size(), 1, 0, 20, reviews);
    }

    // --- POST /api/buyer/reviews/shops/{shopId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void createReviewReturnsCreated() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(4, "Great shop!");
        ReviewResponse review = sampleReview(10L, null);

        when(reviewService.createReview(eq(1L), eq(5L), any(CreateReviewRequest.class)))
            .thenReturn(review);

        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.buyerName").value("João Silva"))
            .andExpect(jsonPath("$.response").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void createReviewReturnsForbiddenWhenNoDeliveredOrder() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(4, "Great!");
        when(reviewService.createReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer has no delivered order from this shop"));

        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void createReviewReturnsConflictForDuplicate() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(4, "Again!");
        when(reviewService.createReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this buyer and shop"));

        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void createReviewReturnsBadRequestForBlockedWord() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(1, "badword");
        when(reviewService.createReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review contains blocked words"));

        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReviewRequiresAuthentication() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(4, "Great!");
        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void createReviewForbidsShopOwner() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(4, "Great!");
        mockMvc.perform(post("/api/buyer/reviews/shops/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // --- PUT /api/buyer/reviews/{reviewId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateReviewReturnsUpdatedReview() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Even better!");
        ReviewResponse updated = sampleReview(10L, null);

        when(reviewService.updateReview(eq(10L), eq(1L), any(UpdateReviewRequest.class)))
            .thenReturn(updated);

        mockMvc.perform(put("/api/buyer/reviews/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateReviewReturnsForbiddenForNonAuthor() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Changed!");
        when(reviewService.updateReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the review author can update this review"));

        mockMvc.perform(put("/api/buyer/reviews/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void updateReviewReturnsNotFoundForUnknownReview() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Changed!");
        when(reviewService.updateReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        mockMvc.perform(put("/api/buyer/reviews/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateReviewRequiresAuthentication() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Changed!");
        mockMvc.perform(put("/api/buyer/reviews/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void updateReviewForbidsShopOwner() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Changed!");
        mockMvc.perform(put("/api/buyer/reviews/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // --- DELETE /api/buyer/reviews/{reviewId} ---

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void deleteReviewReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/buyer/reviews/10"))
            .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(10L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void deleteReviewReturnsForbiddenForNonAuthor() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the review author can delete this review"))
            .when(reviewService).deleteReview(10L, 1L);

        mockMvc.perform(delete("/api/buyer/reviews/10"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteReviewRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/buyer/reviews/10"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void deleteReviewForbidsShopOwner() throws Exception {
        mockMvc.perform(delete("/api/buyer/reviews/10"))
            .andExpect(status().isForbidden());
    }

    // --- POST /api/reviews/{reviewId}/response ---

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void respondToReviewReturnsReviewWithResponse() throws Exception {
        RespondToReviewRequest request = new RespondToReviewRequest("Thank you!");
        ReviewResponse responded = sampleReview(10L, "Thank you!");

        when(reviewService.respondToReview(eq(10L), eq(2L), any(RespondToReviewRequest.class)))
            .thenReturn(responded);

        mockMvc.perform(post("/api/reviews/10/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Thank you!"))
            .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void respondToReviewReturnsForbiddenForWrongOwner() throws Exception {
        RespondToReviewRequest request = new RespondToReviewRequest("Thanks!");
        when(reviewService.respondToReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the shop owner can respond to this review"));

        mockMvc.perform(post("/api/reviews/10/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "SHOP_OWNER")
    void respondToReviewReturnsConflictWhenAlreadyResponded() throws Exception {
        RespondToReviewRequest request = new RespondToReviewRequest("Second response");
        when(reviewService.respondToReview(any(), any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Review already has a response"));

        mockMvc.perform(post("/api/reviews/10/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void respondToReviewRequiresAuthentication() throws Exception {
        RespondToReviewRequest request = new RespondToReviewRequest("Thanks!");
        mockMvc.perform(post("/api/reviews/10/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void respondToReviewForbidsBuyer() throws Exception {
        RespondToReviewRequest request = new RespondToReviewRequest("Thanks!");
        mockMvc.perform(post("/api/reviews/10/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/shops/{shopId}/reviews ---

    @Test
    void getReviewsByShopIsPublicAndReturnsPaginatedResults() throws Exception {
        ReviewResponse r1 = sampleReview(10L, null);
        ReviewResponse r2 = sampleReview(11L, "Owner replied");
        ReviewPageResponse page = samplePage(List.of(r1, r2), 4.5);

        when(reviewService.getReviewsByShop(eq(5L), any())).thenReturn(page);

        mockMvc.perform(get("/api/shops/5/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.reviews.length()").value(2))
            .andExpect(jsonPath("$.reviews[0].id").value(10))
            .andExpect(jsonPath("$.reviews[1].response").value("Owner replied"));
    }

    @Test
    void getReviewsByShopSupportsPagination() throws Exception {
        ReviewPageResponse page = samplePage(List.of(), 0.0);
        when(reviewService.getReviewsByShop(eq(5L), any())).thenReturn(page);

        mockMvc.perform(get("/api/shops/5/reviews?page=2&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviews").isEmpty());
    }

    @Test
    void getReviewsByShopReturnsNotFoundForUnknownShop() throws Exception {
        when(reviewService.getReviewsByShop(eq(999L), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        mockMvc.perform(get("/api/shops/999/reviews"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getReviewsByShopReturnsNullAverageWhenNoReviews() throws Exception {
        ReviewPageResponse page = samplePage(List.of(), null);
        when(reviewService.getReviewsByShop(eq(5L), any())).thenReturn(page);

        mockMvc.perform(get("/api/shops/5/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").doesNotExist())
            .andExpect(jsonPath("$.reviews").isEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "BUYER")
    void getReviewsByShopIsAlsoAccessibleWhenAuthenticated() throws Exception {
        ReviewPageResponse page = samplePage(List.of(sampleReview(10L, null)), 4.0);
        when(reviewService.getReviewsByShop(eq(5L), any())).thenReturn(page);

        mockMvc.perform(get("/api/shops/5/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").value(4.0));
    }
}
