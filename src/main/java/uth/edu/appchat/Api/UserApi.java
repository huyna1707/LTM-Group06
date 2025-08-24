//package uth.edu.appchat.Api;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//import uth.edu.appchat.Models.User;
//import uth.edu.appchat.Repositories.UserRepository;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/users")
//@RequiredArgsConstructor
//public class UserApi {
//
//    private final UserRepository userRepository;
//
//    @GetMapping("/me")
//    public Object me(Authentication auth) {
//        return java.util.Map.of("username", auth.getName());
//    }
//
//    @PutMapping("/profile")
//    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileData, Authentication auth) {
//        try {
//            String currentUsername = auth.getName();
//            User user = userRepository.findByUsername(currentUsername)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            // Update full name (display name)
//            if (profileData.containsKey("fullName")) {
//                String newFullName = profileData.get("fullName").trim();
//                if (!newFullName.isEmpty()) {
//                    user.setFullName(newFullName);
//                }
//            }
//
//            // Update status
//            if (profileData.containsKey("status")) {
//                String statusStr = profileData.get("status");
//                try {
//                    User.UserStatus status = User.UserStatus.valueOf(statusStr.toUpperCase());
//                    user.setStatus(status);
//                } catch (IllegalArgumentException e) {
//                    // Keep current status if invalid
//                }
//            }
//
//            userRepository.save(user);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Profile updated successfully",
//                    "user", Map.of(
//                            "username", user.getUsername(),
//                            "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
//                            "status", user.getStatus() != null ? user.getStatus().toString() : "ONLINE"
//                    )
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Failed to update profile: " + e.getMessage()
//            ));
//        }
//    }
//
//    @GetMapping("/by-username/{username}")
//    @ResponseBody
//    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
//        try {
//            User user = userRepository.findByUsername(username).orElse(null);
//            if (user != null) {
//                return ResponseEntity.ok(Map.of(
//                        "username", user.getUsername(),
//                        "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
//                        "email", user.getEmail(),
//                        "status", user.getStatus() != null ? user.getStatus().toString() : "ONLINE"
//                ));
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "error", "Failed to get user: " + e.getMessage()
//            ));
//        }
//    }
//}