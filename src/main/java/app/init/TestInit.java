package app.init;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestInit implements ApplicationRunner {

    private final TestClient testClient;

    public TestInit(TestClient testClient) {
        this.testClient = testClient;
    }

    @Override
    public void run(ApplicationArguments args) {

        // test-svc is a demo dependency, so a failed call must not take the
        // whole application down the way an escaping exception from an
        // ApplicationRunner would.
        try {
            ResponseEntity<String> helloResponse = testClient.getHelloMessage();
            ResponseEntity<String> goodbyeResponse = testClient.getGoodbyeMessage("John");

            log.info("[S2S Call]: test-svc replied {} and {}", helloResponse.getBody(), goodbyeResponse.getBody());
        } catch (FeignException e) {
            log.warn("[S2S Call]: test-svc is unavailable, skipping the demo call: {}", e.getMessage());
        }
    }
}
