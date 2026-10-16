package pt.trevo.trevo_api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.trevo.trevo_api.dto.CreateReviewRequest;
import pt.trevo.trevo_api.dto.RespondToReviewRequest;
import pt.trevo.trevo_api.dto.ReviewPageResponse;
import pt.trevo.trevo_api.dto.ReviewResponse;
import pt.trevo.trevo_api.dto.UpdateReviewRequest;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.ReviewService;

@RestController
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/api/buyer/reviews/shops/{shopId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ReviewResponse createReview(
        @PathVariable Long shopId,
        @Valid @RequestBody CreateReviewRequest request,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return reviewService.createReview(buyerId, shopId, request);
    }

    @PutMapping("/api/buyer/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER')")
    public ReviewResponse updateReview(
        @PathVariable Long reviewId,
        @Valid @RequestBody UpdateReviewRequest request,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return reviewService.updateReview(reviewId, buyerId, request);
    }

    @DeleteMapping("/api/buyer/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BUYER')")
    public void deleteReview(
        @PathVariable Long reviewId,
        Authentication authentication
    ) {
        Long buyerId = securityUtils.requireUserId(authentication);
        reviewService.deleteReview(reviewId, buyerId);
    }

    @PostMapping("/api/reviews/{reviewId}/response")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ReviewResponse respondToReview(
        @PathVariable Long reviewId,
        @Valid @RequestBody RespondToReviewRequest request,
        Authentication authentication
    ) {
        Long ownerId = securityUtils.requireUserId(authentication);
        return reviewService.respondToReview(reviewId, ownerId, request);
    }

    @GetMapping("/api/shops/{shopId}/reviews")
    public ReviewPageResponse getReviewsByShop(
        @PathVariable Long shopId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reviewService.getReviewsByShop(shopId, pageable);
    }
}
