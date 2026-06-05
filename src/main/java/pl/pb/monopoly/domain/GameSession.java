package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesja rozgrywki (pokoj). KOMPOZYCJA z {@link GamePlayer} - gracze w sesji
 * sa usuwani razem z nia (cascade ALL + orphanRemoval).
 */
@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Krotki kod pokoju do zapraszania znajomych. */
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Indeks gracza, ktory aktualnie wykonuje ruch (rozgrywka hot-seat). */
    @Column(name = "current_turn", nullable = false)
    private int currentTurn = 0;

    /**
     * Pozycja pola, ktore aktualnie czeka na decyzje "kup / pomin" od gracza.
     * Gdy != null, kolejny rzut kostka jest zablokowany - gracz musi
     * wykonac decyzje. Inni gracze moga w tym czasie zlozyc oferte (licytacja).
     */
    @Column(name = "pending_purchase_pos")
    private Integer pendingPurchasePos;

    @Column(name = "pending_purchase_price")
    private Integer pendingPurchasePrice;

    /** ID gracza, ktory ma decyzje na temat kupna pola (= currentTurnPlayer). */
    @Column(name = "pending_decider_id")
    private Long pendingDeciderId;

    /** Gracz, ktory musi uregulowac zobowiazanie (brak siana na oplate). */
    @Column(name = "pending_payment_debtor_id")
    private Long pendingPaymentDebtorId;

    @Column(name = "pending_payment_amount")
    private Integer pendingPaymentAmount;

    /** Wlasciciel pola / odbiorca czynszu; null = oplata do banku (podatek, karta). */
    @Column(name = "pending_payment_creditor_id")
    private Long pendingPaymentCreditorId;

    @Column(name = "pending_payment_reason", length = 120)
    private String pendingPaymentReason;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnOrder asc")
    private List<GamePlayer> players = new ArrayList<>();

    public GameSession() {
    }

    public void addPlayer(GamePlayer player) {
        player.setSession(this);
        player.setTurnOrder(players.size());
        players.add(player);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<GamePlayer> players) {
        this.players = players;
    }

    public Integer getPendingPurchasePos() {
        return pendingPurchasePos;
    }

    public void setPendingPurchasePos(Integer pendingPurchasePos) {
        this.pendingPurchasePos = pendingPurchasePos;
    }

    public Integer getPendingPurchasePrice() {
        return pendingPurchasePrice;
    }

    public void setPendingPurchasePrice(Integer pendingPurchasePrice) {
        this.pendingPurchasePrice = pendingPurchasePrice;
    }

    public Long getPendingDeciderId() {
        return pendingDeciderId;
    }

    public void setPendingDeciderId(Long pendingDeciderId) {
        this.pendingDeciderId = pendingDeciderId;
    }

    public void clearPendingPurchase() {
        this.pendingPurchasePos = null;
        this.pendingPurchasePrice = null;
        this.pendingDeciderId = null;
    }

    public Long getPendingPaymentDebtorId() {
        return pendingPaymentDebtorId;
    }

    public void setPendingPaymentDebtorId(Long pendingPaymentDebtorId) {
        this.pendingPaymentDebtorId = pendingPaymentDebtorId;
    }

    public Integer getPendingPaymentAmount() {
        return pendingPaymentAmount;
    }

    public void setPendingPaymentAmount(Integer pendingPaymentAmount) {
        this.pendingPaymentAmount = pendingPaymentAmount;
    }

    public Long getPendingPaymentCreditorId() {
        return pendingPaymentCreditorId;
    }

    public void setPendingPaymentCreditorId(Long pendingPaymentCreditorId) {
        this.pendingPaymentCreditorId = pendingPaymentCreditorId;
    }

    public String getPendingPaymentReason() {
        return pendingPaymentReason;
    }

    public void setPendingPaymentReason(String pendingPaymentReason) {
        this.pendingPaymentReason = pendingPaymentReason;
    }

    public void clearPendingPayment() {
        this.pendingPaymentDebtorId = null;
        this.pendingPaymentAmount = null;
        this.pendingPaymentCreditorId = null;
        this.pendingPaymentReason = null;
    }
}
