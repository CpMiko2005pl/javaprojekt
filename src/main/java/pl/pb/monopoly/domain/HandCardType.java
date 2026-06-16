package pl.pb.monopoly.domain;

public enum HandCardType {

    SKIP_RENT("Karta Ochrony", "Pomin oplate czynszu — zanim rzucisz kostka gwarantuje Ci bezplatne przejscie przez wrogie pole.", "fa-solid fa-shield"),
    EXTRA_ROLL("Dodatkowy Rzut", "Zagraj karte PRZED rzutem. W tej turze rzucasz kostka dwa razy (wliczajac obecna ture).", "fa-solid fa-dice"),
    DESTROY_PROPERTY("Dekret Wywlaszczeniowy", "Przejmij pole rywala — trafia do Ciebie bez domkow. Wlasciciel moze odkupic za 2x cene.", "fa-solid fa-hammer"),
    ADD_CASH("Stypendium Rektora", "Natychmiastowe +300 000 PLN od banku. Nic nie pytaj, po prostu bierz.", "fa-solid fa-coins"),
    SHIELD("Tarcza Akademicka", "Jezeli w tej turze mialbys wpasc w zadluzenie, do 300 000 PLN placi bank za Ciebie (jednorazowo).", "fa-solid fa-user-shield"),

    FREE_UPGRADE("Akademicki Pozwolenie Budowlane", "Zbuduj darmowe ulepszenie (+1 lvl) na jednej ze swoich posesji z monopolem kolorystycznym. Wybierz pole.", "fa-solid fa-hard-hat"),
    JAIL_PASS("Przepustka z Dziekanatu", "Omijasz oplate 200 000 PLN w Dziekanacie. Jezeli stoisz tam teraz — przechodzisz dalej gratis.", "fa-solid fa-door-open"),
    DOUBLE_RENT_NEXT("Podwojny Wymagacz", "Nastepny czynsz zebrany od rywala zostaje PODWOJONY — rywal placi 2x, Ty inkausujesz 2x.", "fa-solid fa-sack-dollar"),
    TELEPORT("Teleport Kampusowy", "Przenescie pionek na dowolne pole (0-39) przed rzutem. Bez efektu pola START, bez czynszu na nowym polu.", "fa-solid fa-rocket"),
    SCHOLARSHIP_ALL("Zbiorowe Stypendium", "Kazdy aktywny rywal placi Ci teraz 100 000 PLN (z wlasnej kasy). Gracz bez siana placi tyle ile ma.", "fa-solid fa-money-bill-wave");

    public final String label;
    public final String description;
    public final String iconClass;

    HandCardType(String label, String description, String iconClass) {
        this.label = label;
        this.description = description;
        this.iconClass = iconClass;
    }
}
