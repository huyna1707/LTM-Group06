package uth.edu.appchat.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Path ROOT = Paths.get("uploads");

    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String mime = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String ext = getExtension(original);
        String sub = resolveSubFolder(mime, ext);

        Path dir = ROOT.resolve(sub);
        Files.createDirectories(dir);
        String newName = UUID.randomUUID().toString().replace("-", "");
        if (!ext.isEmpty()) newName += "." + ext;
        Path dest = dir.resolve(newName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        Map<String,Object> res = new HashMap<>();
        res.put("url", "/uploads/" + sub + "/" + newName);
        res.put("type", sub.equals("images") ? "image" : sub.equals("videos") ? "video" : "file");
        res.put("name", sanitize(original));
        res.put("size", file.getSize());
        return res;
    }

    private static String resolveSubFolder(String mime, String ext) {
        if (mime.startsWith("image/")) return "images";
        if (mime.startsWith("video/")) return "videos";

        String e = ext.toLowerCase();
        Set<String> imgExt = Set.of("jpg","jpeg","png","gif","webp","bmp","svg","heic","heif","avif");
        Set<String> vidExt = Set.of("mp4","webm","mov","m4v","avi","mkv","mpeg","mpg");
        if (imgExt.contains(e)) return "images";
        if (vidExt.contains(e)) return "videos";
        return "files";
    }

    private static String getExtension(String name) {
        String s = name;
        int idx = s.lastIndexOf('.');
        if (idx == -1 || idx == s.length() - 1) return "";
        return sanitize(s.substring(idx + 1));
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
