package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.entity.UserPhoto;
import pt.trevo.trevo_api.persistence.repository.UserPhotoRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class UserPhotoServiceTest extends BaseUnitTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPhotoRepository userPhotoRepository;

    @Test
    void uploadPhotosStoresExactlyThreeCloudinaryUrlsForBuyer() {
        UserPhotoService userPhotoService = userPhotoService();
        User buyer = User.builder().id(10L).email("buyer@trevo.pt").build();
        UserPhoto previousPhoto = userPhoto("old/public-id", "https://res.cloudinary.com/demo/old.jpg");
        List<MockMultipartFile> photos = List.of(
            photo("photo-1.jpg"),
            photo("photo-2.jpg"),
            photo("photo-3.jpg")
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(userPhotoRepository.findByBuyerId(10L)).thenReturn(List.of(previousPhoto));
        when(cloudinaryService.uploadImage(photos.get(0)))
            .thenReturn(new CloudinaryUploadResult("users/10/1", "https://res.cloudinary.com/demo/photo-1.jpg"));
        when(cloudinaryService.uploadImage(photos.get(1)))
            .thenReturn(new CloudinaryUploadResult("users/10/2", "https://res.cloudinary.com/demo/photo-2.jpg"));
        when(cloudinaryService.uploadImage(photos.get(2)))
            .thenReturn(new CloudinaryUploadResult("users/10/3", "https://res.cloudinary.com/demo/photo-3.jpg"));
        when(userPhotoRepository.saveAll(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        var response = userPhotoService.uploadPhotos(10L, List.copyOf(photos));

        verify(userPhotoRepository).deleteAllByBuyerId(10L);
        verify(cloudinaryService).deleteImage("old/public-id");
        ArgumentCaptor<Iterable<UserPhoto>> userPhotosCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(userPhotoRepository).saveAll(userPhotosCaptor.capture());
        List<UserPhoto> savedPhotos = toList(userPhotosCaptor.getValue());

        assertThat(savedPhotos).hasSize(3);
        assertThat(savedPhotos).allSatisfy(savedPhoto -> {
            assertThat(savedPhoto.getBuyer()).isSameAs(buyer);
            assertThat(savedPhoto.getUploadedAt()).isNotNull();
        });
        assertThat(savedPhotos)
            .extracting(UserPhoto::getPhotoUrl)
            .containsExactly(
                "https://res.cloudinary.com/demo/photo-1.jpg",
                "https://res.cloudinary.com/demo/photo-2.jpg",
                "https://res.cloudinary.com/demo/photo-3.jpg"
            );
        assertThat(savedPhotos)
            .extracting(UserPhoto::getCloudinaryPublicId)
            .containsExactly("users/10/1", "users/10/2", "users/10/3");
        assertThat(response.photos()).hasSize(3);
        assertThat(response.photos().get(0).url()).isEqualTo("https://res.cloudinary.com/demo/photo-1.jpg");
    }

    @Test
    void uploadPhotosRejectsFewerThanThreePhotos() {
        UserPhotoService userPhotoService = userPhotoService();

        Throwable thrown = catchThrowable(() -> userPhotoService.uploadPhotos(10L, List.of(photo("one.jpg"), photo("two.jpg"))));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Exactly 3 photos are required");
        verify(cloudinaryService, never()).uploadImage(any());
        verify(userPhotoRepository, never()).saveAll(any());
    }

    @Test
    void uploadPhotosRejectsMoreThanThreePhotos() {
        UserPhotoService userPhotoService = userPhotoService();

        Throwable thrown = catchThrowable(() -> userPhotoService.uploadPhotos(
            10L,
            List.of(photo("one.jpg"), photo("two.jpg"), photo("three.jpg"), photo("four.jpg"))
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Exactly 3 photos are required");
        verify(cloudinaryService, never()).uploadImage(any());
        verify(userPhotoRepository, never()).saveAll(any());
    }

    @Test
    void uploadPhotosReturnsNotFoundWhenBuyerDoesNotExist() {
        UserPhotoService userPhotoService = userPhotoService();
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> userPhotoService.uploadPhotos(
            10L,
            List.of(photo("one.jpg"), photo("two.jpg"), photo("three.jpg"))
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException exception = (ResponseStatusException) thrown;
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Buyer not found");
        verify(cloudinaryService, never()).uploadImage(any());
        verify(userPhotoRepository, never()).saveAll(any());
    }

    @Test
    void uploadPhotosDeletesNewCloudinaryUploadsWhenDatabaseWriteFails() {
        UserPhotoService userPhotoService = userPhotoService();
        User buyer = User.builder().id(10L).email("buyer@trevo.pt").build();
        List<MockMultipartFile> photos = List.of(
            photo("photo-1.jpg"),
            photo("photo-2.jpg"),
            photo("photo-3.jpg")
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(userPhotoRepository.findByBuyerId(10L)).thenReturn(List.of(userPhoto("old/public-id", "old-url")));
        when(cloudinaryService.uploadImage(photos.get(0)))
            .thenReturn(new CloudinaryUploadResult("users/10/1", "https://res.cloudinary.com/demo/photo-1.jpg"));
        when(cloudinaryService.uploadImage(photos.get(1)))
            .thenReturn(new CloudinaryUploadResult("users/10/2", "https://res.cloudinary.com/demo/photo-2.jpg"));
        when(cloudinaryService.uploadImage(photos.get(2)))
            .thenReturn(new CloudinaryUploadResult("users/10/3", "https://res.cloudinary.com/demo/photo-3.jpg"));
        when(userPhotoRepository.saveAll(any())).thenThrow(new IllegalStateException("database write failed"));

        Throwable thrown = catchThrowable(() -> userPhotoService.uploadPhotos(10L, List.copyOf(photos)));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(cloudinaryService).deleteImage("users/10/1");
        verify(cloudinaryService).deleteImage("users/10/2");
        verify(cloudinaryService).deleteImage("users/10/3");
        verify(cloudinaryService, never()).deleteImage("old/public-id");
    }

    @Test
    void uploadPhotosDeletesCompletedCloudinaryUploadsWhenLaterUploadFails() {
        UserPhotoService userPhotoService = userPhotoService();
        User buyer = User.builder().id(10L).email("buyer@trevo.pt").build();
        List<MockMultipartFile> photos = List.of(
            photo("photo-1.jpg"),
            photo("photo-2.jpg"),
            photo("photo-3.jpg")
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));
        when(userPhotoRepository.findByBuyerId(10L)).thenReturn(List.of(userPhoto("old/public-id", "old-url")));
        when(cloudinaryService.uploadImage(photos.get(0)))
            .thenReturn(new CloudinaryUploadResult("users/10/1", "https://res.cloudinary.com/demo/photo-1.jpg"));
        when(cloudinaryService.uploadImage(photos.get(1)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary unavailable"));

        Throwable thrown = catchThrowable(() -> userPhotoService.uploadPhotos(10L, List.copyOf(photos)));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        verify(cloudinaryService).deleteImage("users/10/1");
        verify(cloudinaryService, never()).deleteImage("old/public-id");
        verify(userPhotoRepository, never()).deleteAllByBuyerId(any());
        verify(userPhotoRepository, never()).saveAll(any());
    }

    private MockMultipartFile photo(String filename) {
        return new MockMultipartFile("photos", filename, "image/jpeg", new byte[] {1, 2, 3});
    }

    private UserPhoto userPhoto(String cloudinaryPublicId, String photoUrl) {
        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setCloudinaryPublicId(cloudinaryPublicId);
        userPhoto.setPhotoUrl(photoUrl);
        return userPhoto;
    }

    private List<UserPhoto> toList(Iterable<UserPhoto> userPhotos) {
        return ((List<UserPhoto>) userPhotos);
    }

    private UserPhotoService userPhotoService() {
        return new UserPhotoService(
            cloudinaryService,
            userRepository,
            userPhotoRepository,
            TransactionOperations.withoutTransaction()
        );
    }
}
