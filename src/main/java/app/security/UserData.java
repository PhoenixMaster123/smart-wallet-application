package app.security;

import app.user.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserData implements UserDetails {

    private UUID userId;

    private String username;

    private String password;

//    private List<String> permissions;

    private UserRole role;

    private boolean isAccountActive;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

//        SimpleGrantedAuthority role = new SimpleGrantedAuthority(this.role.name()); // it can be permission too
//        SimpleGrantedAuthority permission1 = new SimpleGrantedAuthority("read_all_products");
//        SimpleGrantedAuthority permission2 = new SimpleGrantedAuthority("do_transfer");
//        SimpleGrantedAuthority permission3 = new SimpleGrantedAuthority("open_new_wallet");
//
//        return List.of(role, permission1, permission2, permission3);

        // Everything is hardcoded for now

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + this.role.name());

//        List<SimpleGrantedAuthority> authorities = permissions.stream().map(SimpleGrantedAuthority::new).toList();

        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isAccountActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isAccountActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isAccountActive;
    }

    @Override
    public boolean isEnabled() {
        return this.isAccountActive;
    }
}
