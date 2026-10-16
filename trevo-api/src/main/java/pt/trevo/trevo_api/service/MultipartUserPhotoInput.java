package pt.trevo.trevo_api.service;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.CloudinaryUploadResult;

public class MultipartUserPhotoInput implements UserPhotoInput {

    private final MultipartFile file;
    private final CloudinaryService cloudinaryService;
    private String cachedUrl;

    public MultipartUserPhotoInput(MultipartFile file) {
        this(file, null);
    }

    public MultipartUserPhotoInput(MultipartFile file, CloudinaryService cloudinaryService) {
        this.file = file;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public String filename() {
        return file.getOriginalFilename();
    }

    @Override
    public String contentType() {
        return file.getContentType();
    }

    @Override
    public long size() {
        return file.getSize();
    }

    @Override
    public byte[] bytes() {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read photo upload", exception);
        }
    }

    @Override
    public String temporaryUrl() {
        if (cachedUrl != null) {
            return cachedUrl;
        }
        if (cloudinaryService == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "CloudinaryService is required for temporary URL generation");
        }
        CloudinaryUploadResult result = cloudinaryService.uploadImage(bytes(), filename(), contentType());
        cachedUrl = result.url();
        return cachedUrl;
    }
}
