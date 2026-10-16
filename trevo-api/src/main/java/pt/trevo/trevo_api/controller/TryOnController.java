package pt.trevo.trevo_api.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pt.trevo.trevo_api.dto.TryOnImageResponse;
import pt.trevo.trevo_api.dto.TryOnUsageResponse;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.CloudinaryService;
import pt.trevo.trevo_api.service.MultipartUserPhotoInput;
import pt.trevo.trevo_api.service.TryOnService;
import pt.trevo.trevo_api.service.UserPhotoInput;

@RestController
@RequestMapping("/api/buyer/try-on")
@PreAuthorize("hasRole('BUYER')")
public class TryOnController {

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TryOnImageResponse generateTryOn(
        Authentication authentication,
        @RequestParam Long productId,
        @RequestParam("photo") MultipartFile photo
    ) {
        imageUploadValidator.validateOptionalImage(photo, "photo");
        UserPhotoInput photoInput = new MultipartUserPhotoInput(photo, cloudinaryService);
        Long buyerId = securityUtils.requireUserId(authentication);
        return tryOnService.generateTryOn(buyerId, productId, photoInput);
    }

    @GetMapping("/usage")
    public TryOnUsageResponse getUsage(Authentication authentication) {
        Long buyerId = securityUtils.requireUserId(authentication);
        return tryOnService.getUsage(buyerId);
    }
}
