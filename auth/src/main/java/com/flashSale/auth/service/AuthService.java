package com.flashSale.auth.service;

import com.flashSale.auth.domain.User;
import com.flashSale.auth.dto.RegisterRequest;
import com.flashSale.auth.exception.ConflictException;
import com.flashSale.auth.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder){
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public User register(RegisterRequest req) {
        // 先做快速存在性检查（更友好错误信息）
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("USERNAME_TAKEN", "username already exists");
        }
        if (req.email() != null && !req.email().isBlank() && users.existsByEmail(req.email())) {
            throw new ConflictException("EMAIL_TAKEN", "email already exists");
        }

        var u = new User();
        u.setUsername(req.username());
        u.setEmail((req.email() == null || req.email().isBlank()) ? null : req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole("USER");
        u.setStatus((byte) 1);

        try {
            return users.save(u);
        } catch (DataIntegrityViolationException e) {
            // 防并发双写：最终仍以 DB unique 为准
            throw new ConflictException("DUPLICATE", "duplicate username/email");
        }
    }
}
