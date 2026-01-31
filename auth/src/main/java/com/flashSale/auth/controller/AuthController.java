package com.flashSale.auth.controller;

import com.flashSale.auth.dto.RegisterRequest;
import com.flashSale.auth.dto.UserResponse;
import com.flashSale.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        var u = auth.register(req);
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }
}
