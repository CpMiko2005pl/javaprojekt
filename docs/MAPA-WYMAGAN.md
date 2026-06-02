# Mapa wymagań → pliki (stan na pierwszy postęp, 20–30%)

Legenda: ✅ gotowe · ◑ częściowo / fundament · ⏳ planowane na kolejny etap

Odniesienie do `projekty-2025.pdf`. Punktacja wg slajdów.

## Wymagania ogólne (model danych)
| Wymaganie | Status | Gdzie |
|---|---|---|
| ≥3 klasy domeny | ✅ | `domain/` — 6 klas: `User`, `PlayerStatistics`, `Property`, `MonopolyCard`, `GameLog`, `Achievement` |
| ≥2 klasy połączone **kompozycją** | ✅ | `domain/User.java` — `User`↔`PlayerStatistics` (1–1), `User`↔`Achievement` (1–*), `User`↔`GameLog` (1–*), wszystkie `cascade=ALL, orphanRemoval=true` |
| Pola różnych typów + ≥1 `Date` | ✅ | `String`, `int`, `BigDecimal`, `boolean`, `enum`; daty: `User.createdAt`, `PlayerStatistics.lastLogin`, `GameLog.occurredAt`, `Achievement.earnedAt` |
| Ograniczenia wartości pól | ✅ | `@Size`, `@Min`, `@Max`, `@Pattern`, `@DecimalMin/@DecimalMax`, `@Email` + ograniczenia kolumn (`@Column`) w `domain/*` |

## Dostęp, role, UI
| Wymaganie | Status | Gdzie |
|---|---|---|
| Logowanie | ✅ | `config/SecurityConfig.java`, `templates/login.html` |
| ≥3 role (w tym admin) | ✅ | `domain/Role.java` — **4 role**: ADMIN, MODERATOR, USER, GUEST |
| UI Thymeleaf + WCAG 2.1 | ✅ | `templates/` (lang, skip-link, `aria-*`, etykiety, `role`), `static/css/styles.css` (widoczny focus, kontrast, `.sr-only`) |
| CRUD w MVC **i** REST | ◑ | MVC: `controller/PropertyController`, `AdminController`; REST: `controller/rest/RankingRestController` (na razie odczyt — pełny CRUD REST w kolejnym etapie) |

## Szczegółowe funkcjonalności (20p)
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Dodanie/edycja/usunięcie/lista (≥4 pola) | 5 | ✅ | `PropertyController` + `templates/property/*` (6 pól); `AdminController` |
| Walidacja formularza (6 reguł) | 3 | ✅ | `dto/RegistrationForm.java` + `validation/PasswordMatches` (szczegóły niżej) |
| Edycja na danych bieżących | 1 | ✅ | `PropertyController.editForm` ładuje aktualny rekord z bazy |
| Współdzielenie danych między użytkownikami | 3 | ⏳ | planowane (udostępnianie po wskazaniu użytkownika / link) |
| Sortowanie w obu kierunkach (3 kryteria) | 2 | ✅ | `PropertyService.findAll` + `templates/property/list.html` (nazwa, lokalizacja, cena) |
| Zapamiętanie kierunku/kryterium sortowania | 1 | ✅ | **ciasteczka** `propSort`/`propDir` w `PropertyController` |
| Filtrowanie wg daty i pola domeny | 2 | ⏳ | planowane |
| Logowanie | 1 | ✅ | jak wyżej |
| Zapis do bazy dopiero przy wylogowaniu/wygaśnięciu sesji | 2 | ⏳ | planowane (stan gry w `HttpSession` → zrzut przy wylogowaniu) |

## Dodatkowe funkcjonalności (6p)
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Rejestracja (niezalogowany) | 2 | ✅ | `AuthController.register`, `templates/register.html` |
| Strona powitalna | 1 | ✅ | `templates/index.html` |
| Wyświetlenie z udostępnionego linku (niezalogowany) | 1 | ⏳ | planowane |
| Lista użytkowników (admin) | 1 | ✅ | `AdminController.users`, `templates/admin/users.html` |
| Zarządzanie rolami (admin) | 1 | ✅ | `AdminController.changeRole` |

