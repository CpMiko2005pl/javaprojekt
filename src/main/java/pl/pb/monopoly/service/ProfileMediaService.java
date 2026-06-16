package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProfileMediaService {

    private static final Path UPLOADS = Path.of("./data/uploads");

    private static final Path PRIVATE = Path.of("./data/private");
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final long MAX_BANNER_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private static final Set<String> ALLOWED = EXT_BY_CONTENT_TYPE.keySet();

    public String saveAvatar(String username, MultipartFile file, String publicBaseUrl) throws IOException {
        return save(username, file, "avatars", MAX_AVATAR_BYTES, publicBaseUrl);
    }

    public String saveBanner(String username, MultipartFile file, String publicBaseUrl) throws IOException {
        return save(username, file, "banners", MAX_BANNER_BYTES, publicBaseUrl);
    }

    public String saveBackground(String username, MultipartFile file, String publicBaseUrl) throws IOException {
        return save(username, file, "backgrounds", MAX_BANNER_BYTES, publicBaseUrl);
    }

    public void deleteBackground(String username) throws IOException {
        deleteInDir("backgrounds", username);
    }

    private static final long MAX_DOC_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> DOC_EXT_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf"
    );

    public String saveVerificationDoc(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nie wybrano pliku.");
        }
        if (file.getSize() > MAX_DOC_BYTES) {
            throw new IllegalArgumentException("Plik jest za duży (maks. 10 MB).");
        }
        String contentType = file.getContentType();
        String ext = contentType == null ? null : DOC_EXT_BY_TYPE.get(contentType.toLowerCase(Locale.ROOT));
        if (ext == null) {
            throw new IllegalArgumentException("Dozwolone formaty: JPG, PNG, WEBP, PDF.");
        }
        Path dir = PRIVATE.resolve("verification");
        Files.createDirectories(dir);
        deletePrivateVerification(username);
        Path target = dir.resolve(safeFilename(username, ext));
        file.transferTo(target.toAbsolutePath());
        return "verification/" + safeFilename(username, ext);
    }

    private void deletePrivateVerification(String username) throws IOException {
        Path dir = PRIVATE.resolve("verification");
        if (!Files.isDirectory(dir)) {
            return;
        }
        String prefix = safeBasename(username) + ".";
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    public Path resolveUpload(String relativePath) {
        return PRIVATE.resolve(relativePath).toAbsolutePath().normalize();
    }

    public void deleteAvatar(String username) throws IOException {
        deleteInDir("avatars", username);
    }

    public void deleteBanner(String username) throws IOException {
        deleteInDir("banners", username);
    }

    private String save(String username, MultipartFile file, String subdir,
                        long maxBytes, String publicBaseUrl) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nie wybrano pliku.");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Plik jest za duży (maks. " + (maxBytes / 1024 / 1024) + " MB).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Dozwolone formaty: JPG, PNG, WEBP, GIF.");
        }
        String ext = EXT_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT));
        Path dir = UPLOADS.resolve(subdir);
        Files.createDirectories(dir);
        deleteInDir(subdir, username);
        Path target = dir.resolve(safeFilename(username, ext));
        file.transferTo(target.toAbsolutePath());
        return mediaUrl(publicBaseUrl, subdir, username, ext);
    }

    private void deleteInDir(String subdir, String username) throws IOException {
        Path dir = UPLOADS.resolve(subdir);
        if (!Files.isDirectory(dir)) {
            return;
        }
        String prefix = safeBasename(username) + ".";
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static String mediaUrl(String publicBaseUrl, String subdir, String username, String ext) {
        String base = publicBaseUrl != null ? publicBaseUrl.strip() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isBlank()) {
            return "/media/" + subdir + "/" + safeFilename(username, ext);
        }
        return base + "/media/" + subdir + "/" + safeFilename(username, ext);
    }

    private static String safeFilename(String username, String ext) {
        return safeBasename(username) + "." + ext;
    }

    private static String safeBasename(String username) {
        String safe = username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (safe.isBlank()) {
            throw new IllegalArgumentException("Nieprawidłowy login.");
        }
        return safe;
    }
}
