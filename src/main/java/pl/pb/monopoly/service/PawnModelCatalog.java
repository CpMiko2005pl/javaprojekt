package pl.pb.monopoly.service;

import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.service.LootboxService.LootboxItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mapowanie pionkow 3D ze skrzynki (slug lootbox) na plik GLB w /models/.
 */
public final class PawnModelCatalog {

    private static final Map<String, String> SLUG_TO_FILE = Map.ofEntries(
            Map.entry("pawn-3d-corn", "CornPawn.glb"),
            Map.entry("pawn-3d-steve", "PawnSteve.glb"),
            Map.entry("pawn-3d-creeper", "PawnCreeper.glb"),
            Map.entry("pawn-3d-enderman", "PawnEnderman.glb"),
            Map.entry("pawn-3d-skeleton", "PawnSkeleton.glb"),
            Map.entry("pawn-3d-piglin", "PawnPiglin.glb"),
            Map.entry("pawn-3d-pillager", "PawnPillager.glb"),
            Map.entry("pawn-3d-penguin", "PawnPenguin.glb"),
            Map.entry("pawn-3d-goblin", "PawnGoblin.glb")
    );

    private PawnModelCatalog() {
    }

    public static boolean isPawn3dItem(LootboxItem item) {
        return item != null && "Pionek 3D".equals(item.category());
    }

    public static String pathForSlug(String slug) {
        String file = SLUG_TO_FILE.get(slug);
        return file != null ? "/models/" + file : null;
    }

    /** Sciezka do modelu zalozonego pionka 3D lub null (domyslny walec). */
    public static String equippedModelPath(Collection<OwnedItem> equipped) {
        if (equipped == null) {
            return null;
        }
        for (OwnedItem item : equipped) {
            LootboxItem catalog = LootboxService.findBySlug(item.getItemSlug());
            if (catalog != null && isPawn3dItem(catalog)) {
                return pathForSlug(item.getItemSlug());
            }
        }
        return null;
    }

    /** Wszystkie sciezki modeli — do preloadu na planszy 3D. */
    public static List<String> allModelPaths() {
        return SLUG_TO_FILE.values().stream()
                .map(f -> "/models/" + f)
                .distinct()
                .toList();
    }
}
