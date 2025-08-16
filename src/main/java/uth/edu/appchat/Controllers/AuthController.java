package uth.edu.appchat.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import uth.edu.appchat.Configs.JwtService;
import uth.edu.appchat.Dtos.AuthDtos.*;
import uth.edu.appchat.Dtos.AuthDtos.AuthResponse;
import uth.edu.appchat.Dtos.AuthDtos.LoginRequest;
import uth.edu.appchat.Dtos.AuthDtos.RegisterRequest;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Services.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(UserService userService, AuthenticationManager authManager, JwtService jwtService) {
        this.userService = userService;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
            try {
                User u = userService.register(req);
                return ResponseEntity.ok().body(
                        java.util.Map.of(
                                "id", u.getId(),
                                "username", u.getUsername(),
                                "email", u.getEmail()
                        )
                );
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(java.util.Map.of("error", "Đăng ký thất bại"));
            }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
            try {
                Authentication auth = authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword())
                );
                String token = jwtService.generate(auth.getName());
                return ResponseEntity.ok(new AuthResponse(token, auth.getName()));
            } catch (BadCredentialsException ex) {
                return ResponseEntity.status(401).body(java.util.Map.of("error", "Sai tài khoản hoặc mật khẩu"));
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(java.util.Map.of("error", "Đăng nhập thất bại"));
            }
    }
}
