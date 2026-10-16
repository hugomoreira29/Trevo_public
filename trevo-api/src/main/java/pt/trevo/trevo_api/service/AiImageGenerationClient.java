package pt.trevo.trevo_api.service;

import pt.trevo.trevo_api.persistence.entity.Product;

public interface AiImageGenerationClient {

    String generateTryOn(Product product, UserPhotoInput photo);
}
