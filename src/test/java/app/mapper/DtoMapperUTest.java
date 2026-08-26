package app.mapper;

import app.user.model.User;
import app.web.dto.EditProfileRequest;
import app.web.mapper.DtoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DtoMapperUTest {

    @Test
    void fromUserToEditProfileRequest_whenPassUser_thenReturnEditProfileRequestWithSameData() {

        // Given
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .profilePicture("www.example.com")
                .build();

        // When
        EditProfileRequest request = DtoMapper.fromUser(user);

        // Then
        assertEquals("John", request.getFirstName());
        assertEquals("Doe", request.getLastName());
        assertEquals("johndoe@example.com", request.getEmail());
        assertEquals("www.example.com", request.getProfilePicture());
    }
}
