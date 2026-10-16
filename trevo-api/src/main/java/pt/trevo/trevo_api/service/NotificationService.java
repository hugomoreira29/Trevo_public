package pt.trevo.trevo_api.service;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.NotificationResponse;
import pt.trevo.trevo_api.dto.NotificationsResponse;
import pt.trevo.trevo_api.persistence.entity.AppNotification;
import pt.trevo.trevo_api.persistence.entity.NotificationType;
import pt.trevo.trevo_api.persistence.entity.Order;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppNotification createNewOrderNotification(Order order) {
        Shop shop = order.getShop();

        AppNotification notification = new AppNotification();
        notification.setRecipient(shop.getOwner());
        notification.setOrder(order);
        notification.setType(NotificationType.NEW_ORDER);
        notification.setTitle("Nova encomenda recebida");
        notification.setMessage("Recebeste uma nova encomenda na loja " + shop.getName() + ".");
        notification.setCreatedAt(Instant.now());

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationsResponse getNotifications(Long recipientId, int page, int size) {
        if (recipientId == null || page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification page request");
        }

        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
        Page<AppNotification> notificationPage = notificationRepository
            .findByRecipientId(
                recipientId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        List<NotificationResponse> notifications = notificationPage
            .stream()
            .map(this::toResponse)
            .toList();

        return new NotificationsResponse(
            unreadCount,
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages(),
            notifications
        );
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long recipientId) {
        if (notificationId == null || recipientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification read request");
        }

        AppNotification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    private NotificationResponse toResponse(AppNotification notification) {
        Order order = notification.getOrder();

        return new NotificationResponse(
            notification.getId(),
            order == null ? null : order.getId(),
            notification.getType().name(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getReadAt() != null,
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
