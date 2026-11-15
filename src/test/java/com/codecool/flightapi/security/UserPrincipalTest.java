package com.codecool.flightapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.user.Role;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.User;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserPrincipalTest {

    @Test
    @DisplayName("built from a user: id, names, password hash and one authority per role")
    void buildFromUser() {
        User user = new User(
                7L,
                "Ana",
                "ana",
                "{bcrypt}hash",
                "ana@example.com",
                Set.of(new Role(1, RoleName.ROLE_USER), new Role(2, RoleName.ROLE_ADMIN)));

        UserPrincipal principal = UserPrincipal.build(user);

        assertThat(principal.getId()).isEqualTo(7L);
        assertThat(principal.getUsername()).isEqualTo("ana");
        assertThat(principal.getEmail()).isEqualTo("ana@example.com");
        assertThat(principal.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("principals compare by user id only")
    void comparesByUserId() {
        UserPrincipal ana = new UserPrincipal(1L, "ana", "ana@example.com", "x", List.of());
        UserPrincipal sameInstance = ana; // AssertJ short-circuits on ==

        assertThat(ana.equals(sameInstance)).isTrue();
        assertThat(ana)
                .isEqualTo(new UserPrincipal(1L, "renamed", "other@example.com", "y", List.of()))
                .hasSameHashCodeAs(new UserPrincipal(1L, "renamed", "other@example.com", "y", List.of()))
                .isNotEqualTo(new UserPrincipal(2L, "ana", "ana@example.com", "x", List.of()))
                .isNotEqualTo("ana");
    }
}
