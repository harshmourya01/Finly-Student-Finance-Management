package com.studentfinance.service;

import com.studentfinance.dto.NotificationResponse;
import com.studentfinance.dto.UnreadCountResponse;
import com.studentfinance.entity.Notification;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.NotificationRepository;
import com.studentfinance.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(int requestedLimit) {
        Long userId = currentUserService.getCurrentUser().getId();
        int limit = Math.max(1, Math.min(requestedLimit, 50));
        return notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                        userId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationRepository.countByUser_IdAndReadStatusFalse(
                currentUserService.getCurrentUser().getId()));
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = findOwned(id);
        notification.setReadStatus(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public UnreadCountResponse markAllRead() {
        notificationRepository.markAllRead(currentUserService.getCurrentUser().getId());
        return getUnreadCount();
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.delete(findOwned(id));
    }

    @Transactional
    public void create(User user, NotificationType type, String title, String message,
                       Long relatedEntityId, String deduplicationKey) {
        User lockedOwner = lockOwner(user);
        if (deduplicationKey != null
                && notificationRepository.existsByUser_IdAndDeduplicationKey(lockedOwner.getId(), deduplicationKey)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(lockedOwner);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setDeduplicationKey(deduplicationKey);
        notificationRepository.save(notification);
    }

    @Transactional
    public User lockOwner(User user) {
        return userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Notification owner not found"));
    }

    private Notification findOwned(Long id) {
        Long userId = currentUserService.getCurrentUser().getId();
        return notificationRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.isReadStatus(), notification.getCreatedAt(),
                notification.getRelatedEntityId());
    }
}
