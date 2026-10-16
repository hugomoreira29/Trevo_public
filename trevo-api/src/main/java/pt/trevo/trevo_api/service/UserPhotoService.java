package pt.trevo.trevo_api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;
import pt.trevo.trevo_api.dto.UserPhotoResponse;
import pt.trevo.trevo_api.dto.UserPhotosResponse;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.UserPhoto;
import pt.trevo.trevo_api.persistence.repository.UserPhotoRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class UserPhotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPhotoService.class);
    private static final int REQUIRED_PHOTO_COUNT = 3;

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final TransactionOperations transactionOperations;

    @Autowired
    public UserPhotoService(
        CloudinaryService cloudinaryService,
        UserRepository userRepository,
        UserPhotoRepository userPhotoRepository,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        this(
            cloudinaryService,
            userRepository,
            userPhotoRepository,
            transactionOperations(transactionManagerProvider)
        );
    }

    UserPhotoService(
        CloudinaryService cloudinaryService,
        UserRepository userRepository,
        UserPhotoRepository userPhotoRepository,
        TransactionOperations transactionOperations
    ) {
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.userPhotoRepository = userPhotoRepository;
        this.transactionOperations = transactionOperations;
    }

    public UserPhotosResponse uploadPhotos(Long buyerId, List<MultipartFile> photos) {
        if (buyerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer id is required");
        }
        if (photos == null || photos.size() != REQUIRED_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 photos are required");
        }

        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));
        List<UserPhoto> previousPhotos = userPhotoRepository.findByBuyerId(buyerId);

        List<CloudinaryUploadResult> uploadedPhotos = uploadPhotosToCloudinary(photos);

        List<UserPhoto> savedPhotos;
        try {
            savedPhotos = transactionOperations.execute(status -> replaceStoredPhotos(buyerId, buyer, uploadedPhotos));
        } catch (RuntimeException ex) {
            deleteUploadedPhotos(uploadedPhotos);
            throw ex;
        }

        deletePreviousPhotos(previousPhotos);

        return new UserPhotosResponse(savedPhotos.stream()
            .map(this::toResponse)
            .toList());
    }

    private List<UserPhoto> replaceStoredPhotos(
        Long buyerId,
        User buyer,
        List<CloudinaryUploadResult> uploadedPhotos
    ) {
        Instant uploadedAt = Instant.now();
        userPhotoRepository.deleteAllByBuyerId(buyerId);
        return userPhotoRepository.saveAll(uploadedPhotos.stream()
            .map(uploadedPhoto -> toEntity(buyer, uploadedPhoto, uploadedAt))
            .toList());
    }

    private List<CloudinaryUploadResult> uploadPhotosToCloudinary(List<MultipartFile> photos) {
        List<CloudinaryUploadResult> uploadedPhotos = new ArrayList<>();
        try {
            for (MultipartFile photo : photos) {
                uploadedPhotos.add(cloudinaryService.uploadImage(photo));
            }
        } catch (RuntimeException ex) {
            deleteUploadedPhotos(uploadedPhotos);
            throw ex;
        }

        return uploadedPhotos;
    }

    private UserPhoto toEntity(User buyer, CloudinaryUploadResult uploadedPhoto, Instant uploadedAt) {
        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setBuyer(buyer);
        userPhoto.setPhotoUrl(uploadedPhoto.url());
        userPhoto.setCloudinaryPublicId(uploadedPhoto.publicId());
        userPhoto.setUploadedAt(uploadedAt);
        return userPhoto;
    }

    private UserPhotoResponse toResponse(UserPhoto userPhoto) {
        return new UserPhotoResponse(userPhoto.getId(), userPhoto.getPhotoUrl(), userPhoto.getUploadedAt());
    }

    private void deletePreviousPhotos(List<UserPhoto> previousPhotos) {
        previousPhotos.stream()
            .map(UserPhoto::getCloudinaryPublicId)
            .filter(publicId -> publicId != null && !publicId.isBlank())
            .forEach(publicId -> deleteImage(publicId, "previous user photo"));
    }

    private void deleteUploadedPhotos(List<CloudinaryUploadResult> uploadedPhotos) {
        uploadedPhotos.stream()
            .map(CloudinaryUploadResult::publicId)
            .filter(publicId -> publicId != null && !publicId.isBlank())
            .forEach(publicId -> deleteImage(publicId, "orphaned uploaded user photo"));
    }

    private void deleteImage(String publicId, String context) {
        try {
            cloudinaryService.deleteImage(publicId);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete {} {} from Cloudinary", context, publicId, ex);
        }
    }

    private static TransactionOperations transactionOperations(
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();
        if (transactionManager == null) {
            return TransactionOperations.withoutTransaction();
        }

        return new TransactionTemplate(transactionManager);
    }
}
