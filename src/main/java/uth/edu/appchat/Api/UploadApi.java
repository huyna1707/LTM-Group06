package uth.edu.appchat.Api;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadApi {

    private final Path rootDir;

    public UploadApi(@Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootDir.resolve("img"));
        Files.createDirectories(rootDir.resolve("video"));
        Files.createDirectories(rootDir.resolve("file"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") @NotNull MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
            }
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String base = (dot >= 0 ? original.substring(0, dot) : original)
                    .replaceAll("[^a-zA-Z0-9-_\\.]", "_");

            String contentType = (file.getContentType() == null) ? "" : file.getContentType().toLowerCase();
            String category = inferCategory(contentType, ext); // "img" | "video" | "file"
            String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS").format(LocalDateTime.now());
            String storedName = base + "_" + stamp + ext;
            Path targetDir = rootDir.resolve(category).normalize();
            Files.createDirectories(targetDir);
            Path stored = targetDir.resolve(storedName);

            try (var in = file.getInputStream()) {
                Files.copy(in, stored, StandardCopyOption.REPLACE_EXISTING);
            }
            if (contentType.isBlank()) {
                String guessed = Files.probeContentType(stored);
                if (guessed != null) contentType = guessed.toLowerCase();
            }
            if (contentType.isBlank()) contentType = "application/octet-stream";
            String publicUrl = "/uploads/" + category + "/" + storedName;

            return ResponseEntity.ok(Map.of(
                    "url", publicUrl,
                    "name", original,
                    "size", file.getSize(),
                    "contentType", contentType,
                    "category", category
            ));
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lưu file thất bại"));
        }
    }

    private static String inferCategory(String contentType, String ext) {
        ext = (ext == null) ? "" : ext.toLowerCase();
        if (contentType != null && contentType.startsWith("image/")) return "img";
        if (contentType != null && contentType.startsWith("video/")) return "video";

        // fallback theo phần mở rộng
        if (ext.matches("\\.(png|jpe?g|gif|webp|bmp|svg)$")) return "img";
        if (ext.matches("\\.(mp4|webm|ogg|mov|m4v)$")) return "video";
        return "file";
    }
}
