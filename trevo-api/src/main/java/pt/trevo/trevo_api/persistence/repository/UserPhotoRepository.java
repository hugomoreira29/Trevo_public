package pt.trevo.trevo_api.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.trevo.trevo_api.persistence.entity.UserPhoto;

@Repository
public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {

    List<UserPhoto> findByBuyerId(Long buyerId);

    void deleteAllByBuyerId(Long buyerId);
}
