package com.example.qidian.web.login;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.qidian.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthContoller {

    private final AuthService authService;

    public AuthContoller(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public String logout() {
        return "logout";
    }

    @PostMapping("/refresh")
    public String refresh() {
        return "refresh";
    }
}
