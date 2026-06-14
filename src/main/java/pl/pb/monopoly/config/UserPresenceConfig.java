package pl.pb.monopoly.config;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.pb.monopoly.service.UserPresenceService;

@Configuration
public class UserPresenceConfig {

    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> userPresenceSessionListener(
            UserPresenceService presenceService) {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                presenceService.removeSession(se.getSession().getId());
            }
        });
    }
}
