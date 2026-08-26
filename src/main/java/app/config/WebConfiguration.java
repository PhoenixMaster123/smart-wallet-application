package app.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableMethodSecurity
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.authorizeHttpRequests(matcher -> matcher
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/", "/register").permitAll()
//              .requestMatchers("admin-panel").hasRole("ADMIN")
//              This is for the admin panel, but we will do it on methods
                .anyRequest().authenticated()
        )
        .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        //.usernameParameter("email") // if we login with email | our id = email in the login form
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll()

        )
        .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/")
        );


        return httpSecurity.build();
    }

    // NOTE: to login with Google or to have 2FA -> we need to use OAuth2 Login
}


////////////////////////////////////////// Interceptors //////////////////////////////////////////////////

//    private final SessionCheckInterceptor sessionCheckInterceptor;
//
//    @Autowired
//    public WebConfiguration(SessionCheckInterceptor sessionCheckInterceptor) {
//        this.sessionCheckInterceptor = sessionCheckInterceptor;
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(sessionCheckInterceptor)
//                .addPathPatterns("/**")
//                .excludePathPatterns("/css/**", "/js/**", "/images/**");
//    }
