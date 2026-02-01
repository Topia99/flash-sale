package com.flashSale.auth.controller;

import com.flashSale.auth.dto.LoginRequest;
import com.flashSale.auth.dto.LoginResponse;
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

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req){
        var jwt = auth.login(req);

        long expiresIn = jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond();
        return new LoginResponse(jwt.getTokenValue(), "Bearer", expiresIn);
    }
}
