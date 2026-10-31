package com.campus.events.service;

import com.campus.events.model.AppUser;
import com.campus.events.model.Notification;
import com.campus.events.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void sendNotification(AppUser user, String type, String subject, String message) {
        try {
            Notification notification = new Notification(user, type, subject, message);
            notificationRepository.save(notification);
            log.info("In-app notification saved for {}: [{}] {}", user.getEmail(), type, subject);
            
            // Email abstraction fallback
            simulateEmailDelivery(user.getEmail(), subject, message);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}", user.getEmail(), e);
        }
    }

    private void simulateEmailDelivery(String recipientEmail, String subject, String message) {
        // Production SMTP integration placeholder with structured fallback
        log.info("[EMAIL SERVICE DISPATCH] To: {} | Subject: {} | Body length: {} chars", 
                recipientEmail, subject, message != null ? message.length() : 0);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadStatusFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadStatusFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setReadStatus(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}

