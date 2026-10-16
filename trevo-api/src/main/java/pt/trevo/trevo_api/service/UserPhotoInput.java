package pt.trevo.trevo_api.service;

public interface UserPhotoInput {

    String filename();

    String contentType();

    long size();

    byte[] bytes();

    String temporaryUrl();
}
