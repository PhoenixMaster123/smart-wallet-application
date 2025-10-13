package app.user.service;

import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.property.UserProperties;
import app.user.repository.UserRepository;
import app.wallet.service.WalletService;
import app.web.dto.EditProfileRequest;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final SubscriptionService subscriptionService;
    private final UserProperties userProperties;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletService walletService, SubscriptionService subscriptionService, UserProperties userProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.subscriptionService = subscriptionService;
        this.userProperties = userProperties;
    }

    public User login(LoginRequest loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());

        if(user.isEmpty()) {
            throw new RuntimeException("Incorrect username or password.");
        }

        String rawPassword = loginRequest.getPassword();
        String hashedPassword = user.get().getPassword();

        if(!passwordEncoder.matches(rawPassword, hashedPassword)) {
            throw new RuntimeException("Incorrect username or password.");
        }

        return user.get();
    }

    @Transactional // That means every method will be executed successfully or rollback
    public void register(RegisterRequest registerRequest) {
        Optional<User> user = userRepository.findByUsername(registerRequest.getUsername());

        if(user.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User newUser = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .country(registerRequest.getCountry())
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);
        walletService.createDefaultWallet(newUser);
        subscriptionService.createDefaultSubscription(newUser);

        log.info("User {} registered successfully", newUser.getUsername());
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getByUsername(String username) {

        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getById(UUID id) {
        return  userRepository.findById(id).orElseThrow(() -> new RuntimeException("User by id " + id + " not found"));
    }

    public User getDefaultUser() {
        return getByUsername(userProperties.getDefaultUser().getUsername());
    }

    public void updateProfile(EditProfileRequest editProfileRequest, UUID id) {
        User user = getById(id);

        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setEmail(editProfileRequest.getEmail());
        user.setProfilePicture(editProfileRequest.getProfilePicture());

        userRepository.save(user);
    }

    public void switchStatus(UUID userId) {
        User user = getById(userId);

        // true -> false
        // false -> true
        user.setActive(!user.isActive());

        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);
    }

    public void switchRole(UUID userId) {

        User user = getById(userId);

        if(user.getRole() == UserRole.USER) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.USER);
        }

        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);
    }
}
