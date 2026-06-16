# -*- coding: utf-8 -*-
"""Generuje PDF z audytem projektu Politechnika Monopoly."""
from fpdf import FPDF
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "AUDYT-PROJEKTU-MONOPOLY.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_B = Path(r"C:\Windows\Fonts\arialbd.ttf")


class AuditPDF(FPDF):
    def footer(self):
        self.set_y(-15)
        self.set_font("Arial", "", 9)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Politechnika Monopoly — audyt projektu  |  strona {self.page_no()}", align="C")


def section(pdf, title):
    pdf.ln(4)
    pdf.set_font("Arial", "B", 13)
    pdf.set_text_color(30, 60, 120)
    pdf.multi_cell(0, 8, title)
    pdf.set_text_color(0, 0, 0)
    pdf.ln(2)


def body(pdf, text):
    pdf.set_font("Arial", "", 10)
    pdf.multi_cell(0, 5.5, text)
    pdf.ln(1)


def bullet(pdf, text):
    pdf.set_font("Arial", "", 10)
    pdf.set_x(pdf.l_margin)
    pdf.multi_cell(0, 5.5, "- " + text)


def table_row(pdf, col1, col2, col3, header=False):
    w = [62, 20, 88]
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Arial", "B" if header else "", 9)
    if header:
        pdf.set_fill_color(230, 235, 245)
    else:
        pdf.set_fill_color(255, 255, 255)
    pdf.cell(w[0], 7, col1[:42], border=1, fill=True)
    pdf.cell(w[1], 7, col2[:10], border=1, fill=True, align="C")
    pdf.cell(w[2], 7, col3[:52], border=1, fill=True)
    pdf.ln()


