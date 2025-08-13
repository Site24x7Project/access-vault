package com.accessvault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/admin")
    public ResponseEntity<String> testAdminAccess() {
        return ResponseEntity.ok("Hello Admin – You are authorized.");
    }
    @GetMapping("/dev")
    public ResponseEntity<String> testDevAccess() {
        return ResponseEntity.ok("Hello Dev – You are authorized.");
    }

    @GetMapping("/ops")
    public ResponseEntity<String> testOpsAccess() {
        return ResponseEntity.ok("Hello Ops – You are authorized.");
    }

}
