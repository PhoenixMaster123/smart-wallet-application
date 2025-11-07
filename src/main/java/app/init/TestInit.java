package app.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TestInit implements ApplicationRunner {

    private final TestClient testClient;

    public TestInit(TestClient testClient) {
        this.testClient = testClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        ResponseEntity<String> response = testClient.getHelloMessage();
        ResponseEntity<String> response2 = testClient.getGoodbyeMessage("John");

        System.out.println(response.getBody());

    }
}