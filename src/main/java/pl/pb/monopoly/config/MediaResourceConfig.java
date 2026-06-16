package pl.pb.monopoly.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer, ApplicationRunner {

    private static final Path UPLOADS = Path.of("./data/uploads");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + UPLOADS.toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Files.createDirectories(UPLOADS.resolve("avatars"));
        Files.createDirectories(UPLOADS.resolve("banners"));
    }
}
