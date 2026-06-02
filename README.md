# Gra o Przetrwanie — Politechnika Białostocka 🎲

Satyryczna, wieloosobowa gra planszowa online (mechanika handlu nieruchomościami)
osadzona na kampusie PB. Waluta: **Złotówki (PLN)**.

> **Status:** szkielet na **pierwszy postęp pracy (20–30%)**. Zaimplementowano
> rejestrację, logowanie, role, panel CRUD i walidację. Właściwy silnik gry
> (WebSockets, plansza, transakcje, karty wydarzeń) powstanie w kolejnych etapach.

## Stack technologiczny
- **Java 21**, **Spring Boot 3.5**
- Spring MVC + **Thymeleaf** (widoki, WCAG 2.1)
- Spring Data JPA / Hibernate
- **Spring Security** (logowanie, role, hasła BCrypt) — oparte o bazę danych
- **PostgreSQL** (profil docelowy) oraz **H2** (profil deweloperski, baza w pamięci)
- Walidacja: Jakarta Bean Validation

## Jak uruchomić

### Wymagania
- Zainstalowana **Java 21** (sprawdź: `java -version`).
- Maven **nie jest wymagany** — w repo jest *Maven Wrapper* (`mvnw` / `mvnw.cmd`).

### Wariant A — od ręki, baza H2 (domyślny, zalecany na prezentację)
Nie wymaga instalacji żadnej bazy. W katalogu projektu:

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```
```bash
# Linux / macOS
./mvnw spring-boot:run
```

Aplikacja: <http://localhost:8080>
Konsola bazy H2: <http://localhost:8080/h2-console>
(JDBC URL: `jdbc:h2:mem:monopoly`, użytkownik `sa`, hasło puste)

### Wariant B — docelowy PostgreSQL
1. Zainstaluj PostgreSQL i utwórz bazę (skrypt w `db/postgres-setup.sql`):
   ```bash
   psql -U postgres -f db/postgres-setup.sql
   ```
2. Uruchom z profilem `postgres`:
   ```powershell
   .\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=postgres"
   ```
   Dane logowania do bazy można nadpisać zmiennymi `DB_USER` / `DB_PASSWORD`.

> Tabele tworzą się automatycznie (Hibernate `ddl-auto`). Baza **nie musi**
> istnieć wcześniej w wariancie H2; w wariancie PostgreSQL wystarczy sama baza
> (skrypt powyżej) — strukturę tabel dorobi Hibernate.

## Konta testowe (tworzone automatycznie przy pierwszym starcie)
| Login       | Hasło         | Rola          | Uwagi |
|-------------|---------------|---------------|-------|
| `admin`     | `admin123`    | Administrator | wysoki ranking ELO |
| `moderator` | `moderator123`| Moderator     | znajomy `gracza` |
| `gracz`     | `gracz123`    | Użytkownik    | pełny profil: historia meczów, znajomi, zaproszenia |
| `kuba`      | `kuba123`     | Użytkownik    | znajomy `gracza` |
| `ola`       | `ola123`      | Użytkownik    | wysłała zaproszenie do `gracza` |

## Co już działa
- **Rejestracja** (`/register`) z 6 regułami walidacji (więcej w `docs/MAPA-WYMAGAN.md`).
- **Logowanie / wylogowanie** (Spring Security, hasła BCrypt, role z bazy).
- **Ciemny motyw** w stylu FACEIT (cała aplikacja, WCAG 2.1).
- **Profil gracza w stylu FACEIT** (`/dashboard`): poziom, ELO, win rate, seria
  zwycięstw, **historia meczów**. Saldo nie jest tu pokazywane — „siano" istnieje
  tylko podczas rozgrywki.
- **Plansza na canvasie** (`/game/{id}`): plansza „Kampus PB" (40 pól), pionki,
  rzut kostką i ruch, **system transferów „siana"** między graczami w trakcie gry.
- **Lobby gry** (`/game`): tworzenie pokoju, zapraszanie znajomych, dołączanie po kodzie.
- **Codzienne Koło Fortuny** (`/wheel`): losowanie ułatwienia raz dziennie + Daily Streak.
- **System znajomych** (`/friends`): wyszukiwanie graczy, zaproszenia, akceptacja.
- **Panel CRUD nieruchomości** (`/properties`): dodaj/edytuj/usuń/lista, sortowanie
  w obu kierunkach wg 3 kryteriów (zapamiętane w ciasteczku).
- **Panel administratora** (`/admin/users`): lista użytkowników, doładowanie środków,
  weryfikacja konta, zmiana roli, usuwanie.
- **Usługa REST**: `GET /api/ranking-najlepszych` (ranking ELO w JSON) oraz REST
  rozgrywki (`/api/game/{id}/state|roll|transfer`).
- **Strona powitalna**.

## Mapa spełnionych wymagań
Szczegółowe odniesienie każdego punktowanego elementu do pliku/katalogu znajduje
się w **[`docs/MAPA-WYMAGAN.md`](docs/MAPA-WYMAGAN.md)**.

## Struktura projektu (skrót)
```
src/main/java/pl/pb/monopoly
├── MonopolyApplication.java     # start aplikacji
├── config/                      # SecurityConfig, DataInitializer (dane startowe)
├── domain/                      # encje: User, PlayerStatistics, Property, MonopolyCard, GameLog,
│                                #        Achievement, MatchHistory, Friendship, GameSession, GamePlayer
├── repository/                  # repozytoria Spring Data JPA
├── service/                     # UserService, PropertyService, GameService, WheelService,
│                                #        FriendService, CustomUserDetailsService
├── dto/                         # RegistrationForm, RankingEntryDto, GameStateDto, WheelResultDto, ...
├── validation/                  # @PasswordMatches (walidacja międzypolowa)
└── controller/                  # Home, Auth, Property, Admin, Game, Wheel, Friends
                                 #   + rest/ (RankingRestController, GameRestController)
src/main/resources
├── templates/                   # widoki Thymeleaf (index, dashboard, wheel, friends,
│                                #   game/lobby, game/board, property/*, admin/*, fragments/layout)
├── static/css/styles.css        # ciemny motyw (WCAG 2.1)
├── static/js/                   # board.js (plansza canvas), wheel.js (koło fortuny)
└── application*.properties       # konfiguracja + profile h2 / postgres
db/postgres-setup.sql            # tworzenie bazy PostgreSQL
```
