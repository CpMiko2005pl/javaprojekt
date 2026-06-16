package pl.pb.monopoly.service;

import org.junit.jupiter.api.Test;
import pl.pb.monopoly.domain.GameSession;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

class ActiveGameStoreTest {

    private GameSession session(long id) {
        GameSession s = new GameSession();
        s.setId(id);
        return s;
    }

    @Test
    void putGetContainsRemove() {
        ActiveGameStore store = new ActiveGameStore();
        assertFalse(store.contains(1L));
        assertNull(store.get(1L));

        GameSession s = session(1L);
        store.put(s);
        assertTrue(store.contains(1L));
        assertSame(s, store.get(1L));
        assertEquals(1, store.size());

        store.remove(1L);
        assertFalse(store.contains(1L));
        assertNull(store.get(1L));
        assertEquals(0, store.size());
    }

    @Test
    void nullSafeOperations() {
        ActiveGameStore store = new ActiveGameStore();
        assertFalse(store.contains(null));
        assertNull(store.get(null));
        store.put(null);
        store.put(new GameSession());
        store.remove(null);
        assertEquals(0, store.size());
    }

    @Test
    void lockForReturnsSameInstancePerSession() {
        ActiveGameStore store = new ActiveGameStore();
        ReentrantLock a1 = store.lockFor(7L);
        ReentrantLock a2 = store.lockFor(7L);
        ReentrantLock b = store.lockFor(8L);
        assertSame(a1, a2);
        assertNotSame(a1, b);
    }

    @Test
    void lockSerializesConcurrentMutations() throws InterruptedException {
        ActiveGameStore store = new ActiveGameStore();
        final Long id = 42L;
        final int threads = 8;
        final int incrementsPerThread = 5_000;
        final int[] sharedCounter = {0};
        final AtomicInteger interleavings = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        ReentrantLock lock = store.lockFor(id);
                        lock.lock();
                        try {
                            int before = sharedCounter[0];
                            sharedCounter[0] = before + 1;
                            if (sharedCounter[0] != before + 1) interleavings.incrementAndGet();
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "watki nie zdazyly");
        pool.shutdownNow();

        assertEquals(threads * incrementsPerThread, sharedCounter[0],
                "lock per sesji powinien serializowac mutacje (brak utraconych inkrementacji)");
        assertEquals(0, interleavings.get());
    }
}
