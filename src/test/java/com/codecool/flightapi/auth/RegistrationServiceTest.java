package com.codecool.flightapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.user.AdminAccountProperties;
import com.codecool.flightapi.user.Role;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.RoleRepository;
import com.codecool.flightapi.user.User;
import com.codecool.flightapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    private final RegisterRequest request = new RegisterRequest("alice", "Alice", "alice@example.com", "Secret123!");

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository, roleRepository, passwordEncoder, new AdminAccountProperties("admin", "", ""));
    }

    @Test
    void registersPlainUserWithEncodedPassword() {
        when(roleRepository.getByName(RoleName.ROLE_USER)).thenReturn(new Role(1, RoleName.ROLE_USER));
        when(passwordEncoder.encode("Secret123!")).thenReturn("{bcrypt}hash");

        registrationService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(saved.getValue().getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_USER);
        verify(roleRepository, never()).getByName(RoleName.ROLE_ADMIN);
    }

    @Test
    void storesUsernameAndEmailInLowerCase() {
        when(roleRepository.getByName(RoleName.ROLE_USER)).thenReturn(new Role(1, RoleName.ROLE_USER));

        registrationService.register(new RegisterRequest("Alice", "Alice", "Alice@Example.com", "Secret123!"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).existsByUsername("alice");
        verify(userRepository).existsByEmail("alice@example.com");
    }

    @Test
    void rejectsTheAdminUsernameEvenWhenNoAdminExistsYet() {
        assertThatThrownBy(() -> registrationService.register(
                        new RegisterRequest("Admin", "Mallory", "mallory@example.com", "Secret123!")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Username is already taken.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsTakenUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Username is already taken.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsTakenEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("E-mail is already registered.");
        verify(userRepository, never()).save(any());
    }
}
