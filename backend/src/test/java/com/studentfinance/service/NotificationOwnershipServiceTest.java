package com.studentfinance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.studentfinance.entity.Notification;
import com.studentfinance.dto.UnreadCountResponse;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.NotificationRepository;
import com.studentfinance.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NotificationOwnershipServiceTest {
    @Test
    void guessedOtherUsersNotificationIdReturnsNotFound() {
        NotificationRepository repository = mock(NotificationRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        User user = new User();
        user.setId(1L);
        when(currentUser.getCurrentUser()).thenReturn(user);
        UserRepository users = mock(UserRepository.class);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(repository.findByIdAndUser_Id(55L, 1L)).thenReturn(Optional.empty());
        NotificationService service = new NotificationService(repository, currentUser, users);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.markRead(55L));

        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void duplicateNotificationKeyDoesNotInsertTwice() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        User owner = new User();
        owner.setId(7L);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(owner));
        when(repository.existsByUser_IdAndDeduplicationKey(7L, "key")).thenReturn(true);
        NotificationService service = new NotificationService(repository, mock(CurrentUserService.class), users);

        service.create(owner, com.studentfinance.entity.NotificationType.SAVINGS_GOAL_DUE,
                "Title", "Message", 4L, "key");

        verify(repository, never()).save(any(Notification.class));
    }
    @Test
    void markingOwnedNotificationReadAndCountingUnreadUsesAuthenticatedOwner() {
        NotificationRepository repository = mock(NotificationRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        User owner = new User();
        owner.setId(2L);
        Notification notice = new Notification();
        notice.setId(6L);
        notice.setUser(owner);
        notice.setType(NotificationType.TRANSACTION_RECORDED);
        notice.setTitle("Transaction recorded");
        notice.setMessage("A transaction was recorded.");
        when(currentUser.getCurrentUser()).thenReturn(owner);
        UserRepository users = mock(UserRepository.class);
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(owner));
        when(repository.findByIdAndUser_Id(6L, 2L)).thenReturn(Optional.of(notice));
        when(repository.save(notice)).thenReturn(notice);
        when(repository.countByUser_IdAndReadStatusFalse(2L)).thenReturn(0L);
        NotificationService service = new NotificationService(repository, currentUser, users);

        assertTrue(service.markRead(6L).read());
        assertEquals(new UnreadCountResponse(0), service.getUnreadCount());
        verify(repository).save(notice);
        verify(repository).countByUser_IdAndReadStatusFalse(2L);
    }

}
