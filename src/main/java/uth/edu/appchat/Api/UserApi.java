package uth.edu.appchat.Api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApi {

    private final UserRepository userRepository;
    private static final String DEFAULT_AVATAR = "/images/defaultAvt.jpg";

    // ==== Email & Phone validators ====
    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_RE.matcher(email).matches();
    }
    /** Chuẩn hoá số: giữ lại chữ số và dấu + ở đầu (nếu có), bỏ khoảng trắng/ký tự ngăn cách */
    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return "";
        // Giữ + nếu ở đầu, còn lại lấy digit
        StringBuilder sb = new StringBuilder();
        int i = 0;
        if (raw.charAt(0) == '+') { sb.append('+'); i = 1; }
        for (; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }
    /** Ràng buộc độ dài số điện thoại (rộng tay 8..15 chữ số, không tính dấu +) */
    private static boolean isValidPhone(String normalized) {
        if (normalized == null) return false;
        String digits = normalized.startsWith("+") ? normalized.substring(1) : normalized;
        return digits.length() >= 8 && digits.length() <= 15;
    }

    @GetMapping("/me")
    public Object me(Authentication auth) {
        return Map.of("username", auth.getName());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        try {
            String currentUsername = auth.getName();
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String frontendStatus = "active";
            if (user.getStatus() != null) {
                switch (user.getStatus()) {
                    case ONLINE -> frontendStatus = "active";
                    case DND    -> frontendStatus = "busy";
                    case OFFLINE-> frontendStatus = "offline";
                    case AWAY   -> frontendStatus = "away";
                }
            }

            String avatar = (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank())
                    ? user.getAvatarUrl()
                    : DEFAULT_AVATAR;

            return ResponseEntity.ok(Map.of(
                    "username", user.getUsername(),
                    "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    "status",   frontendStatus,
                    "email",    user.getEmail(),             // <-- thêm
                    "phone",    user.getPhone(),             // <-- thêm
                    "avatarUrl", avatar
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get profile: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileData, Authentication auth) {
        try {
            String currentUsername = auth.getName();
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // full name
            if (profileData.containsKey("fullName")) {
                String newFullName = profileData.get("fullName");
                if (newFullName != null && !newFullName.trim().isEmpty()) {
                    user.setFullName(newFullName.trim());
                }
            }

            // email (rỗng => xoá/đặt null; có giá trị => validate)
            if (profileData.containsKey("email")) {
                String email = profileData.get("email");
                email = (email == null) ? null : email.trim();
                if (email == null || email.isEmpty()) {
                    user.setEmail(null);
                } else {
                    if (!isValidEmail(email)) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "message", "Email không hợp lệ"
                        ));
                    }
                    user.setEmail(email);
                }
            }

            // phone (rỗng => xoá/đặt null; có giá trị => normalize + validate)
            if (profileData.containsKey("phone")) {
                String phoneRaw = profileData.get("phone");
                phoneRaw = (phoneRaw == null) ? null : phoneRaw.trim();
                if (phoneRaw == null || phoneRaw.isEmpty()) {
                    user.setPhone(null);
                } else {
                    String normalized = normalizePhone(phoneRaw);
                    if (!isValidPhone(normalized)) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "message", "Số điện thoại không hợp lệ"
                        ));
                    }
                    user.setPhone(normalized);
                }
            }

            // avatar (rỗng/null => ảnh mặc định)
            if (profileData.containsKey("avatarUrl")) {
                String avatarUrl = profileData.get("avatarUrl");
                if (avatarUrl == null || avatarUrl.isBlank() || "null".equalsIgnoreCase(avatarUrl)) {
                    user.setAvatarUrl(DEFAULT_AVATAR);
                } else {
                    user.setAvatarUrl(avatarUrl);
                }
            }

            // status map
            if (profileData.containsKey("status")) {
                String statusStr = String.valueOf(profileData.get("status")).toLowerCase();
                User.UserStatus status = switch (statusStr) {
                    case "busy"    -> User.UserStatus.DND;
                    case "offline" -> User.UserStatus.OFFLINE;
                    case "away"    -> User.UserStatus.AWAY;
                    default        -> User.UserStatus.ONLINE;
                };
                user.setStatus(status);
            }

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", Map.of(
                            "username", user.getUsername(),
                            "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                            "status",   user.getStatus() != null ? user.getStatus().toString() : "ONLINE",
                            "email",    user.getEmail(),                               // <-- thêm
                            "phone",    user.getPhone(),                               // <-- thêm
                            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : DEFAULT_AVATAR
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update profile: " + e.getMessage()
            ));
        }
    }

    // API xoá avatar rõ ràng
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication auth) {
        try {
            String currentUsername = auth.getName();
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setAvatarUrl(DEFAULT_AVATAR);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", DEFAULT_AVATAR,
                    "message", "Avatar reset to default"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Delete avatar failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/by-username/{username}")
    @ResponseBody
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            String statusStr = user.getStatus() != null ? user.getStatus().toString() : "ONLINE";
            return ResponseEntity.ok(Map.of(
                    "username",  user.getUsername(),
                    "fullName",  user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    "email",     user.getEmail(),
                    "phone",     user.getPhone(),                 // <-- thêm
                    "status",    statusStr,
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get user: " + e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                          Authentication auth) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
            }
            if (file.getSize() > 2 * 1024 * 1024) { // 2MB
                return ResponseEntity.status(413).body(Map.of("error", "Ảnh quá lớn (>2MB)"));
            }
            String ct = file.getContentType();
            if (ct == null || !ct.matches("image\\/(png|jpe?g|gif|webp|bmp|svg\\+xml)")) {
                return ResponseEntity.status(415).body(Map.of("error", "Định dạng ảnh không hợp lệ"));
            }

            java.nio.file.Path folder = java.nio.file.Paths.get("uploads", "avatars");
            java.nio.file.Files.createDirectories(folder);

            String currentUsername = auth.getName();
            String ext = switch (ct) {
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "image/bmp" -> ".bmp";
                case "image/svg+xml" -> ".svg";
                default -> ".img";
            };
            String filename = "avatar-" + currentUsername + "-" + System.currentTimeMillis() + ext;

            java.nio.file.Path dest = folder.resolve(filename);
            try (var in = file.getInputStream()) {
                java.nio.file.Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/avatars/" + filename;

            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAvatarUrl(url);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload thất bại: " + e.getMessage()));
        }
    }
}
