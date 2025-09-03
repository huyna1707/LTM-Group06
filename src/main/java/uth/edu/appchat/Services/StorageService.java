package uth.edu.appchat.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir; // gốc là thư mục "uploads"

    public String saveWallpaper(MultipartFile file) throws IOException {
        // thư mục con wallpapers nằm trong uploads
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve("wallpapers");
        Files.createDirectories(dir);

        // lấy phần mở rộng (jpg/png/webp…)
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.') + 1))
                .orElse("jpg");

        // đặt tên file random
        String name = UUID.randomUUID().toString() + "." + ext.toLowerCase();

        // đường dẫn lưu thực tế
        Path dest = dir.resolve(name);
        file.transferTo(dest.toFile());

        // URL public cho frontend (WebConfig map /uploads/** → thư mục uploads)
        return "/uploads/wallpapers/" + name;
    }
}
