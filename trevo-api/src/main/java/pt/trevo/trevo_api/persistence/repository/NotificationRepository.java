package pt.trevo.trevo_api.persistence.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.trevo.trevo_api.persistence.entity.AppNotification;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Page<AppNotification> findByRecipientId(Long recipientId, Pageable pageable);

    Optional<AppNotification> findByIdAndRecipientId(Long id, Long recipientId);
}
