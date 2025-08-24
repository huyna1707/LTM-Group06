package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Configs.JwtService;
import uth.edu.appchat.Dtos.LoginRequest;
import uth.edu.appchat.Dtos.JwtResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), request.getPassword())
            );

            if (authentication.isAuthenticated()) {
                String token = jwtService.generate(request.getUsernameOrEmail());
                return ResponseEntity.ok(new JwtResponse(token));
            }

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        }

        return ResponseEntity.status(401).body("Không xác thực được");
    }
}
