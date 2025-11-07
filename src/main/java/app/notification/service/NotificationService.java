package app.notification.service;

import app.notification.client.NotificationClient;
import app.notification.client.dto.UpsertPreferenceRequest;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient client;

    @Autowired
    public NotificationService(NotificationClient client) {
        this.client = client;
    }

    public void upsertPreference(UUID userId, boolean notificationsEnabled, String email) {

        UpsertPreferenceRequest request = UpsertPreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(notificationsEnabled)
                .contactInfo(email)
                .build();

        // 2xx - Success
        // otherwise - exception will be thrown
        try {
            client.upsertPreference(request);
        } catch (FeignException e) {
            log.error("[S2S Call]: Failed due to %s.".formatted(e.getMessage()));
        }

    }
}