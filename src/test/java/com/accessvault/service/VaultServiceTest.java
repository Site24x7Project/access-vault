package com.accessvault.service;

import com.accessvault.dto.SecretRequest;
import com.accessvault.dto.SecretResponse;
import com.accessvault.model.VaultSecret;
import com.accessvault.repository.VaultSecretRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VaultServiceTest {

    VaultSecretRepository repo = mock(VaultSecretRepository.class);
    VaultService service = new VaultService(repo);

    @BeforeEach
    void setAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "devtest3", null, List.of(new SimpleGrantedAuthority("DEV"))));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addSecret_valid_savesWithOwner() {
        when(repo.existsByKeyNameAndOwnerUsername(eq("stripe_api"), eq("devtest3"))).thenReturn(false);

        SecretRequest req = new SecretRequest();
        req.setKeyName("stripe_api");
        req.setSecretValue("sk_live_123456");

        service.addSecret(req);

        ArgumentCaptor<VaultSecret> cap = ArgumentCaptor.forClass(VaultSecret.class);
        verify(repo).save(cap.capture());
        VaultSecret saved = cap.getValue();
        assertEquals("stripe_api", saved.getKeyName());
        assertEquals("sk_live_123456", saved.getSecretValue());
        assertEquals("devtest3", saved.getOwnerUsername());
    }

    @Test
    void addSecret_duplicate_throws() {
        when(repo.existsByKeyNameAndOwnerUsername(eq("stripe_api"), eq("devtest3"))).thenReturn(true);

        SecretRequest req = new SecretRequest();
        req.setKeyName("stripe_api");
        req.setSecretValue("x");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.addSecret(req));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(repo, never()).save(any());
    }

    @Test
    void addSecret_invalid_throws() {
        SecretRequest req = new SecretRequest();
        req.setKeyName("  ");
        req.setSecretValue("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.addSecret(req));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be empty"));
        verify(repo, never()).save(any());
    }

    @Test
    void getMySecrets_masksCorrectly() {
        VaultSecret s1 = new VaultSecret();
        s1.setId(1L); s1.setKeyName("k1"); s1.setSecretValue("abcd"); s1.setOwnerUsername("devtest3");

        VaultSecret s2 = new VaultSecret();
        s2.setId(2L); s2.setKeyName("k2"); s2.setSecretValue("abcdef123456"); s2.setOwnerUsername("devtest3");

        when(repo.findByOwnerUsername(eq("devtest3"))).thenReturn(List.of(s1, s2));

        List<SecretResponse> out = service.getMySecrets();
        assertThat(out, hasSize(2));
        assertThat(out.get(0).getMaskedSecret(), is("****"));          // <=4 case
        assertTrue(out.get(1).getMaskedSecret().endsWith("3456"));
        assertTrue(out.get(1).getMaskedSecret().startsWith("****"));

    }

    @Test
    void deleteSecret_own_ok() {
        VaultSecret s = new VaultSecret();
        s.setId(10L); s.setOwnerUsername("devtest3");
        when(repo.findByIdAndOwnerUsername(10L, "devtest3")).thenReturn(Optional.of(s));

        String msg = service.deleteSecret(10L);
        assertTrue(msg.toLowerCase().contains("deleted"));
        verify(repo, times(1)).deleteById(10L);
    }

    @Test
    void deleteSecret_notOwned_throwsSecurityException() {
        when(repo.findByIdAndOwnerUsername(99L, "devtest3")).thenReturn(Optional.empty());
        SecurityException ex = assertThrows(SecurityException.class, () -> service.deleteSecret(99L));
        assertTrue(ex.getMessage().toLowerCase().contains("not authorized"));
        verify(repo, never()).deleteById(anyLong());
    }
}
