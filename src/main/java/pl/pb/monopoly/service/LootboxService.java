package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Skrzynki (lootboxy) z humorystycznymi przedmiotami studenckimi.
 *
 * Mechanika:
 *  - kazdy gracz dostaje codziennie 1 darmowa skrzynke (max 5 zapasu),
 *  - po otwarciu losuje sie przedmiot z waga rarity:
 *      COMMON 60%, RARE 25%, EPIC 12%, LEGENDARY 3%,
 *  - przedmiot zapisuje sie w `OwnedItem` (encja JPA),
 *  - katalog itemow jest STATYCZNY i wbudowany w te klase (LOOTBOX_ITEMS).
 */
@Service
public class LootboxService {

    public enum Rarity {
        COMMON("Common", "#9ca3af", 60),
        RARE("Rare", "#3b82f6", 25),
        EPIC("Epic", "#a855f7", 12),
        LEGENDARY("Legendary", "#f59e0b", 3);

        public final String label;
        public final String color;
        public final int weight;

        Rarity(String label, String color, int weight) {
            this.label = label;
            this.color = color;
            this.weight = weight;
        }
    }

    /** Pojedynczy przedmiot z katalogu (statyczny). */
    public record LootboxItem(
            String slug,
            String name,
            Rarity rarity,
            String category,
            String iconClass,
            String description
    ) {}

