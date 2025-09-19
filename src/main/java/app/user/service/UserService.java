package app.user.service;

import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.wallet.service.WalletService;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletService walletService, SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.subscriptionService = subscriptionService;
    }

    public User login(LoginRequest loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.username());

        if(user.isEmpty()) {
            throw new RuntimeException("Incorrect username or password.");
        }

        String rawPassword = loginRequest.password();
        String hashedPassword = user.get().getPassword();

        if(!passwordEncoder.matches(rawPassword, hashedPassword)) {
            throw new RuntimeException("Incorrect username or password.");
        }

        return user.get();
    }

    @Transactional // That means every method will be executed successfully or rollback
    public void register(RegisterRequest registerRequest) {
        Optional<User> user = userRepository.findByUsername(registerRequest.username());

        if(user.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User newUser = User.builder()
                .username(registerRequest.username())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(UserRole.USER)
                .country(registerRequest.country())
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);
        walletService.createDefaultWallet(newUser);
        subscriptionService.createDefaultSubscription(newUser);

        log.info("User {} registered successfully", newUser.getUsername());
    }
}
