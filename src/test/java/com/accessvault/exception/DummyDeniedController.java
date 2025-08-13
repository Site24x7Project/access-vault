package com.accessvault.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dummy")
public class DummyDeniedController {
    @GetMapping("/denied")
    public String denied() {
        throw new AccessDeniedException("Access Denied");
    }
}
