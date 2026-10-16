package pt.trevo.trevo_api.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.UserPhotosResponse;
import pt.trevo.trevo_api.security.SecurityUtils;
import pt.trevo.trevo_api.service.UserPhotoService;

@RestController
@RequestMapping("/api/buyer/photos")
public class UserPhotoController {

    private static final int REQUIRED_PHOTO_COUNT = 3;

    private final UserPhotoService userPhotoService;
    private final ImageUploadValidator imageUploadValidator;
    private final SecurityUtils securityUtils;

    public UserPhotoController(
        UserPhotoService userPhotoService,
        ImageUploadValidator imageUploadValidator,
        SecurityUtils securityUtils
    ) {
        this.userPhotoService = userPhotoService;
        this.imageUploadValidator = imageUploadValidator;
        this.securityUtils = securityUtils;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public UserPhotosResponse uploadPhotos(
        Authentication authentication,
        @RequestParam("photos") List<MultipartFile> photos
    ) {
        if (photos == null || photos.size() != REQUIRED_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 photos are required");
        }
        photos.forEach(photo -> imageUploadValidator.validateOptionalImage(photo, "photos"));
        Long buyerId = securityUtils.requireUserId(authentication);
        return userPhotoService.uploadPhotos(buyerId, photos);
    }
}
