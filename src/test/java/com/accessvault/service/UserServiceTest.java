package com.accessvault.service;

import com.accessvault.dto.LoginRequest;
import com.accessvault.dto.SignupRequest;
import com.accessvault.model.Role;
import com.accessvault.model.User;
import com.accessvault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    UserService service = new UserService(repo, encoder);

    @Test
    void register_encodesPassword_and_setsRole() {
        when(repo.existsByUsername("u")).thenReturn(false);
        when(encoder.encode("p")).thenReturn("ENC");

        SignupRequest req = new SignupRequest();
        req.setUsername("u"); req.setPassword("p"); req.setRole(Role.ADMIN);

        service.registerUser(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repo).save(cap.capture());
        User saved = cap.getValue();
        assertEquals("u", saved.getUsername());
        assertEquals("ENC", saved.getPassword());
        assertEquals(Role.ADMIN, saved.getRole());
    }

    @Test
    void register_duplicate_throws() {
        when(repo.existsByUsername("u")).thenReturn(true);
        SignupRequest req = new SignupRequest();
        req.setUsername("u"); req.setPassword("p");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registerUser(req));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(repo, never()).save(any());
    }

    @Test
    void authenticate_ok_returnsUser() {
        User u = User.builder().id(1L).username("u").password("ENC").role(Role.DEV).build();
        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("p", "ENC")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("p");

        Optional<User> out = service.authenticateUser(req);
        assertTrue(out.isPresent());
        assertEquals("u", out.get().getUsername());
    }

    @Test
    void authenticate_badPassword_empty() {
        User u = User.builder().id(1L).username("u").password("ENC").role(Role.DEV).build();
        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("wrong", "ENC")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("wrong");

        assertTrue(service.authenticateUser(req).isEmpty());
    }
}