    /** Pelny katalog itemow - rozszerzaj dowolnie. */
    public static final List<LootboxItem> LOOTBOX_ITEMS = List.of(
            // COMMON
            new LootboxItem("avatar-student", "Awatar: Student PB", Rarity.COMMON, "Awatar",
                    "fa-solid fa-user-graduate", "Klasyczny student z plecakiem."),
            new LootboxItem("avatar-zmeczony", "Awatar: Zmeczony zaliczeniami", Rarity.COMMON, "Awatar",
                    "fa-solid fa-bed", "Po sesji wyglada tak kazdy."),
            new LootboxItem("color-blue", "Pionek: Kobalt PB", Rarity.COMMON, "Kolor pionka",
                    "fa-solid fa-circle", "Niebieski pionek w barwach uczelni."),
            new LootboxItem("color-red", "Pionek: Czerwien Wydzialu", Rarity.COMMON, "Kolor pionka",
                    "fa-solid fa-circle", "Klasyczna czerwien."),
            new LootboxItem("emoji-coffee", "Naklejka: Kawa z USOSweb", Rarity.COMMON, "Naklejka",
                    "fa-solid fa-mug-saucy", "Nieskonczona ilosc kofeiny."),
            new LootboxItem("emoji-pizza", "Naklejka: Pizza ze Stolowki", Rarity.COMMON, "Naklejka",
                    "fa-solid fa-pizza-slice", "Tylko za 8 PLN po promocji."),
            new LootboxItem("frame-bronze", "Ramka profilu: Brazowa", Rarity.COMMON, "Ramka",
                    "fa-solid fa-square-poll-vertical", "Skromna, ale wlasna."),

            // RARE
            new LootboxItem("avatar-bibliotekarz", "Awatar: Bibliotekarz", Rarity.RARE, "Awatar",
                    "fa-solid fa-book-open-reader", "Wie wszystko o terminach."),
            new LootboxItem("avatar-gwint", "Awatar: Krol Gwintu", Rarity.RARE, "Awatar",
                    "fa-solid fa-cards-blank", "Mistrz wieczornych rozgrywek."),
            new LootboxItem("color-emerald", "Pionek: Szmaragd", Rarity.RARE, "Kolor pionka",
                    "fa-solid fa-circle", "Zieleń kampusowych traw."),
            new LootboxItem("color-pink", "Pionek: Pink Hype", Rarity.RARE, "Kolor pionka",
                    "fa-solid fa-circle", "Dla odwaznych."),
            new LootboxItem("frame-silver", "Ramka profilu: Srebrna", Rarity.RARE, "Ramka",
                    "fa-solid fa-medal", "Stylowa, blyszczy w ciemnosci."),
            new LootboxItem("title-stypendysta", "Tytul: Stypendysta", Rarity.RARE, "Tytul",
                    "fa-solid fa-award", "Nadawany w panelu profilu."),
            new LootboxItem("pawn-3d-skeleton", "Pionek 3D: Szkielet", Rarity.RARE, "Pionek 3D",
                    "fa-solid fa-skull", "Klik-klak po polach Monopoly."),
            new LootboxItem("pawn-3d-piglin", "Pionek 3D: Piglin", Rarity.RARE, "Pionek 3D",
                    "fa-solid fa-piggy-bank", "Netherowy handlarz jako Twoj pionek."),
            new LootboxItem("pawn-3d-pillager", "Pionek 3D: Rozbojnik", Rarity.RARE, "Pionek 3D",
                    "fa-solid fa-user-ninja", "Najezdzca z wioski — strzela do konkurencji."),
            new LootboxItem("pawn-3d-goblin", "Pionek 3D: Goblin", Rarity.RARE, "Pionek 3D",
                    "fa-solid fa-frog", "Zlosliwy stwor — widoczny na planszy 3D."),

            // EPIC
            new LootboxItem("avatar-dziekan", "Awatar: Dziekan w gniewie", Rarity.EPIC, "Awatar",
                    "fa-solid fa-user-tie", "Idz na konsultacje! Idz!"),
            new LootboxItem("color-neon", "Pionek: Neon Cyber", Rarity.EPIC, "Kolor pionka",
                    "fa-solid fa-circle-radiation", "Swieci w 3D na planszy."),
            new LootboxItem("frame-gold", "Ramka profilu: Zlota", Rarity.EPIC, "Ramka",
                    "fa-solid fa-crown", "Tylko dla najlepszych."),
            new LootboxItem("title-dziekan", "Tytul: Postrach Dziekanatu", Rarity.EPIC, "Tytul",
                    "fa-solid fa-skull", "Stoisz tam czesciej niz wykladowca."),
            new LootboxItem("emoji-trophy", "Naklejka: Puchar Spartakiady", Rarity.EPIC, "Naklejka",
                    "fa-solid fa-trophy", "Spartakiada PB - I miejsce."),
            new LootboxItem("pawn-3d-creeper", "Pionek 3D: Creeper", Rarity.EPIC, "Pionek 3D",
                    "fa-solid fa-bomb", "Sssss... na planszy wyglada groznie."),
            new LootboxItem("pawn-3d-enderman", "Pionek 3D: Enderman", Rarity.EPIC, "Pionek 3D",
                    "fa-solid fa-ghost", "Wysoki, cienisty — idealny na nocne rozgrywki."),
            new LootboxItem("pawn-3d-penguin", "Pionek 3D: Pingwin", Rarity.EPIC, "Pionek 3D",
                    "fa-solid fa-snowflake", "Pingwin z blockowego swiata — rzadki drop."),

            // LEGENDARY
            new LootboxItem("avatar-rektor", "Awatar: Rektor PB", Rarity.LEGENDARY, "Awatar",
                    "fa-solid fa-chess-king", "Najwyzsza wladza na kampusie."),
            new LootboxItem("frame-rainbow", "Ramka profilu: Tecza", Rarity.LEGENDARY, "Ramka",
                    "fa-solid fa-rainbow", "Animowana, spektakularna."),
            new LootboxItem("title-legenda", "Tytul: Legenda Kampusu", Rarity.LEGENDARY, "Tytul",
                    "fa-solid fa-star", "Wszyscy o Tobie slyszeli."),
            new LootboxItem("pawn-3d-corn", "Pionek 3D: Zlota Kukurydza", Rarity.LEGENDARY, "Pionek 3D",
                    "fa-solid fa-chess-pawn", "Unikalny zloty pionek 3D widoczny na planszy."),
            new LootboxItem("pawn-3d-steve", "Pionek 3D: Steve", Rarity.LEGENDARY, "Pionek 3D",
                    "fa-solid fa-cube", "Klasyk blockowego swiata — legendarny pionek na planszy.")
    );

    private final OwnedItemRepository ownedItemRepository;
    private final UserRepository userRepository;

    public LootboxService(OwnedItemRepository ownedItemRepository, UserRepository userRepository) {
        this.ownedItemRepository = ownedItemRepository;
        this.userRepository = userRepository;
    }

    public static List<LootboxItem> catalog() {
        return LOOTBOX_ITEMS;
    }

    public static LootboxItem findBySlug(String slug) {
        return LOOTBOX_ITEMS.stream().filter(i -> i.slug().equals(slug)).findFirst().orElse(null);
    }

    /**
     * Doliczenie darmowej skrzynki, jezeli gracz nie odebral dzis i ma < 5.
     * Wywoluje sie automatycznie z {@code grantDailyIfNeeded()} przy wejsciu na profil.
     */
    @Transactional
    public void grantDailyIfNeeded(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        PlayerStatistics stats = user.getStatistics();
        if (stats == null) return;
        LocalDate today = LocalDate.now();
        if (stats.getLastLootboxGrantDate() == null || !stats.getLastLootboxGrantDate().equals(today)) {
            int current = stats.getAvailableLootboxes();
            stats.setAvailableLootboxes(Math.min(current + 1, 5));
            stats.setLastLootboxGrantDate(today);
        }
    }

