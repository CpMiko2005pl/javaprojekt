package pl.pb.monopoly.service;

import org.springframework.stereotype.Component;
import pl.pb.monopoly.domain.GameSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Magazyn aktywnych rozgrywek (status ACTIVE) trzymanych w PAMIECI serwera (RAM),
 * a nie w PostgreSQL. Dzieki temu kazdy ruch (roll/buy/skip/...) operuje na obiekcie
 * w pamieci — bez setek zapytan SQL i zapisow encji na ruch.
 *
 * <p>Przechowujemy te same obiekty {@link GameSession} (wraz z {@code players}), ale
 * <b>odlaczone</b> od kontekstu JPA (po {@code EntityManager.detach}). Sa wiec zwyklymi
 * POJO w RAM. Trwaly zapis do DB nastepuje tylko przy starcie (lobby) i koncu meczu.
 *
 * <p><b>Bezpieczenstwo watkowe:</b> REST (ruchy gracza), {@code BotAutoplayService}
 * (timery botow) oraz watek harmonogramu moga trafic na te sama sesje rownolegle.
 * Mutacje wspoldzielonego obiektu w RAM musza byc serializowane — sluzy do tego
 * {@link #lockFor(Long)} (jeden {@link ReentrantLock} na sesje).
 *
 * <p><b>Ograniczenie:</b> restart JVM kasuje cala zawartosc RAM. Aktywne gry przepadaja;
 * przy starcie aplikacji osierocone wpisy ACTIVE w DB sa oznaczane jako FINISHED
 * (patrz hook startowy).
 */
@Component
public class ActiveGameStore {

    private final Map<Long, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /** Czy sesja jest aktualnie obslugiwana z RAM. */
    public boolean contains(Long sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    /** Zwraca obiekt sesji z RAM lub {@code null}, gdy nie jest aktywna w pamieci. */
    public GameSession get(Long sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    /** Wstawia/aktualizuje sesje w RAM (klucz = id sesji). */
    public void put(GameSession session) {
        if (session != null && session.getId() != null) {
            sessions.put(session.getId(), session);
        }
    }

    /** Usuwa sesje z RAM (po zakonczeniu meczu / przez admina). */
    public void remove(Long sessionId) {
        if (sessionId == null) return;
        sessions.remove(sessionId);
        // Watek konczacy mecz trzyma referencje do locka i sam go zwolni;
        // kolejni wolajacy i tak pojda sciezka DB (sesji nie ma juz w RAM).
        locks.remove(sessionId);
    }

    /** Migawka wszystkich aktywnych sesji w RAM (np. dla harmonogramu limitu czasu). */
    public Collection<GameSession> activeSessions() {
        return sessions.values();
    }

    /** Liczba aktywnych gier w RAM. */
    public int size() {
        return sessions.size();
    }

    /**
     * Zwraca (tworzac w razie potrzeby) lock dla danej sesji. Wolajacy zawsze
     * powinien zwolnic go w bloku {@code finally}.
     */
    public ReentrantLock lockFor(Long sessionId) {
        return locks.computeIfAbsent(sessionId, k -> new ReentrantLock());
    }
}
