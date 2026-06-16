# Mapa wymagań → pliki (stan aktualny, 2026)

Legenda: ✅ gotowe · ⛔ świadomie pominięte (z uzasadnieniem)

Odniesienie do `projekty-2025.pdf`. Aktywna rozgrywka żyje w **RAM serwera**
(`service/ActiveGameStore.java`) — stan gry ACTIVE nie jest zapisywany do bazy co ruch,
tylko na koniec meczu. Plansza to widok 3D (`static/js/board3d.js`, Three.js).

## Wymagania ogólne (model danych)
| Wymaganie | Status | Gdzie |
|---|---|---|
| ≥3 klasy domeny | ✅ | `domain/` — m.in. `User`, `PlayerStatistics`, `GameSession`, `GamePlayer`, `MatchHistory`, `Friendship`, `OwnedItem`, `ProfileComment` |
| ≥2 klasy połączone **kompozycją** | ✅ | `User`↔`PlayerStatistics` (1–1), `GameSession`↔`GamePlayer` (1–*) — `cascade=ALL, orphanRemoval=true` |
| Pola różnych typów + ≥1 `Date` | ✅ | `String/int/boolean/enum`; daty: `User.createdAt`, `GameSession.createdAt`, `MatchHistory.playedAt` |
| Ograniczenia wartości pól | ✅ | `@Size`, `@Min`, `@Max`, `@Pattern`, `@Email` + `@Column` w `domain/*` |

## Dostęp, role, UI
| Wymaganie | Status | Gdzie |
|---|---|---|
| Logowanie | ✅ | `config/SecurityConfig.java`, `templates/login.html`, `CustomUserDetailsService` (z bazy, BCrypt) |
| ≥3 role (w tym admin) | ✅ | `domain/Role.java` — **4 role**: ADMIN, MODERATOR, USER, GUEST |
| UI Thymeleaf + WCAG 2.1 | ✅ | `templates/` (lang, skip-link, `aria-*`, `scope`, `caption`, `.sr-only`), `static/css/styles.css` |

## CRUD encji User (MVC + REST)
| Operacja | Status | Gdzie |
|---|---|---|
| Create | ✅ | rejestracja — `AuthController.register`, `UserService.register`, `templates/register.html` |
| Read (lista) | ✅ | `AdminController.users` + `templates/admin/users.html`; REST: `GET /api/admin/users` |
| Update (dane bieżące) | ✅ | `GET/POST /admin/users/{id}/edit` — `templates/admin/user-edit.html`, `UserService.updateByAdmin` |
| Delete | ✅ | `AdminController.delete` (`POST /admin/users/{id}/delete`) |

> CRUD **pól planszy Monopoly** (encja `Property`) **nie jest implementowany** — ⛔ świadoma decyzja.
> Plansza jest stałym widokiem 3D (`board3d.js`), a nie zasobem zarządzanym przez admina.

## Szczegółowe funkcjonalności (20p)
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Dodanie/edycja/usunięcie/lista (≥4 pola) | 5 | ✅ | CRUD User (wyżej) — formularze ≥4 pól |
| Walidacja formularza (6 reguł) | 3 | ✅ | `dto/RegistrationForm.java` + `validation/PasswordMatches` (szczegóły niżej) |
| Edycja na danych bieżących | 1 | ✅ | `AdminController.editForm` ładuje aktualne dane z bazy do `AdminUserEditForm` |
| Współdzielenie danych między użytkownikami | 3 | ✅ | publiczny profil `GET /u/{username}` (`PublicController`) — dostępny bez logowania |
| Sortowanie w obu kierunkach (3 kryteria) | 2 | ✅ | `/admin/users` — login, data rejestracji, monety (`UserService.findForAdmin`, klikalne nagłówki) |
| Zapamiętanie kierunku/kryterium sortowania | 1 | ✅ | **ciasteczka** `adminUserSort`/`adminUserDir` (maxAge 30 dni) w `AdminController.users` |
| Filtrowanie wg daty i pola domeny | 2 | ✅ | `/admin/users` — `dateFrom` (data rejestracji od) + `role` (enum), łączone z sortowaniem |
| Logowanie | 1 | ✅ | jak wyżej |
| Zapis do bazy dopiero przy wylogowaniu | 2 | ⛔ | **pominięte — decyzja prowadzącego** (brak `HttpSession` draft / opóźnionego zapisu) |

## Dodatkowe funkcjonalności
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Rejestracja (niezalogowany) | 2 | ✅ | `AuthController.register`, `templates/register.html` |
| Strona powitalna | 1 | ✅ | `templates/index.html` |
| Wyświetlenie z udostępnionego linku (niezalogowany) | 1 | ✅ | `GET /u/{username}` (permitAll w `SecurityConfig`) |
| Lista użytkowników (admin) | 1 | ✅ | `AdminController.users` |
| Zarządzanie rolami (admin) | 1 | ✅ | `AdminController.changeRole` |

