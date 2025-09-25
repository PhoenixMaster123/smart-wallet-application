package app.user.property;

import app.user.model.Country;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "users")
public class UserProperties {

    private DefaultUser defaultUser;

    private String testProperty;

//    private String defaultUsername;
//    private String defaultPassword;
//    private Country defaultCountry;

    @Data
    public static class DefaultUser {

        private String username;

        private String password;

        private Country country;
    }

//    @PostConstruct
//    public void test() { // For testing if the properties are loaded correctly
//        System.out.println("UserProperties loaded: " + this); // or without arguments only for debugging
//    }

}
