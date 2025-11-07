package app.notification.client;

import app.notification.client.dto.UpsertPreferenceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-svc", url = "localhost:8081/api/v1")
public interface NotificationClient {

   @PostMapping("/preferences")
   void upsertPreference(@RequestBody UpsertPreferenceRequest requestBody);
}
