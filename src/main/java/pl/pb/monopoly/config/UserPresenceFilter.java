package pl.pb.monopoly.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.pb.monopoly.service.UserPresenceService;

import java.io.IOException;

@Component
public class UserPresenceFilter extends OncePerRequestFilter {

    private final UserPresenceService presenceService;

    public UserPresenceFilter(UserPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            HttpSession session = request.getSession(true);
            presenceService.registerSession(auth.getName(), session.getId());
        }
        filterChain.doFilter(request, response);
    }
}
