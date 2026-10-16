package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CreateReviewRequest;
import pt.trevo.trevo_api.dto.RespondToReviewRequest;
import pt.trevo.trevo_api.dto.ReviewResponse;
import pt.trevo.trevo_api.dto.UpdateReviewRequest;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;
import pt.trevo.trevo_api.persistence.entity.Review;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.OrderRepository;
import pt.trevo.trevo_api.persistence.repository.ReviewRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class ReviewServiceTest extends BaseUnitTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ProfanityFilter profanityFilter;

    @InjectMocks
    private ReviewService reviewService;

    // REV-B01: createReview

    @Test
    void createReviewWithValidDataSucceeds() {
        Long buyerId = 10L;
        Long shopId = 301L;
        User buyer = User.builder().id(buyerId).build();
        Shop shop = shop(shopId, 901L, "North Shop");

        when(orderRepository.existsByBuyerIdAndShopIdAndStatus(buyerId, shopId, OrderStatus.DELIVERED)).thenReturn(true);
        when(reviewRepository.existsByBuyerIdAndShopId(buyerId, shopId)).thenReturn(false);
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse review = reviewService.createReview(buyerId, shopId, new CreateReviewRequest(4, "Great shop!"));

        assertThat(review.buyerId()).isEqualTo(buyerId);
        assertThat(review.shopId()).isEqualTo(shopId);
        assertThat(review.rating()).isEqualTo(4);
        assertThat(review.comment()).isEqualTo("Great shop!");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReviewWithBlockedWordThrows400() {
        when(profanityFilter.containsBlockedWord("This shop is a badword.")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(10L, 301L, new CreateReviewRequest(4, "This shop is a badword."))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReviewWithCleanCommentPassesProfanityFilter() {
        Long buyerId = 10L;
        Long shopId = 301L;
        User buyer = User.builder().id(buyerId).build();
        Shop shop = shop(shopId, 901L, "North Shop");

        when(orderRepository.existsByBuyerIdAndShopIdAndStatus(buyerId, shopId, OrderStatus.DELIVERED)).thenReturn(true);
        when(reviewRepository.existsByBuyerIdAndShopId(buyerId, shopId)).thenReturn(false);
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse review = reviewService.createReview(buyerId, shopId, new CreateReviewRequest(5, "Clean and helpful service."));

        assertThat(review.comment()).isEqualTo("Clean and helpful service.");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReviewWithNoDeliveredOrderThrows403() {
        Long buyerId = 10L;
        Long shopId = 301L;

        when(orderRepository.existsByBuyerIdAndShopIdAndStatus(buyerId, shopId, OrderStatus.DELIVERED)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(buyerId, shopId, new CreateReviewRequest(5, "Amazing!"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createDuplicateReviewThrows409() {
        Long buyerId = 10L;
        Long shopId = 301L;

        when(orderRepository.existsByBuyerIdAndShopIdAndStatus(buyerId, shopId, OrderStatus.DELIVERED)).thenReturn(true);
        when(reviewRepository.existsByBuyerIdAndShopId(buyerId, shopId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(buyerId, shopId, new CreateReviewRequest(3, "Good shop"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReviewWithNullRatingThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(10L, 301L, new CreateReviewRequest(null, "Missing rating"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReviewWithRatingOutOfRangeThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(10L, 301L, new CreateReviewRequest(6, "Too high"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // REV-B02: updateReview and deleteReview

    @Test
    void updateReviewByAuthorSucceeds() {
        Long buyerId = 10L;
        Review review = review(1L, User.builder().id(buyerId).build(), shop(301L, 901L, "North Shop"), 3, "Ok", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse updated = reviewService.updateReview(1L, buyerId, new UpdateReviewRequest(5, "Actually great!"));

        assertThat(updated.rating()).isEqualTo(5);
        assertThat(updated.comment()).isEqualTo("Actually great!");
        verify(reviewRepository).save(review);
    }

    @Test
    void updateReviewWithCleanCommentPassesProfanityFilter() {
        Long buyerId = 10L;
        Review review = review(1L, User.builder().id(buyerId).build(), shop(301L, 901L, "North Shop"), 3, "Ok", null);

        when(profanityFilter.containsBlockedWord("Helpful and clean update.")).thenReturn(false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse updated = reviewService.updateReview(1L, buyerId, new UpdateReviewRequest(4, "Helpful and clean update."));

        assertThat(updated.comment()).isEqualTo("Helpful and clean update.");
        verify(reviewRepository).save(review);
    }

    @Test
    void updateReviewByNonAuthorThrows403() {
        Long authorId = 10L;
        Long intruderId = 99L;
        Review review = review(1L, User.builder().id(authorId).build(), shop(301L, 901L, "North Shop"), 3, "Ok", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(1L, intruderId, new UpdateReviewRequest(5, "Changed!"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateReviewWithRatingOutOfRangeThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(1L, 10L, new UpdateReviewRequest(0, "Too low"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateReviewWithBlockedWordThrows400() {
        when(profanityFilter.containsBlockedWord("The service was stupid.")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(1L, 10L, new UpdateReviewRequest(4, "The service was stupid."))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deleteReviewByAuthorSucceeds() {
        Long buyerId = 10L;
        Review review = review(1L, User.builder().id(buyerId).build(), shop(301L, 901L, "North Shop"), 4, "Good", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L, buyerId);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewByNonAuthorThrows403() {
        Long authorId = 10L;
        Long intruderId = 99L;
        Review review = review(1L, User.builder().id(authorId).build(), shop(301L, 901L, "North Shop"), 4, "Good", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.deleteReview(1L, intruderId)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void deleteReviewWithNullReviewIdThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.deleteReview(null, 10L)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    // REV-B03: respondToReview

    @Test
    void respondToReviewByShopOwnerSucceeds() {
        Long shopOwnerId = 901L;
        Review review = review(1L, User.builder().id(10L).build(), shop(301L, shopOwnerId, "North Shop"), 4, "Good", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse responded = reviewService.respondToReview(1L, shopOwnerId, new RespondToReviewRequest("Thank you for your feedback!"));

        assertThat(responded.response()).isEqualTo("Thank you for your feedback!");
        verify(reviewRepository).save(review);
    }

    @Test
    void respondToReviewByNonOwnerThrows403() {
        Long shopOwnerId = 901L;
        Long intruderId = 999L;
        Review review = review(1L, User.builder().id(10L).build(), shop(301L, shopOwnerId, "North Shop"), 4, "Good", null);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.respondToReview(1L, intruderId, new RespondToReviewRequest("Thank you!"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void respondToReviewWhenResponseAlreadyExistsThrows409() {
        Long shopOwnerId = 901L;
        Review review = review(1L, User.builder().id(10L).build(), shop(301L, shopOwnerId, "North Shop"), 4, "Good", "Already responded!");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.respondToReview(1L, shopOwnerId, new RespondToReviewRequest("Another response"))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void respondToReviewWithBlankResponseThrows400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.respondToReview(1L, 901L, new RespondToReviewRequest("   "))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // Helpers

    private Review review(Long id, User buyer, Shop shop, Integer rating, String comment, String response) {
        Review review = new Review();
        review.setId(id);
        review.setBuyer(buyer);
        review.setShop(shop);
        review.setRating(rating);
        review.setComment(comment);
        review.setResponse(response);
        review.setCreatedAt(Instant.now());
        return review;
    }

    private Shop shop(Long id, Long ownerId, String name) {
        return Shop.builder()
                .id(id)
                .owner(User.builder().id(ownerId).build())
                .name(name)
                .build();
    }
}
