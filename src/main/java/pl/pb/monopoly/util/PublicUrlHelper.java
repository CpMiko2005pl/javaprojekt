package pl.pb.monopoly.util;

import jakarta.servlet.http.HttpServletRequest;

public final class PublicUrlHelper {

    private PublicUrlHelper() {
    }

    public static String publicBaseUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean def = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            if (!def) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }

    public static String mediaPublicBaseUrl(HttpServletRequest request, String configured) {
        String base = publicBaseUrl(request);
        if (!base.isBlank()) {
            return base + "/media";
        }
        return configured != null ? configured : "";
    }
}