## Elementy techniczne (30p)
| Wymaganie | Pkt | Status | Gdzie |
|---|---|---|---|
| Kontrolery | 2 | ✅ | `controller/` (Home, Auth, Public, Admin, Moderator, Game, Wheel, Friends, Lootbox, `rest/*`) |
| Baza danych (≥2 tabele z relacją) | 3 | ✅ | wiele tabel; relacje 1–1, 1–*, *–1, kompozycje (`domain/`) |
| Widoki: formularze z walidacją + ≥5 znaczników Thymeleaf | 3 | ✅ | `th:text`, `th:each`, `th:if`, `th:object`/`th:field`, `th:errors`, `th:href`, `th:action` |
| Sesja | 3 | ✅ | sesja Spring Security (`HttpSession`) |
| Ciasteczka | 2 | ✅ | `AdminController` — zapamiętane sortowanie listy użytkowników |
| Usługa REST | 10 | ✅ | gra: `GameRestController` (~26 endpointów `/api/game/**`); admin: `GET /api/admin/users` (`AdminUserRestController`); ranking: `GET /api/ranking-najlepszych` |
| Klient REST | 2 | ✅ | `config/AppConfig` (`RestTemplate`) + `service/AvatarFetchService` → DiceBear; wpinany w `UserService.register` (awatar gdy brak własnego) |

## Funkcje gry (poza siatką punktową)
| Funkcja | Gdzie |
|---|---|
| Plansza „Kampus PB" **3D** (Three.js) | `static/js/board3d.js`, `templates/game/board.html` |
| Silnik rozgrywki: ruch, czynsz, kupno pól, karty, ulepszenia, bankructwo, licytacje | `GameService`, `GameEconomy` |
| **Wykup cudzej działki** po wylądowaniu (Business Tour) | `GameService` (`pendingTakeover*`), `GameEconomy.buyoutPrice` |
| Aktywna gra w **RAM serwera** (zero zapisów do DB co ruch) | `service/ActiveGameStore.java` |
| Boty (auto-rzut, decyzje kupna/ulepszenia/wykupu) | `BotAutoplayService` |
| Synchronizacja na żywo (WebSocket/STOMP) | `GameSyncService`, `/topic/game/{id}` |
| Sklep ze skrzynkami, ekwipunek, kosmetyki (admin: dodawanie itemów, monet, skrzynek) | `LootboxService`, `AdminController` (inventory/give-lootbox/add-coins) — **zachowane bez zmian** |
| **Osiągnięcia z nagrodą w coinach** (np. „Pierwsze zwycięstwo" +500) — przyznawane po meczu i retroaktywnie na dashboardzie | `AchievementType`, `Achievement` (tabela `player_achievements`), `AchievementService`, sekcja na `dashboard.html` |
| Profil FACEIT, znajomi, Koło Fortuny, komentarze profilowe | `dashboard.html`, `FriendService`, `WheelService`, `ProfileComment` |

## 6 reguł walidacji formularza rejestracji
Plik: `dto/RegistrationForm.java` (+ `validation/PasswordMatches.java`)
1. `@NotBlank` — pole wymagane (login, e-mail, hasło)
2. `@Size(min,max)` — długość tekstu
3. `@Pattern` — format ciągu (login: małe litery/cyfry)
4. `@Email` — poprawny adres e-mail
5. `@Min` / `@Max` — wiek ≥ 18
6. `@PasswordMatches` — **własna walidacja międzypolowa** (hasło == powtórzenie)

## Świadomie pominięte (⛔)
- **CRUD pól planszy (`Property`)** — plansza to stały widok 3D, nie zasób admina.
- **Zapis do bazy dopiero przy wylogowaniu / `HttpSession` draft profilu** — decyzja prowadzącego.
- Stare odniesienia do `PropertyController` i `board.js` jako głównej planszy są **nieaktualne**
  (główna plansza to `board3d.js`).

## Sprzątanie martwego kodu (2026)
Usunięto nieużywane encje i ich repozytoria (brak odczytu w aplikacji):
`Property`, `MonopolyCard`, `BoardTile`, `BoardCard`, `Rank`, `DailyTask`, `GameLog`.
(`Achievement` wrócił jako używana funkcja — nowa tabela `player_achievements`; stara, martwa tabela `achievements` jest w skrypcie DROP.)
Odpowiadające tabele w bazie usuwa skrypt `db/drop-dead-tables.sql`
(`ddl-auto=update` sam ich nie kasuje). Komentarze w kodzie Java/CSS/properties wyczyszczone.
