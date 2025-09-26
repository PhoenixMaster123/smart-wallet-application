package app.web.dto;

import app.user.model.Country;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


// Notes: records are immutable
//@Builder
//public record RegisterRequest(
//    @NotBlank
//    @Size(min = 6, max = 20, message = "Username must be between 6 and 20 characters")
//    String username,
//    @NotBlank
//    @Size(min = 6, max = 6, message = "Password must be exactly 6 characters")
//    String password,
//    @NotNull
//    Country country
//) {}

// Note: records are with a full argument constructor by default.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(min = 6, max = 20, message = "Username must be between 6 and 20 characters")
    private String username;

    @NotBlank
    @Size(min = 6, max = 6, message = "Password must be exactly 6 characters")
    private String password;

    @NotNull
    private Country country;
}
