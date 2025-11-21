package app.user;

import app.notification.service.NotificationService;
import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.property.UserProperties;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.wallet.service.WalletService;
import app.web.dto.EditProfileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    // 1. Mock all dependencies
    // 2. Inject mocks into the service
    // 3. Test service methods

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private WalletService walletService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserProperties userProperties;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("When edit user details and repository returns optional empty then throws exception")
    void whenEditUserDetails_andRepositoryReturnsOptionalEmpty_thenThrowsException() {

        // Given
        UUID userId = UUID.randomUUID();
        EditProfileRequest request = null;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.updateProfile(request, userId));
    }

    @Test
    @DisplayName("When edit user details and repository returns user from the database then update the user details and save it to the database")
    void whenEditUserDetails_andRepositoryReturnsUserFromTheDatabase_thenUpdateTheUserDetailsAndSaveItToTheDatabase() {

        // Given
        UUID userId = UUID.randomUUID();
        EditProfileRequest request = EditProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .profilePicture("www.example.com")
                .build();
        User userReturnedFromRepository = User.builder()
                .id(userId)
                .firstName("Jane")
                .lastName("Smith")
                .email("janesmith@example.com")
                .profilePicture(null)
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(userReturnedFromRepository));

        // When
        userService.updateProfile(request, userId);

        // Then
        assertEquals("John", userReturnedFromRepository.getFirstName());
        assertEquals("Doe", userReturnedFromRepository.getLastName());
        assertNotNull(userReturnedFromRepository.getProfilePicture());
        assertEquals("www.example.com", userReturnedFromRepository.getProfilePicture());
        assertEquals("johndoe@example.com", userReturnedFromRepository.getEmail());
        verify(userRepository).save(userReturnedFromRepository);
    }

    @Test
    @DisplayName("When edit user details and repository returns user and dto comes with non empty email then invoke upsertNotificationPreference")
    void whenEditUserDetails_andRepositoryReturnsUserAndDtoComesWithNonEmptyEmail_thenInvokeUpsertNotificationPreferenceWithTrue() {

        // Given
        UUID userId = UUID.randomUUID();
        EditProfileRequest request = EditProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .profilePicture("www.example.com")
                .build();
        User userReturnedFromRepository = User.builder()
                .id(userId)
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(userReturnedFromRepository));

        // When
        userService.updateProfile(request, userId);

        // Then
        verify(notificationService).upsertPreference(userId, true, "johndoe@example.com");
    }

    @Test
    @DisplayName("When edit user details and repository returns user and dto comes with non empty email then invoke upsertNotificationPreference")
    void whenEditUserDetails_andRepositoryReturnsUserAndDtoComesWithEmptyEmail_thenInvokeUpsertNotificationPreferenceWithFalse() {

        // Given
        UUID userId = UUID.randomUUID();
        EditProfileRequest request = EditProfileRequest.builder()
                .email(null)
                .build();
        User userReturnedFromRepository = User.builder()
                .id(userId)
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(userReturnedFromRepository));

        // When
        userService.updateProfile(request, userId);

        // Then
        verify(notificationService).upsertPreference(userId, false, null);
    }

    @Test
    void whenSwitchRole_andRepositoryReturnsAdmin_thenUserIsUpdatedWithRoleUserAndUpdatedOnNow_andPersistedInTheDatabase() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchRole(userId);

        // Then
        assertEquals(UserRole.USER, user.getRole());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
        verify(userRepository).save(user);

    }

    @Test
    void whenSwitchRole_andRepositoryReturnsUser_thenUserIsUpdatedWithRoleAdminAndUpdatedOnNow_andPersistedInTheDatabase() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .role(UserRole.USER)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchRole(userId);

        // Then
        assertEquals(UserRole.ADMIN, user.getRole());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchRole_andRepositoryReturnsOptionalEmpty_thenThrowsException() {

        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.switchRole(userId));
    }
}