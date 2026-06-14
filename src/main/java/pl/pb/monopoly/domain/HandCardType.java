package pl.pb.monopoly.domain;

/** Typy kart w rece gracza — rozdawane na poczatek gry (maks. 3 na gre). */
public enum HandCardType {
    SKIP_RENT("Karta Ochrony", "Pomin oplate czynszu — zanim rzucisz kostka gwarantuje Ci bezplatne przejscie przez wrogie pole.", "fa-solid fa-shield"),
    EXTRA_ROLL("Dodatkowy Rzut", "Zagraj karte PRZED rzutem. W tej turze rzucasz kostka dwa razy (wliczajac obecna ture).", "fa-solid fa-dice"),
    DESTROY_PROPERTY("Dekret Wywlaszczeniowy", "Przejmij pole rywala — trafia do Ciebie bez domkow. Wlasciciel moze odkupic za 2x cene.", "fa-solid fa-hammer"),
    ADD_CASH("Stypendium Rektora", "Natychmiastowe +300 000 PLN od banku. Nic nie pytaj, po prostu bierz.", "fa-solid fa-coins"),
    SHIELD("Tarcza Akademicka", "Jezeli w tej turze mialbys wpasc w zadluzenie, do 300 000 PLN placi bank za Ciebie (jednorazowo).", "fa-solid fa-user-shield");

    public final String label;
    public final String description;
    public final String iconClass;

    HandCardType(String label, String description, String iconClass) {
        this.label = label;
        this.description = description;
        this.iconClass = iconClass;
    }
}
