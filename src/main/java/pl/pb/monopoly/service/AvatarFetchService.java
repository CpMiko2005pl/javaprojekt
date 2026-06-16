package pl.pb.monopoly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Service
public class AvatarFetchService {

    private static final Logger log = LoggerFactory.getLogger(AvatarFetchService.class);
    private static final String DICEBEAR = "https://api.dicebear.com/7.x/avataaars/png";

    private final RestTemplate restTemplate;

    public AvatarFetchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchAvatarUrl(String seed) {
        if (seed == null || seed.isBlank()) return null;
        String url = UriComponentsBuilder.fromHttpUrl(DICEBEAR)
                .queryParam("seed", URLEncoder.encode(seed, StandardCharsets.UTF_8))
                .toUriString();
        try {
            ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
            HttpStatusCode status = resp.getStatusCode();
            if (status.is2xxSuccessful() && resp.getBody() != null && resp.getBody().length > 0) {
                return url;
            }
            log.warn("DiceBear zwrocil status {} dla seeda {}", status, seed);
        } catch (Exception ex) {
            log.warn("Nie udalo sie pobrac awatara z DiceBear dla {}: {}", seed, ex.getMessage());
        }
        return null;
    }
}