def main():
    pdf = AuditPDF()
    pdf.set_margins(18, 18, 18)
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_font("Arial", "", str(FONT))
    pdf.add_font("Arial", "B", str(FONT_B))

    # --- Strona tytułowa ---
    pdf.add_page()
    pdf.ln(35)
    pdf.set_font("Arial", "B", 22)
    pdf.multi_cell(0, 12, "Audyt projektu\nPolitechnika Monopoly", align="C")
    pdf.ln(8)
    pdf.set_font("Arial", "", 12)
    pdf.set_text_color(80, 80, 80)
    pdf.multi_cell(
        0, 7,
        "Analiza zgodności z wymaganiami projekty-2025.pdf\n"
        "Struktura bazy danych · martwy kod · wydajność · rekomendacje",
        align="C",
    )
    pdf.set_text_color(0, 0, 0)
    pdf.ln(20)
    pdf.set_font("Arial", "", 10)
    pdf.multi_cell(
        0, 6,
        "Repozytorium: javaprojekt (Spring Boot 3.5, Java 17, PostgreSQL, Thymeleaf, WebSocket, Three.js)\n"
        "Data audytu: czerwiec 2026\n"
        "Dokument przygotowany na potrzeby prezentacji i dalszego rozwoju projektu.",
        align="C",
    )

    # --- 1. Wprowadzenie ---
    pdf.add_page()
    section(pdf, "1. Wprowadzenie")
    body(
        pdf,
        "Projekt Politechnika Monopoly to rozbudowana aplikacja WWW wykraczająca poza typowy "
        "zakres wymagań z projekty-2025.pdf: pełna gra wieloosobowa, ranking ELO, koło fortuny, "
        "lootboxy, znajomi, moderacja i plansza 3D. Niniejszy audyt ocenia zgodność z punktacją "
        "egzaminacyjną, identyfikuje martwy kod oraz proponuje usprawnienia architektury — "
        "w szczególności przeniesienie aktywnej rozgrywki z bazy danych do pamięci serwera (RAM).",
    )

    # --- 2. Wymagania PDF ---
    section(pdf, "2. Zgodność z wymaganiami (projekty-2025.pdf)")
    body(pdf, "Legenda: TAK = spełnione  |  CZĘŚCIOWO  |  NIE = brak lub sprzeczne z kodem")
    pdf.ln(2)
    table_row(pdf, "Wymaganie", "Status", "Uwagi", header=True)
    rows = [
        ("≥3 klasy domeny, kompozycje, Date, walidacja pól", "TAK", "User, GameSession, Property itd."),
        ("Logowanie, ≥3 role (admin)", "TAK", "ADMIN, MODERATOR, USER, GUEST"),
        ("Thymeleaf + WCAG 2.1", "CZĘŚCIOWO", "Dobre na stronach statycznych; plansza 3D słabsza"),
        ("CRUD MVC i REST dla klas domeny", "CZĘŚCIOWO", "Brak PropertyController — usunięty"),
        ("Walidacja 6 reguł formularza", "TAK", "RegistrationForm + PasswordMatches"),
        ("Edycja na danych bieżących", "TAK", "SettingsController"),
        ("Współdzielenie danych / link publiczny", "TAK", "/u/{username}, zaproszenia do gry"),
        ("Sortowanie 3 kryteria × 2 kierunki", "NIE", "Było w Property CRUD — brak po usunięciu"),
        ("Zapamiętanie sortowania (cookies)", "NIE", "Brak cookies sortowania; motyw w localStorage"),
        ("Filtrowanie: data + pole domeny", "NIE", "Brak widoku z filtrem"),
        ("Zapis do DB przy wylogowaniu/sesji", "NIE", "Każdy ruch zapisuje do PostgreSQL od razu"),
        ("Rejestracja, strona powitalna", "TAK", "register.html, index.html"),
        ("Lista użytkowników + role (admin)", "TAK", "AdminController"),
        ("Usługa REST", "TAK", "~24 endpointy gry + ranking"),
        ("Klient REST (serwer → API)", "NIE", "Brak RestTemplate/WebClient w Javie"),
        ("Spring Security z bazą", "TAK", "CustomUserDetailsService, BCrypt"),
    ]
    for r in rows:
        table_row(pdf, *r)

    pdf.ln(3)
    body(
        pdf,
        "Szacunkowo projekt zdobywa ok. 53–57 pkt z ~56 pkt wymaganych, lecz prowadzący może "
        "odejmować punkty za brak Property CRUD, sortowania/filtrowania, zapisu sesyjnego "
        "oraz nieaktualną dokumentację (docs/MAPA-WYMAGAN.md opisuje pliki, których już nie ma).",
    )

    # --- 3. Mocne strony ---
    section(pdf, "3. Mocne strony projektu (ponad wymagania)")
    bullets = [
        "Pełny silnik Monopoly: kupno, czynsz, ulepszenia, karty szansy, bankructwo, boty.",
        "Synchronizacja w czasie rzeczywistym przez WebSocket (STOMP).",
        "Metagame: ELO, poziomy, koło fortuny, skrzynki, sklep, historia meczów.",
        "System znajomych, zaproszeń do gry i publiczne profile graczy.",
        "Moderacja: weryfikacja legitymacji, zawieszenia kont, ostrzeżenia.",
        "Panel administratora z podglądem aktywnych sesji i zarządzaniem użytkownikami.",
        "Plansza 3D (Three.js) - estetyka Business Tour, bez koniecznosci upraszczania do 2D.",
    ]
    for b in bullets:
        bullet(pdf, b)

    # --- 4. Martwy kod ---
    pdf.add_page()
    section(pdf, "4. Martwy i nieużywany kod")
    body(pdf, "Backend — encje bez realnego użycia w runtime:")
    for b in [
        "Property + PropertyRepository — encja pod CRUD egzaminacyjny; gra używa GamePlayer.ownedPositions.",
        "Achievement, GameLog — kompozycja w User, ale nigdy nie zapisywane w kodzie.",
        "BoardTile, BoardCard, Rank, DailyTask — seed w DataInitializer; silnik czyta GameEconomy (kod).",
        "db/add-state-epoch.sql — kolumna state_epoch bez pola JPA w GameSession.",
    ]:
        bullet(pdf, b)
    pdf.ln(2)
    body(pdf, "Frontend — pliki po migracji 2D → 3D:")
    for b in [
        "static/js/board.js — stara plansza canvas, niepodpięta.",
        "static/js/board-preview.js, static/js/lootbox.js — brak referencji w szablonach.",
        "templates/game/room.html — brak kontrolera, zastąpiony przez lobby.html.",
    ]:
        bullet(pdf, b)
    pdf.ln(2)
    body(
        pdf,
        "Duplikacja źródła prawdy: tablice w GameEconomy.java vs tabele board_tiles/board_cards "
        "w PostgreSQL. Zmiana ekonomii w kodzie nie aktualizuje automatycznie słowników w bazie.",
    )

    # --- 5. Baza danych ---
    section(pdf, "5. Struktura bazy danych")
    body(pdf, "Warstwa trwała (schema monopoly, PostgreSQL):")
    for b in [
        "Użytkownicy: users, player_statistics, owned_items, match_history.",
        "Społeczność: friendships, profile_comments, game_invites, user_warnings.",
        "Rozgrywka: game_sessions, game_players + 4 tabele element-collection (posesje, level, landing, karty).",
        "Słowniki (głównie nieużywane w runtime): board_tiles, board_cards, ranks, daily_tasks, properties.",
    ]:
        bullet(pdf, b)

    section(pdf, "5.1. Problemy wykorzystania bazy podczas gry")
    for b in [
        "Każdy rzut kostką, kupno, skip, upgrade = transakcja JPA + sessionRepository.save().",
        "GamePlayer ma FetchType.EAGER na wszystkich kolekcjach — każdy odczyt sesji ładuje 4 tabele pomocnicze.",
        "Brak projekcji / EntityGraph — pełny graf obiektów przy każdym GET /api/game/{id}/state.",
        "Polling co 4 s i 5 s w board3d.js (fallback) generuje dodatkowy ruch HTTP + zapytania DB.",
        "GameStateDto wysyła pełny stan gry przy każdym broadcastzie WebSocket — duży JSON.",
    ]:
        bullet(pdf, b)

    # --- 6. RAM vs DB - KEY SECTION ---
    pdf.add_page()
    section(pdf, "6. Architektura: baza danych vs pamięć serwera (RAM)")
    body(
        pdf,
        "OBECNY STAN — wszystko w bazie:\n"
        "Aktywna rozgrywka (pozycje pionków, gotówka, pending kupna/płatności, tura, karty w ręce) "
        "jest trzymana w PostgreSQL i zapisywana przy KAŻDEJ akcji gracza. To podejście zapewnia "
        "trwałość po restarcie serwera, ale jest kosztowne: setki zapytań SQL na jedną grę, "
        "opóźnienia transakcyjne, obciążenie Hibernate i wolniejsza reakcja UI — szczególnie "
        "na słabszych urządzeniach i przy wielu równoległych grach.",
    )
    pdf.ln(2)
    body(
        pdf,
        "REKOMENDOWANY STAN — rozgrywka w RAM serwera:\n"
        "Stan aktywnej gry (status ACTIVE) powinien żyć w pamięci procesu Spring Boot — np. "
        "ConcurrentHashMap<Long, ActiveGameState> lub Redis (przy wielu instancjach). "
        "Operacje roll/buy/skip modyfikują obiekt w RAM (mikrosekundy), a WebSocket od razu "
        "pushuje lekki DTO do klientów. Dopiero przy zakończeniu meczu, wyjściu ostatniego "
        "gracza lub crash-recovery (opcjonalny snapshot co N tur) następuje zapis do PostgreSQL: "
        "MatchHistory, aktualizacja ELO/coins, ewentualnie archiwum sesji.",
    )
    pdf.ln(2)
    body(pdf, "Proponowany podział odpowiedzialności:")
    table_row(pdf, "Typ danych", "Teraz", "Powinno być", header=True)
    arch = [
        ("Stan ACTIVE (tura, pending, pozycje)", "PostgreSQL co ruch", "RAM serwera (ActiveGameStore)"),
        ("Lobby WAITING", "PostgreSQL", "PostgreSQL (OK — mało operacji)"),
        ("Profil, monety, ELO, inventory", "PostgreSQL", "PostgreSQL (trwałe metadane)"),
        ("Słownik planszy", "DB + kod (duplikat)", "Tylko kod (GameEconomy) lub tylko DB"),
        ("Draft edycji profilu (PDF)", "Brak", "HttpSession → flush przy logout"),
    ]
    for r in arch:
        table_row(pdf, *r)

    pdf.ln(3)
    body(
        pdf,
        "Korzyści przeniesienia rozgrywki do RAM:\n"
        "• 10–100× mniej zapytań SQL w trakcie meczu.\n"
        "• Szybsza odpowiedź REST/WebSocket — brak czekania na commit transakcji przy każdym rzucie.\n"
        "• Mniejsze obciążenie PostgreSQL — baza tylko dla danych trwałych i lobby.\n"
        "• Lepsza responsywność na słabszych urządzeniach (mniej opóźnień sieciowych po stronie serwera).\n"
        "• Łatwiejsze anulowanie starych timerów bota (BotAutoplayService) bez race z DB.\n\n"
        "Uwaga egzaminacyjna: wymaganie PDF „zapis do bazy dopiero przy wylogowaniu” idealnie "
        "pasuje do wzorca: stan roboczy w sesji/RAM → persist przy logout/koniec gry. Obecna "
        "implementacja robi odwrotnie (zapis natychmiastowy), co jest sprzeczne z PDF, ale "
        "logiczne dla multiplayer — warto to opisać na prezentacji jako świadomą decyzję "
        "architektoniczną z planem migracji do ActiveGameStore.",
    )

    # --- 7. Wydajność frontend (bez 2D) ---
    section(pdf, "7. Wydajność po stronie klienta (bez rezygnacji z 3D)")
    body(
        pdf,
        "Plansza 3D (board3d.js, ~142 KB, ~2790 linii) pozostaje głównym elementem wizualnym — "
        "nie proponuje się trybu 2D, aby nie psuć estetyki projektu. Optymalizacje w ramach 3D:",
    )
    for b in [
        "Wyłączyć polling HTTP gdy WebSocket jest połączony (obecnie 4 s + 5 s niezależnie).",
        "Delta updates w WebSocket — wysyłać tylko zmienione pola, nie pełny GameStateDto.",
        "Lazy load modeli GLTF pionków; cache w przeglądarce.",
        "prefers-reduced-motion: skrócone animacje kostki/ruchu (bez zmiany wyglądu planszy).",
        "Cache busting wersji JS (np. ?v=...) — uniknięcie starego board3d.js w przeglądarce.",
        "Jeden rebuild mvn clean package po zmianach — IntelliJ często trzyma stary target/.",
    ]:
        bullet(pdf, b)

    # --- 8. Rekomendacje ---
    pdf.add_page()
    section(pdf, "8. Rekomendacje — priorytety")
    body(pdf, "A. Egzamin (wysoki priorytet):")
    for b in [
        "Przywrócić PropertyController + widoki CRUD z sortowaniem (3 kryteria) i cookies.",
        "Dodać filtrowanie listy Property po dacie i colorGroup.",
        "Draft profilu w HttpSession + zapis User przy logout (wymaganie PDF).",
        "Serwerowy klient REST (np. RestTemplate → API awatarów przy rejestracji).",
        "Zaktualizować docs/MAPA-WYMAGAN.md — mapowanie wymaganie → plik.",
    ]:
        bullet(pdf, b)
    pdf.ln(2)
    body(pdf, "B. Architektura i wydajność (średni priorytet):")
    for b in [
        "Wprowadzić ActiveGameStore — stan ACTIVE w RAM, flush do DB na FINISHED.",
        "GamePlayer: LAZY + DTO zamiast EAGER element collections przy odczycie.",
        "Usunąć lub podłączyć martwe encje (Achievement, GameLog, Property CRUD demo).",
        "Jedno źródło prawdy planszy — GameEconomy LUB board_tiles, nie oba.",
        "Naprawić BotAutoplayService — anulowanie starych timerów (bug „Brak aktywnego pola do kupna”).",
    ]:
        bullet(pdf, b)
    pdf.ln(2)
    body(pdf, "C. Rozszerzenia gry Monopoly (niski priorytet / po egzaminie):")
    for b in [
        "Handel nieruchomościami między graczami (obecnie tylko transfer gotówki).",
        "Czat w pokoju przez WebSocket /topic/game/{id}/chat.",
        "Tutorial przy pierwszej grze; replay z GameLog (gdy encja zacznie być używana).",
        "Testy integracyjne GameService (src/test jest pusty).",
    ]:
        bullet(pdf, b)

    # --- 9. Podsumowanie ---
    section(pdf, "9. Podsumowanie")
    body(
        pdf,
        "Politechnika Monopoly to dojrzały projekt gry wieloosobowej wykraczający poza standard "
        "kursowy. Formalnie brakuje kilku punktów z PDF (Property CRUD, sort/filter, zapis "
        "sesyjny, klient REST Java). Największy problem techniczny to trzymanie całej aktywnej "
        "rozgrywki w PostgreSQL z zapisem przy każdym ruchu — powinno to żyć w pamięci serwera, "
        "a baza powinna służyć profilom, historii meczów i lobby. Plansza 3D zostaje bez "
        "uproszczeń wizualnych; wydajność poprawia się przez RAM po stronie serwera, mniejsze "
        "payloady WebSocket i usunięcie zbędnego pollingu.",
    )

    OUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUT))
    print(f"Wygenerowano: {OUT}")

if __name__ == "__main__":
    main()
