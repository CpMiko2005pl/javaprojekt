package pl.pb.monopoly.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    @Profile("h2")
    public SecurityFilterChain h2ConsoleSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(PathRequest.toH2Console())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/register", "/login", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/media/**", "/models/**", "/favicon.ico").permitAll()
                .requestMatchers("/ws/**").authenticated()
                .requestMatchers("/api/ranking-najlepszych").permitAll()
                .requestMatchers("/api/avatar/**").permitAll()
                .requestMatchers("/u/**").permitAll()

                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                .requestMatchers("/admin/**").hasRole("ADMIN")

                .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler((request, response, exception) -> {
                    String redirect = "/login?error";
                    if (exception instanceof org.springframework.security.authentication.LockedException) {
                        redirect = "/login?locked";
                    }
                    response.sendRedirect(redirect);
                })
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
