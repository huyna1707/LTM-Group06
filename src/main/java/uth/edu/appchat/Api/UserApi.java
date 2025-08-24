package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApi {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public Object me(Authentication auth) {
        return java.util.Map.of("username", auth.getName());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            String currentUsername = auth.getName();
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Map backend status to frontend status
            String frontendStatus = "active";
            if (user.getStatus() != null) {
                switch (user.getStatus()) {
                    case ONLINE:
                        frontendStatus = "active";
                        break;
                    case DND:
                        frontendStatus = "busy";
                        break;
                    case OFFLINE:
                        frontendStatus = "offline";
                        break;
                    case AWAY:
                        frontendStatus = "away";
                        break;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "username", user.getUsername(),
                    "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    "status", frontendStatus,
                    "email", user.getEmail(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get profile: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileData, Authentication auth) {
        try {
            String currentUsername = auth.getName();
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update full name (display name)
            if (profileData.containsKey("fullName")) {
                String newFullName = profileData.get("fullName").trim();
                if (!newFullName.isEmpty()) {
                    user.setFullName(newFullName);
                }
            }

            // Update avatar URL
            if (profileData.containsKey("avatarUrl")) {
                String avatarUrl = profileData.get("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    user.setAvatarUrl(avatarUrl);
                } else {
                    user.setAvatarUrl(null); // Clear avatar
                }
            }

            // Update status
            if (profileData.containsKey("status")) {
                String statusStr = profileData.get("status");
                try {
                    User.UserStatus status;
                    // Map frontend status values to backend enum
                    switch (statusStr.toLowerCase()) {
                        case "active":
                            status = User.UserStatus.ONLINE;
                            break;
                        case "busy":
                            status = User.UserStatus.DND;
                            break;
                        case "offline":
                            status = User.UserStatus.OFFLINE;
                            break;
                        case "away":
                            status = User.UserStatus.AWAY;
                            break;
                        default:
                            status = User.UserStatus.ONLINE;
                            break;
                    }
                    user.setStatus(status);
                } catch (IllegalArgumentException e) {
                    // Keep current status if invalid
                }
            }

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", Map.of(
                            "username", user.getUsername(),
                            "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                            "status", user.getStatus() != null ? user.getStatus().toString() : "ONLINE"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update profile: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/by-username/{username}")
    @ResponseBody
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return ResponseEntity.ok(Map.of(
                        "username", user.getUsername(),
                        "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        "email", user.getEmail(),
                        "status", user.getStatus() != null ? user.getStatus().toString() : "ONLINE"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get user: " + e.getMessage()
            ));
        }
    }
}