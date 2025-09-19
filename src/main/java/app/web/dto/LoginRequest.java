package app.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(min = 6, max = 20, message = "Username must be between 6 and 20 characters")
        String username,

        @NotBlank
        @Size(min = 6, max = 6, message = "Password must be exactly 6 characters")
        String password
) {}
