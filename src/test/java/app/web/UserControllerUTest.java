package app.web;

import app.user.model.User;
import app.user.service.UserService;
import app.web.controller.UserController;
import app.web.dto.EditProfileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerUTest {

    @Mock
    private UserService userService;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private UserController userController;

    private static EditProfileRequest request() {
        return EditProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .profilePicture("https://example.com/avatar.png")
                .build();
    }

    @Test
    @DisplayName("An invalid profile edit returns the form and is never saved")
    void whenUpdateProfile_andValidationFails_thenTheFormIsReturnedAndNothingIsSaved() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("Vik1234").build();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.getById(userId)).thenReturn(user);

        // When
        ModelAndView result = userController.updateProfile(request(), userId, bindingResult);

        // Then
        assertEquals("profile-menu", result.getViewName());
        assertEquals(user, result.getModel().get("user"));
        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("A valid profile edit is saved and redirects home")
    void whenUpdateProfile_andValidationPasses_thenTheProfileIsSavedAndRedirected() {

        // Given
        UUID userId = UUID.randomUUID();
        EditProfileRequest editProfileRequest = request();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        ModelAndView result = userController.updateProfile(editProfileRequest, userId, bindingResult);

        // Then
        assertEquals("redirect:/home", result.getViewName());
        verify(userService).updateProfile(editProfileRequest, userId);
    }

    @Test
    void whenGetProfilePage_thenTheFormIsPrefilledFromTheUser() {

        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("Vik1234")
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .build();
        when(userService.getById(userId)).thenReturn(user);

        // When
        ModelAndView result = userController.getProfilePage(userId);

        // Then
        assertEquals("profile-menu", result.getViewName());
        assertEquals(user, result.getModel().get("user"));

        EditProfileRequest prefilled = (EditProfileRequest) result.getModel().get("editProfileRequest");
        assertEquals("John", prefilled.getFirstName());
        assertEquals("Doe", prefilled.getLastName());
        assertEquals("johndoe@example.com", prefilled.getEmail());
    }

    @Test
    void whenGetAllUsersPage_thenEveryUserIsPassedToTheView() {

        // Given
        List<User> users = List.of(
                User.builder().id(UUID.randomUUID()).build(),
                User.builder().id(UUID.randomUUID()).build());
        when(userService.getAll()).thenReturn(users);

        // When
        ModelAndView result = userController.getAllUsersPage();

        // Then
        assertEquals("users", result.getViewName());
        assertEquals(users, result.getModel().get("users"));
    }

    @Test
    @DisplayName("The status handler switches status, not role")
    void whenSwitchUserStatus_thenOnlyTheStatusIsSwitched() {

        // Given
        UUID userId = UUID.randomUUID();

        // When
        String view = userController.switchUserStatus(userId);

        // Then
        assertEquals("redirect:/users", view);
        verify(userService).switchStatus(userId);
        verify(userService, never()).switchRole(any());
    }

    @Test
    @DisplayName("The role handler switches role, not status")
    void whenSwitchUserRole_thenOnlyTheRoleIsSwitched() {

        // Given
        UUID userId = UUID.randomUUID();

        // When
        String view = userController.switchUserRole(userId);

        // Then
        assertEquals("redirect:/users", view);
        verify(userService).switchRole(userId);
        verify(userService, never()).switchStatus(any());
    }

    @Test
    @DisplayName("The per-user endpoints are guarded, so an id alone is not enough to reach them")
    void profileEndpointsCarryAnAuthorizationRule() throws NoSuchMethodException {

        // Given & When
        String getRule = UserController.class
                .getMethod("getProfilePage", UUID.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class)
                .value();
        String putRule = UserController.class
                .getMethod("updateProfile", EditProfileRequest.class, UUID.class, BindingResult.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class)
                .value();

        // Then
        assertThat(getRule).isEqualTo("#id == principal.userId or hasRole('ADMIN')");
        assertThat(putRule).isEqualTo("#id == principal.userId or hasRole('ADMIN')");
    }
}