## Elementy techniczne (30p)
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Kontrolery | 2 | ✅ | `controller/` (Home, Auth, Property, Admin, Game, Wheel, Friends, rest/*) |
| Baza danych (≥2 tabele z relacją) | 3 | ✅ | 10 tabel; relacje 1–1, 1–*, *–1, kompozycje (`domain/`) |
| Widoki: formularze z walidacją (3 różne elementy) + 5 znaczników Thymeleaf | 3 | ✅ | pola: text/email/number/password/select/checkbox; znaczniki: `th:text`, `th:each`, `th:if`/`th:unless`, `th:object`/`th:field`, `th:errors`, `th:href`, `th:action`, `th:inline`, `sec:authorize` (>5) |
| Sesja | 3 | ◑ | sesja Spring Security (`HttpSession`); pełny zrzut stanu gry przy wylogowaniu — kolejny etap |
| Ciasteczka | 2 | ✅ | `PropertyController` (zapamiętane sortowanie) |
| Usługa REST | 10 | ◑ | `GET /api/ranking-najlepszych` (ranking ELO) + REST rozgrywki `GET/POST /api/game/{id}/state\|roll\|transfer` (`GameRestController`); pełny CRUD REST — kolejny etap |
| Klient REST | 2 | ⏳ | planowane (losowe awatary z zewnętrznego API) |

### Funkcje dodane w 2. iteracji (poza siatką punktową, pod właściwą grę)
| Funkcja | Gdzie |
|---|---|
| Plansza „Kampus PB" na **canvasie** (40 pól, pionki, kostka, ruch) | `static/js/board.js`, `templates/game/board.html`, `GameService` |
| **Transfery „siana"** między graczami w trakcie gry | `GameService.transfer`, `GameRestController`, `board.js` |
| Profil gracza w **stylu FACEIT** (poziom, ELO, win rate, historia meczów) | `templates/dashboard.html`, `MatchHistory`, `PlayerStatistics` |
| **Codzienne Koło Fortuny** (losowanie raz dziennie + Daily Streak) | `WheelService`, `templates/wheel.html`, `static/js/wheel.js` |
| **System znajomych** (zaproszenia, akceptacja, wyszukiwanie, zaproś do gry) | `FriendService`, `Friendship`, `templates/friends.html` |
| Lobby gry (tworzenie pokoju, dołączanie po kodzie) | `GameController`, `templates/game/lobby.html` |
| Ciemny motyw w stylu FACEIT (cała aplikacja) | `static/css/styles.css` |
| Spring Security (z bazą) | 3 | ✅ | `CustomUserDetailsService` (ładuje użytkownika z bazy) + `SecurityConfig` (BCrypt) |

## 6 reguł walidacji formularza rejestracji
Plik: `dto/RegistrationForm.java` (+ `validation/PasswordMatches.java`)
1. `@NotBlank` — pole wymagane (login, e-mail, hasło)
2. `@Size(min,max)` — długość tekstu
3. `@Pattern` — format ciągu (login: małe litery/cyfry; imię/nazwisko: pierwsza wielka)
4. `@Email` — poprawny adres e-mail
5. `@Min` (i `@Max`) — wiek ≥ 18
6. `@PasswordMatches` — **własna walidacja międzypolowa** (hasło == powtórzenie)

> Prosiłeś o min. 4 walidacje — jest 6, czyli z zapasem na pełną punktację (3p).

## Co świadomie zostawiamy na kolejne etapy
- **Synchronizacja w czasie rzeczywistym (WebSockets)** — obecnie ruch jest hot-seat
  (tury lokalne); brak live-sync między przeglądarkami graczy.
- Transakcyjny silnik rozliczeń PLN (kupno pól, czynsz, bankructwo, karty wydarzeń).
- Pełna usługa REST (CRUD wszystkich encji) + klient REST (awatary z zewnętrznego API).
- Współdzielenie danych przez link, filtrowanie po dacie, zapis stanu do bazy dopiero przy wylogowaniu.
- Czat w pokoju gry, efekty dźwiękowe.
