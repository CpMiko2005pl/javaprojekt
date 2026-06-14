package pl.pb.monopoly.service;

import pl.pb.monopoly.domain.GamePlayer;

import java.util.Map;
import java.util.Set;

/**
 * Ekonomia gry w stylu Business Tour — stałe globalne, ceny pól, tablice czynszów
 * oraz logika kosztów ulepszeń (każdy poziom = cena zakupu gruntu).
 */
public final class GameEconomy {

    private GameEconomy() {
    }

    // --- Stałe globalne (Business Tour) ---

    public static final int STARTING_CASH = 2_000_000;
    public static final int GO_BONUS = 300_000;
    public static final int JAIL_BAIL = 200_000;
    /** Koszt lotu / World Tour (lotniska). */
    public static final int FLY_COST = 50_000;
    public static final double TAX_RATE = 0.10;
    public static final int SCHOLARSHIP_BONUS = 150_000;
    public static final int SHIELD_MAX_ABSORB = 300_000;
    public static final double BANK_SELL_RATE = 0.70;
    public static final int RESORT_BUY_PRICE = 200_000;
    public static final int UTILITY_BUY_PRICE = 150_000;

    /** Czynsz resortów wg liczby posiadanych: 1→50k, 2→100k, 3→200k, 4→400k. */
    public static final int[] RESORT_RENT = {0, 50_000, 100_000, 200_000, 400_000};

    /** Mnożnik czynszu Wodociągów PB (suma oczek × mnożnik). */
    public static final int UTILITY_RENT_MULTIPLIER = 15_000;

    public static final int POS_START = 0;
    public static final int POS_JAIL = 10;
    public static final int POS_GO_TO_JAIL = 30;
    public static final int POS_UTILITY = 28;

    /** 8 grup kolorystycznych — 22 nieruchomości (2+3+3+3+3+3+3+2). */
    public static final int[][] COLOR_GROUPS = {
            {1, 3},              // brązowa — 60k
            {6, 8, 9},           // jasnoniebieska — 80k
            {11, 13, 14},        // różowa — 100k
            {12, 16, 18},        // pomarańczowa — 120k
            {19, 21, 23},        // czerwona — 160k
            {26, 27, 29},        // żółta — 220k
            {31, 32, 34},        // zielona — 300k
            {24, 37}             // ciemnoniebieska — 350k / 400k
    };

    /** Pola resortów (lotniska / dworce). */
    public static final boolean[] RESORT_TILES = new boolean[40];

    static {
        RESORT_TILES[5] = true;
        RESORT_TILES[15] = true;
        RESORT_TILES[25] = true;
        RESORT_TILES[35] = true;
    }

    /** Indeks grupy koloru dla każdego pola (-1 = brak). */
    public static final int[] TILE_COLOR_GROUP = buildColorGroupIndex();

    /**
     * Ceny zakupu pól. -1 = nie do kupienia.
     * Progres nieruchomości: 60k → 400k (22 pola w 8 grupach) + 4 resorty po 200k.
     */
    public static final int[] TILE_PRICE = {
            -1, 60_000, -1, 60_000, -1, RESORT_BUY_PRICE, 80_000, -1, 80_000, 80_000,
            -1, 100_000, 120_000, 100_000, 100_000, RESORT_BUY_PRICE, 120_000, -1, 120_000, 160_000,
            -1, 160_000, -1, 160_000, 350_000, RESORT_BUY_PRICE, 220_000, 220_000, UTILITY_BUY_PRICE, 220_000,
            -1, 300_000, 300_000, -1, 300_000, RESORT_BUY_PRICE, -1, 400_000, -1, -1
    };

    /**
     * Czynsze nieruchomości: [rent0, lvl1, lvl2, lvl3, biurowiec].
     * Rent monopol (cały kolor, 0 budynków) = 2 × rent0 — liczone w {@link #rentForLevel}.
     */
    public static final int[][] TILE_RENT_TABLE = buildRentTable();

