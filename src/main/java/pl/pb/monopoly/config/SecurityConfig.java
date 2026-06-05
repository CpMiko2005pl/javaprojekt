package pl.pb.monopoly.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfiguracja Spring Security (logowanie formularzowe + role + dostep do zasobow).
 *
 * Role: ADMIN, MODERATOR, USER, GUEST. Hierarchia dostepu:
 *   - strony publiczne: powitalna, logowanie, rejestracja, CSS, REST ranking
 *   - /admin/**            -> tylko ADMIN
 *   - operacje na nieruchomosciach (tworzenie/edycja/usuwanie) -> ADMIN, MODERATOR
 *   - reszta wymaga zalogowania
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Osobny lancuch filtrow dla konsoli H2 (tylko profil deweloperski).
     * Uzywamy gotowego matchera Spring Boota PathRequest.toH2Console(), dzieki
     * czemu konsola dziala w ramce (sameOrigin) i bez CSRF, nie kolidujac z
     * glownym lancuchem aplikacji.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(PathRequest.toH2Console())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** Glowny lancuch filtrow aplikacji. */
    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // zasoby publiczne (rowniez dla niezalogowanego "Goscia")
                .requestMatchers("/", "/register", "/login", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/ws/**").authenticated()
                .requestMatchers("/api/ranking-najlepszych").permitAll()
                // panel administracyjny
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // CRUD nieruchomosci: modyfikacje tylko admin/moderator
                .requestMatchers("/properties/new", "/properties/*/edit", "/properties/*/delete",
                        "/properties/save").hasAnyRole("ADMIN", "MODERATOR")
                // pozostale operacje wymagaja zalogowania
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/?wylogowano")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"));
        return http.build();
    }
}
