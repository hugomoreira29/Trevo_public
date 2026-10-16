package pt.trevo.trevo_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pt.trevo.trevo_api.dto.AsyncImageUploadAcceptedResponse;
import pt.trevo.trevo_api.dto.AsyncImageUploadStatusResponse;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.AsyncImageUploadService;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {

    private final AsyncImageUploadService asyncImageUploadService;
    private final ImageUploadValidator imageUploadValidator;
    private final SecurityUtils securityUtils;

    public ImageUploadController(
        AsyncImageUploadService asyncImageUploadService,
        ImageUploadValidator imageUploadValidator,
        SecurityUtils securityUtils
    ) {
        this.asyncImageUploadService = asyncImageUploadService;
        this.imageUploadValidator = imageUploadValidator;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/images")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AsyncImageUploadAcceptedResponse uploadImage(@RequestParam MultipartFile file, Authentication authentication) {
        imageUploadValidator.validateOptionalImage(file, "file");
        Long ownerId = securityUtils.requireUserId(authentication);
        return asyncImageUploadService.startUpload(file, ownerId);
    }

    @GetMapping("/images/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    public AsyncImageUploadStatusResponse getUploadStatus(@PathVariable String uploadId, Authentication authentication) {
        Long ownerId = securityUtils.requireUserId(authentication);
        return asyncImageUploadService.getStatus(uploadId, ownerId);
    }
}