    /** Wynik otwarcia skrzynki — item moze byc bez zapisu, gdy gracz ma juz caly katalog. */
    public record OpenResult(LootboxItem item, boolean addedToInventory, String inventoryNote) {}

    /** Otwiera skrzynke jezeli gracz ma >0 dostepnych. Duplikaty nie trafiaja do ekwipunku. */
    @Transactional
    public OpenResult open(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        PlayerStatistics stats = user.getStatistics();
        if (stats == null) {
            throw new IllegalArgumentException("Brak statystyk gracza.");
        }
        if (stats.getAvailableLootboxes() <= 0) {
            throw new IllegalArgumentException("Nie masz dostepnych skrzynek - wroc jutro!");
        }

        Set<String> ownedSlugs = ownedItemRepository.findByUserIdOrderByObtainedAtDesc(user.getId()).stream()
                .map(OwnedItem::getItemSlug)
                .collect(Collectors.toCollection(HashSet::new));

        LootboxItem rolled = rollItemNotOwned(ownedSlugs);
        stats.setAvailableLootboxes(stats.getAvailableLootboxes() - 1);

        if (ownedSlugs.contains(rolled.slug())) {
            return new OpenResult(rolled, false,
                    "Masz juz ten przedmiot — nie dodano duplikatu do ekwipunku.");
        }
        ownedItemRepository.save(new OwnedItem(user, rolled.slug()));
        return new OpenResult(rolled, true, null);
    }

    @Transactional(readOnly = true)
    public List<OwnedItem> inventoryOf(Long userId) {
        return ownedItemRepository.findByUserIdOrderByObtainedAtDesc(userId);
    }

    /** Losuje item z waga rarity. */
    private LootboxItem rollItem() {
        return rollItemFromPool(new ArrayList<>(LOOTBOX_ITEMS));
    }

    /** Losuje item, ktorego gracz jeszcze nie ma. Gdy ma caly katalog — zwykly roll bez zapisu. */
    private LootboxItem rollItemNotOwned(Set<String> ownedSlugs) {
        List<LootboxItem> unowned = LOOTBOX_ITEMS.stream()
                .filter(i -> !ownedSlugs.contains(i.slug()))
                .collect(Collectors.toList());
        if (unowned.isEmpty()) {
            return rollItem();
        }
        return rollItemFromPool(unowned);
    }

    private LootboxItem rollItemFromPool(List<LootboxItem> pool) {
        if (pool.isEmpty()) {
            return LOOTBOX_ITEMS.get(ThreadLocalRandom.current().nextInt(LOOTBOX_ITEMS.size()));
        }
        int totalWeight = 0;
        for (Rarity r : Rarity.values()) {
            boolean has = pool.stream().anyMatch(i -> i.rarity() == r);
            if (has) totalWeight += r.weight;
        }
        if (totalWeight <= 0) {
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        Rarity picked = Rarity.COMMON;
        int acc = 0;
        for (Rarity r : Rarity.values()) {
            if (pool.stream().noneMatch(i -> i.rarity() == r)) continue;
            acc += r.weight;
            if (roll < acc) { picked = r; break; }
        }
        final Rarity selected = picked;
        List<LootboxItem> rarityPool = pool.stream()
                .filter(i -> i.rarity() == selected).collect(Collectors.toList());
        if (rarityPool.isEmpty()) rarityPool = pool;
        return rarityPool.get(ThreadLocalRandom.current().nextInt(rarityPool.size()));
    }

    /**
     * Tworzy "tasme" pseudolosowych itemow do animacji CS2 - 50 itemow,
     * pozycja 40 to faktyczny wynik. Reszta to wizualny szum (przeważnie
     * zwykle itemy + kilka rzadszych).
     */
    public List<LootboxItem> rollVisualStrip(LootboxItem winner) {
        List<LootboxItem> strip = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            if (i == 40) {
                strip.add(winner);
            } else {
                strip.add(LOOTBOX_ITEMS.get(ThreadLocalRandom.current().nextInt(LOOTBOX_ITEMS.size())));
            }
        }
        return strip;
    }

    /** Zwraca slownik slug -> item dla wszystkich katalogowanych. */
    public static Map<String, LootboxItem> bySlug() {
        return LOOTBOX_ITEMS.stream().collect(Collectors.toMap(LootboxItem::slug, i -> i));
    }
}
