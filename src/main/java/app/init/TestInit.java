package app.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TestInit implements ApplicationRunner {

    private final NotificationClient notificationClient;

    public TestInit(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        ResponseEntity<String> response = notificationClient.getHelloMessage();
        ResponseEntity<String> response2 = notificationClient.getGoodbyeMessage("John");

        System.out.println(response.getBody());

    }
}