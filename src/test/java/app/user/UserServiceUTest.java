package app.user;

import app.notification.service.NotificationService;
import app.security.UserData;
import app.subscription.model.Subscription;
import app.subscription.service.SubscriptionService;
import app.user.model.Country;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.property.UserProperties;
import app.user.repository.UserRepository;
import app.user.service.UserService;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.EditProfileRequest;
import app.web.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    /* ---------------------------------------------------------------- register */

    @Test
    @DisplayName("Registering an unused username creates the user with a wallet and a subscription")
    void whenRegister_andUsernameIsFree_thenUserIsCreatedWithDefaultWalletAndSubscription() {

        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("Vik1234")
                .password("123123")
                .country(Country.BULGARIA)
                .build();

        Wallet defaultWallet = Wallet.builder().id(UUID.randomUUID()).build();
        Subscription defaultSubscription = Subscription.builder().id(UUID.randomUUID()).build();

        when(userRepository.findByUsername("Vik1234")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.createDefaultWallet(any(User.class))).thenReturn(defaultWallet);
        when(subscriptionService.createDefaultSubscription(any(User.class))).thenReturn(defaultSubscription);

        // When
        User result = userService.register(request);

        // Then
        assertEquals("Vik1234", result.getUsername());
        assertEquals(UserRole.USER, result.getRole());
        assertEquals(Country.BULGARIA, result.getCountry());
        assertThat(result.isActive()).isTrue();
        assertEquals(List.of(defaultWallet), result.getWallets());
        assertEquals(List.of(defaultSubscription), result.getSubscriptions());
        assertThat(result.getCreatedOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("The raw password is never stored")
    void whenRegister_thenThePasswordIsStoredEncoded() {

        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("Vik1234")
                .password("123123")
                .country(Country.BULGARIA)
                .build();

        when(userRepository.findByUsername("Vik1234")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.createDefaultWallet(any(User.class))).thenReturn(Wallet.builder().build());
        when(subscriptionService.createDefaultSubscription(any(User.class)))
                .thenReturn(Subscription.builder().build());

        // When
        User result = userService.register(request);

        // Then
        assertEquals("hashed-password", result.getPassword());
        assertThat(result.getPassword()).isNotEqualTo("123123");
    }

    @Test
    @DisplayName("A new registration starts with notifications switched off")
    void whenRegister_thenNotificationPreferenceIsCreatedDisabled() {

        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("Vik1234")
                .password("123123")
                .country(Country.BULGARIA)
                .build();

        when(userRepository.findByUsername("Vik1234")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.createDefaultWallet(any(User.class))).thenReturn(Wallet.builder().build());
        when(subscriptionService.createDefaultSubscription(any(User.class)))
                .thenReturn(Subscription.builder().build());

        // When
        User result = userService.register(request);

        // Then
        verify(notificationService).upsertPreference(result.getId(), false, null);
    }

    @Test
    void whenRegister_andUsernameIsTaken_thenThrowsExceptionAndNothingIsCreated() {

        // Given
        RegisterRequest request = RegisterRequest.builder()
                .username("Vik1234")
                .password("123123")
                .country(Country.BULGARIA)
                .build();
        when(userRepository.findByUsername("Vik1234"))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).build()));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(walletService, never()).createDefaultWallet(any(User.class));
        verify(subscriptionService, never()).createDefaultSubscription(any(User.class));
    }

    /* ------------------------------------------------------------ switchStatus */

    @Test
    @DisplayName("Switching status flips an active user to inactive and stamps updatedOn")
    void whenSwitchStatus_andUserIsActive_thenUserBecomesInactive() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).active(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchStatus(userId);

        // Then
        assertThat(user.isActive()).isFalse();
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchStatus_andUserIsInactive_thenUserBecomesActive() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).active(false).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.switchStatus(userId);

        // Then
        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchStatus_andRepositoryReturnsOptionalEmpty_thenThrowsException() {

        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.switchStatus(userId));
    }

    /* ------------------------------------------------------ loadUserByUsername */

    @Test
    @DisplayName("Spring Security is handed the id, password, role and enabled flag")
    void whenLoadUserByUsername_andTheUserExists_thenUserDataCarriesTheAccountDetails() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("Vik1234")
                .password("hashed-password")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        when(userRepository.findByUsername("Vik1234")).thenReturn(Optional.of(user));

        // When
        UserDetails result = userService.loadUserByUsername("Vik1234");

        // Then
        assertThat(result).isInstanceOf(UserData.class);
        UserData userData = (UserData) result;
        assertEquals(userId, userData.getUserId());
        assertEquals("Vik1234", userData.getUsername());
        assertEquals("hashed-password", userData.getPassword());
        assertEquals(UserRole.ADMIN, userData.getRole());
        assertThat(userData.isEnabled()).isTrue();
        assertThat(userData.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("A deactivated account is loaded but reports itself disabled")
    void whenLoadUserByUsername_andTheAccountIsInactive_thenItIsNotEnabled() {

        // Given
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("Vik1234")
                .password("hashed-password")
                .role(UserRole.USER)
                .active(false)
                .build();
        when(userRepository.findByUsername("Vik1234")).thenReturn(Optional.of(user));

        // When
        UserDetails result = userService.loadUserByUsername("Vik1234");

        // Then
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.isAccountNonLocked()).isFalse();
    }

    @Test
    void whenLoadUserByUsername_andTheUserIsUnknown_thenThrowsUsernameNotFoundException() {

        // Given
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("ghost"));
    }

    /* ------------------------------------------------------------------ getAll */

    @Test
    void whenGetAll_thenTheRepositoryResultIsReturned() {

        // Given
        List<User> users = List.of(
                User.builder().id(UUID.randomUUID()).build(),
                User.builder().id(UUID.randomUUID()).build());
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAll();

        // Then
        assertEquals(2, result.size());
        assertEquals(users, result);
    }

    @Test
    void whenGetById_andTheUserIsMissing_thenThrowsException() {

        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.getById(userId));
    }
}
