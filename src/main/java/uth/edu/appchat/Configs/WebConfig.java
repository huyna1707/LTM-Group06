package uth.edu.appchat.Configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);

            String location = root.toUri().toString();
            if (!location.endsWith("/")) location += "/";

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(location)
                    .setCachePeriod(3600)
                    .resourceChain(true);
        } catch (Exception e) {
            throw new RuntimeException("Init upload dir failed", e);
        }
    }
}
