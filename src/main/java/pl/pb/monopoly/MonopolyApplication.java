package pl.pb.monopoly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punkt wejscia aplikacji "Gra o Przetrwanie" (satyryczne Monopoly PB).
 *
 * UWAGA: To jest szkielet na pierwszy postep pracy (20-30%).
 * Zaimplementowane: rejestracja, logowanie (Spring Security z baza), role,
 * panel CRUD (nieruchomosci + zarzadzanie uzytkownikami), walidacja formularzy,
 * usluga REST (ranking). Wlasciwy silnik gry (WebSockets, plansza, transakcje)
 * powstanie w kolejnych etapach.
 */
@SpringBootApplication
public class MonopolyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonopolyApplication.class, args);
    }
}
