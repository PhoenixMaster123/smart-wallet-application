package app.init;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "test-svc", url = "localhost:8086/api/v1")
public interface TestClient {

    @GetMapping("/notifications/say-hello")
    ResponseEntity<String> getHelloMessage();

    @GetMapping("/notifications/say-hello2")
    ResponseEntity<String> getGoodbyeMessage(@RequestParam("name") String name);
}
