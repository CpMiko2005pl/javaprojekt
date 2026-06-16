package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.HandCardType;
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

    public record LootboxItem(
            String slug,
            String name,
            Rarity rarity,
            String category,
            String iconClass,
            String description
    ) {}

    public static final java.util.Map<String, HandCardType> BONUS_CARD_MAP = java.util.Map.of(
            "card-bonus-upgrade", HandCardType.FREE_UPGRADE,
            "card-bonus-shield", HandCardType.SHIELD,
            "card-bonus-cash", HandCardType.ADD_CASH,
            "card-bonus-teleport", HandCardType.TELEPORT,
            "card-bonus-double", HandCardType.DOUBLE_RENT_NEXT
    );

    public static final List<LootboxItem> LOOTBOX_ITEMS = List.of(

            new LootboxItem("color-blue", "Pionek: Kobalt PB", Rarity.COMMON, "Kolor pionka",
                    "fa-solid fa-circle", "Niebieski pionek w barwach uczelni."),
            new LootboxItem("color-red", "Pionek: Czerwien Wydzialu", Rarity.COMMON, "Kolor pionka",
                    "fa-solid fa-circle", "Klasyczna czerwien."),
            new LootboxItem("emoji-coffee", "Naklejka: Kawa z USOSweb", Rarity.COMMON, "Naklejka",
                    "fa-solid fa-mug-hot", "Nieskonczona ilosc kofeiny."),
            new LootboxItem("emoji-pizza", "Naklejka: Pizza ze Stolowki", Rarity.COMMON, "Naklejka",
                    "fa-solid fa-pizza-slice", "Tylko za 8 PLN po promocji."),
            new LootboxItem("frame-bronze", "Ramka: Brazowa", Rarity.COMMON, "Ramka",
                    "fa-solid fa-square-poll-vertical", "Skromna, ale wlasna."),
            new LootboxItem("frame-pixel", "Ramka: Pixel Retro", Rarity.COMMON, "Ramka",
                    "fa-solid fa-gamepad", "8-bit styl rodem z lat 80."),
            new LootboxItem("card-bonus-shield", "Karta bonusowa: Tarcza Akademicka", Rarity.COMMON, "Karta bonusowa",
                    "fa-solid fa-user-shield", "Dodana do reki na START nastepnej gry."),
            new LootboxItem("card-bonus-cash", "Karta bonusowa: Stypendium Rektora", Rarity.COMMON, "Karta bonusowa",
                    "fa-solid fa-coins", "Dodana do reki na START nastepnej gry."),

            new LootboxItem("color-emerald", "Pionek: Szmaragd", Rarity.RARE, "Kolor pionka",
                    "fa-solid fa-circle", "Zielen kampusowych traw."),
            new LootboxItem("color-pink", "Pionek: Pink Hype", Rarity.RARE, "Kolor pionka",
                    "fa-solid fa-circle", "Dla odwaznych."),
            new LootboxItem("frame-silver", "Ramka: Srebrna", Rarity.RARE, "Ramka",
                    "fa-solid fa-medal", "Stylowa, blyszczy w ciemnosci."),
            new LootboxItem("frame-midnight", "Ramka: Polnocna Biblioteka", Rarity.RARE, "Ramka",
                    "fa-solid fa-moon", "Ciemny granat — klimat nocnych sesji."),
            new LootboxItem("frame-ice", "Ramka: Lodowa", Rarity.RARE, "Ramka",
                    "fa-solid fa-snowflake", "Blekitna ramka z lodowym efektem."),
            new LootboxItem("frame-dice", "Ramka: Kostki", Rarity.RARE, "Ramka",
                    "fa-solid fa-dice", "Kostki w rogach — dla hardkorowcow planszowek."),
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
            new LootboxItem("card-bonus-upgrade", "Karta bonusowa: Akademickie Pozwolenie Budowlane", Rarity.RARE, "Karta bonusowa",
                    "fa-solid fa-hard-hat", "Dodana do reki na START nastepnej gry."),
            new LootboxItem("card-bonus-double", "Karta bonusowa: Podwojny Wymagacz", Rarity.RARE, "Karta bonusowa",
                    "fa-solid fa-sack-dollar", "Dodana do reki na START nastepnej gry."),

            new LootboxItem("color-neon", "Pionek: Neon Cyber", Rarity.EPIC, "Kolor pionka",
                    "fa-solid fa-circle-radiation", "Swieci w 3D na planszy."),
            new LootboxItem("frame-gold", "Ramka: Zlota", Rarity.EPIC, "Ramka",
                    "fa-solid fa-crown", "Tylko dla najlepszych."),
            new LootboxItem("frame-neon-green", "Ramka: Neon Kampus", Rarity.EPIC, "Ramka",
                    "fa-solid fa-bolt", "Zielona poswata w barwach PB."),
            new LootboxItem("frame-fire", "Ramka: Ognista", Rarity.EPIC, "Ramka",
                    "fa-solid fa-fire", "Pomaranczowo-czerwona flaga zwyciestwa."),
            new LootboxItem("frame-monopoly", "Ramka: Plansza PB", Rarity.EPIC, "Ramka",
                    "fa-solid fa-chess-board", "Miniaturowa plansza w ramce."),
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
            new LootboxItem("card-bonus-teleport", "Karta bonusowa: Teleport Kampusowy", Rarity.EPIC, "Karta bonusowa",
                    "fa-solid fa-rocket", "Dodana do reki na START nastepnej gry."),

            new LootboxItem("frame-rainbow", "Ramka: Tecza", Rarity.LEGENDARY, "Ramka",
                    "fa-solid fa-rainbow", "Animowana, spektakularna."),
            new LootboxItem("frame-champion", "Ramka: Mistrz Areny", Rarity.LEGENDARY, "Ramka",
                    "fa-solid fa-chess-king", "Zloto i korona — dla prawdziwych mistrzow planszy."),
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

    public static String equipSlot(String category) {
        if (category == null) return "";
        return switch (category) {
            case "Pionek 3D", "Kolor pionka" -> "pawn";
            default -> category;
        };
    }

    public record ShopBox(String key, String name, String description, String iconClass,
                          int price, Set<String> categories) {}

    public static final List<ShopBox> SHOP_BOXES = List.of(
            new ShopBox("pawns", "Skrzynka Pionkow", "Pionki 3D i kolory pionka.",
                    "fa-solid fa-chess-pawn", 300, Set.of("Pionek 3D", "Kolor pionka")),
            new ShopBox("titles", "Skrzynka Tytulow", "Tytuly wyswietlane na profilu.",
                    "fa-solid fa-ranking-star", 250, Set.of("Tytul")),
            new ShopBox("frames", "Skrzynka Ramek", "Ramki awatara.",
                    "fa-solid fa-image", 350, Set.of("Ramka"))
    );

    public static List<ShopBox> shopBoxes() {
        return SHOP_BOXES;
    }

    public static ShopBox findBox(String key) {
        return SHOP_BOXES.stream().filter(b -> b.key().equals(key)).findFirst().orElse(null);
    }

    public record BuyResult(LootboxItem item, boolean addedToInventory, String inventoryNote, int coinsLeft) {}

    @Transactional
    public BuyResult buyBox(String username, String boxKey) {
        ShopBox box = findBox(boxKey);
        if (box == null) {
            throw new IllegalArgumentException("Nieznana skrzynka.");
        }
        User user = userRepository.findByUsername(username).orElseThrow();
        if (user.getCoins() < box.price()) {
            throw new IllegalArgumentException("Za malo monet — potrzebujesz " + box.price() + ".");
        }
        user.setCoins(user.getCoins() - box.price());

        Set<String> ownedSlugs = ownedItemRepository.findByUserIdOrderByObtainedAtDesc(user.getId()).stream()
                .map(OwnedItem::getItemSlug)
                .collect(Collectors.toCollection(HashSet::new));

        List<LootboxItem> pool = LOOTBOX_ITEMS.stream()
                .filter(i -> box.categories().contains(i.category()))
                .collect(Collectors.toList());
        List<LootboxItem> unowned = pool.stream()
                .filter(i -> !ownedSlugs.contains(i.slug()))
                .collect(Collectors.toList());
        LootboxItem rolled = rollItemFromPool(unowned.isEmpty() ? pool : unowned);

        userRepository.save(user);

        if (ownedSlugs.contains(rolled.slug())) {
            return new BuyResult(rolled, false,
                    "Masz juz ten przedmiot — monety zwracane nie sa, ale duplikatu nie dodano.",
                    user.getCoins());
        }
        OwnedItem owned = ownedItemRepository.save(new OwnedItem(user, rolled.slug()));
        if (PawnModelCatalog.isPawn3dItem(rolled)) {
            equipPawn3d(user, owned);
        }
        return new BuyResult(rolled, true, null, user.getCoins());
    }

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

    public record OpenResult(LootboxItem item, boolean addedToInventory, String inventoryNote) {}

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
        OwnedItem owned = ownedItemRepository.save(new OwnedItem(user, rolled.slug()));
        if (PawnModelCatalog.isPawn3dItem(rolled)) {
            equipPawn3d(user, owned);
        } else if ("Karta bonusowa".equals(rolled.category())) {
            equipBonusCard(user, owned);
        }
        return new OpenResult(rolled, true, null);
    }

    private void equipBonusCard(User user, OwnedItem item) {
        HandCardType cardType = BONUS_CARD_MAP.get(item.getItemSlug());
        if (cardType == null) return;
        PlayerStatistics stats = user.getStatistics();
        if (stats == null) return;

        ownedItemRepository.findByUserIdOrderByObtainedAtDesc(user.getId()).stream()
                .filter(o -> !o.getId().equals(item.getId()))
                .filter(o -> "Karta bonusowa".equals(findBySlug(o.getItemSlug()).category()))
                .forEach(o -> {
                    o.setEquipped(false);
                    ownedItemRepository.save(o);
                });

        if (stats.getPendingWheelCard() == null) {
            stats.setPendingWheelCard(cardType.name());
            item.setEquipped(true);
            ownedItemRepository.save(item);
        }
    }

    private void equipPawn3d(User user, OwnedItem item) {
        ownedItemRepository.findByUserIdOrderByObtainedAtDesc(user.getId()).stream()
                .filter(o -> !o.getId().equals(item.getId()))
                .filter(o -> {
                    LootboxItem c = findBySlug(o.getItemSlug());
                    return c != null && "pawn".equals(equipSlot(c.category()));
                })
                .forEach(o -> {
                    o.setEquipped(false);
                    ownedItemRepository.save(o);
                });
        item.setEquipped(true);
        ownedItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<OwnedItem> inventoryOf(Long userId) {
        return ownedItemRepository.findByUserIdOrderByObtainedAtDesc(userId);
    }

    private LootboxItem rollItem() {
        return rollItemFromPool(new ArrayList<>(LOOTBOX_ITEMS));
    }

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

    public List<LootboxItem> rollVisualStrip(LootboxItem winner) {
        return rollVisualStrip(winner, LOOTBOX_ITEMS);
    }

    public List<LootboxItem> rollVisualStrip(LootboxItem winner, List<LootboxItem> pool) {
        List<LootboxItem> noisePool = (pool == null || pool.isEmpty()) ? LOOTBOX_ITEMS : pool;
        List<LootboxItem> strip = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            if (i == 40) {
                strip.add(winner);
            } else {
                strip.add(noisePool.get(ThreadLocalRandom.current().nextInt(noisePool.size())));
            }
        }
        return strip;
    }

    public List<LootboxItem> poolForBox(String boxKey) {
        ShopBox box = findBox(boxKey);
        if (box == null) return LOOTBOX_ITEMS;
        return LOOTBOX_ITEMS.stream()
                .filter(i -> box.categories().contains(i.category()))
                .collect(Collectors.toList());
    }

    public static Map<String, LootboxItem> bySlug() {
        return LOOTBOX_ITEMS.stream().collect(Collectors.toMap(LootboxItem::slug, i -> i));
    }
}