    private static int[] buildColorGroupIndex() {
        int[] idx = new int[40];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = -1;
        }
        for (int g = 0; g < COLOR_GROUPS.length; g++) {
            for (int pos : COLOR_GROUPS[g]) {
                idx[pos] = g;
            }
        }
        return idx;
    }

    private static int[][] buildRentTable() {
        int[][] rents = new int[40][];
        setRent(rents, 1, 6_000, 24_000, 48_000, 72_000, 180_000);
        setRent(rents, 3, 6_000, 24_000, 48_000, 72_000, 180_000);
        setRent(rents, 6, 8_000, 32_000, 64_000, 96_000, 240_000);
        setRent(rents, 8, 8_000, 32_000, 64_000, 96_000, 240_000);
        setRent(rents, 9, 8_000, 32_000, 64_000, 96_000, 240_000);
        setRent(rents, 11, 10_000, 40_000, 80_000, 120_000, 350_000);
        setRent(rents, 13, 10_000, 40_000, 80_000, 120_000, 350_000);
        setRent(rents, 14, 10_000, 40_000, 80_000, 120_000, 350_000);
        setRent(rents, 12, 12_000, 48_000, 96_000, 144_000, 450_000);
        setRent(rents, 16, 12_000, 48_000, 96_000, 144_000, 450_000);
        setRent(rents, 18, 12_000, 48_000, 96_000, 144_000, 450_000);
        setRent(rents, 19, 16_000, 64_000, 128_000, 192_000, 600_000);
        setRent(rents, 21, 16_000, 64_000, 128_000, 192_000, 600_000);
        setRent(rents, 23, 16_000, 64_000, 128_000, 192_000, 600_000);
        setRent(rents, 26, 22_000, 88_000, 176_000, 264_000, 850_000);
        setRent(rents, 27, 22_000, 88_000, 176_000, 264_000, 850_000);
        setRent(rents, 29, 22_000, 88_000, 176_000, 264_000, 850_000);
        setRent(rents, 31, 30_000, 120_000, 240_000, 360_000, 1_200_000);
        setRent(rents, 32, 30_000, 120_000, 240_000, 360_000, 1_200_000);
        setRent(rents, 34, 30_000, 120_000, 240_000, 360_000, 1_200_000);
        setRent(rents, 24, 35_000, 140_000, 280_000, 420_000, 1_600_000);
        setRent(rents, 37, 40_000, 160_000, 320_000, 480_000, 2_000_000);
        return rents;
    }

    private static void setRent(int[][] rents, int pos, int r0, int r1, int r2, int r3, int rMax) {
        rents[pos] = new int[]{r0, r1, r2, r3, rMax};
    }

    /** Koszt każdego poziomu ulepszenia = bazowa cena zakupu gruntu. */
    public static int upgradeCost(int pos) {
        int price = TILE_PRICE[pos];
        return price > 0 && !RESORT_TILES[pos] && pos != POS_UTILITY ? price : 0;
    }

    /** Maksymalny poziom ulepszenia (4 = Biurowiec). */
    public static final int MAX_PROPERTY_LEVEL = 4;

    /** Czynsz dla danego poziomu budynku (0–4). Monopol dotyczy tylko poziomu 0. */
    public static int rentForLevel(int pos, int level, boolean monopoly) {
        if (pos < 0 || pos >= 40 || TILE_PRICE[pos] <= 0) {
            return 0;
        }
        int[] r = TILE_RENT_TABLE[pos];
        if (r == null) {
            return 0;
        }
        return switch (level) {
            case 0 -> monopoly ? r[0] * 2 : r[0];
            case 1 -> r[1];
            case 2 -> r[2];
            case 3 -> r[3];
            case 4 -> r[4];
            default -> r[0];
        };
    }

    /** Czy gracz posiada pełną grupę kolorystyczną danego pola. */
    public static boolean hasColorMonopoly(GamePlayer owner, int pos) {
        int groupIdx = TILE_COLOR_GROUP[pos];
        if (groupIdx < 0 || owner == null) {
            return false;
        }
        for (int member : COLOR_GROUPS[groupIdx]) {
            if (!owner.getOwnedPositions().contains(member)) {
                return false;
            }
        }
        return true;
    }

    /** Wartość majątku gracza: suma cen gruntów + kosztów postawionych ulepszeń. */
    public static int computePropertyNetWorth(Set<Integer> ownedPositions, Map<Integer, Integer> propertyLevels) {
        int total = 0;
        for (int pos : ownedPositions) {
            int buy = TILE_PRICE[pos];
            if (buy <= 0) {
                continue;
            }
            int level = propertyLevels.getOrDefault(pos, 0);
            total += buy + (long) buy * level;
        }
        return total;
    }

    /** Podatek: 10% wartości majątku nieruchomościowego. */
    public static int computeTax(GamePlayer player) {
        int worth = computePropertyNetWorth(player.getOwnedPositions(), player.getPropertyLevels());
        if (worth <= 0) {
            return 0;
        }
        return (int) Math.round(worth * TAX_RATE);
    }

    /** Wypłata od banku — 70% ceny zakupu gruntu (ulepszenia nie podlegają zwrotowi). */
    public static int sellPrice(int position) {
        int price = TILE_PRICE[position];
        return price > 0 ? (int) Math.round(price * BANK_SELL_RATE) : 0;
    }

}
