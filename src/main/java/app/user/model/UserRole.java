package app.user.model;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("User"),
    ADMIN("Admin");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }
}
