package uth.edu.appchat.Services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uth.edu.appchat.Dtos.AuthDtos.RegisterRequest;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setFullName(req.getFullName());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        return userRepo.save(u);
    }

    public User getByUsernameOrEmail(String key) {
        return userRepo.findByUsername(key).or(
                () -> userRepo.findByEmail(key)).orElse(null);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
