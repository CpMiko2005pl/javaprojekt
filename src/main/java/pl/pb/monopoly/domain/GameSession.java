package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "current_turn", nullable = false)
    private int currentTurn = 0;

    @Column(name = "pending_purchase_pos")
    private Integer pendingPurchasePos;

    @Column(name = "pending_purchase_price")
    private Integer pendingPurchasePrice;

    @Column(name = "pending_decider_id")
    private Long pendingDeciderId;

    @Column(name = "pending_payment_debtor_id")
    private Long pendingPaymentDebtorId;

    @Column(name = "pending_payment_amount")
    private Integer pendingPaymentAmount;

    @Column(name = "pending_payment_creditor_id")
    private Long pendingPaymentCreditorId;

    @Column(name = "pending_payment_reason", length = 120)
    private String pendingPaymentReason;

    @Column(name = "pending_upgrade_pos")
    private Integer pendingUpgradePos;

    @Column(name = "pending_upgrade_player_id")
    private Long pendingUpgradePlayerId;

    @Column(name = "pending_upgrade_cost")
    private Integer pendingUpgradeCost;

    @Column(name = "pending_buyback_pos")
    private Integer pendingBuybackPos;

    @Column(name = "pending_buyback_victim_id")
    private Long pendingBuybackVictimId;

    @Column(name = "pending_buyback_holder_id")
    private Long pendingBuybackHolderId;

    @Column(name = "pending_buyback_price")
    private Integer pendingBuybackPrice;

    @Column(name = "pending_extra_roll_player_id")
    private Long pendingExtraRollPlayerId;

    @Column(name = "pending_takeover_pos")
    private Integer pendingTakeoverPos;

    @Column(name = "pending_takeover_buyer_id")
    private Long pendingTakeoverBuyerId;

    @Column(name = "pending_takeover_seller_id")
    private Long pendingTakeoverSellerId;

    @Column(name = "pending_takeover_price")
    private Integer pendingTakeoverPrice;

    @Column(name = "leader_id")
    private Long leaderId;

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

    public void removePlayer(GamePlayer player) {
        if (!players.remove(player)) return;
        player.setSession(null);
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setTurnOrder(i);
        }
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

    public Integer getPendingUpgradePos() { return pendingUpgradePos; }
    public void setPendingUpgradePos(Integer pendingUpgradePos) { this.pendingUpgradePos = pendingUpgradePos; }

    public Long getPendingUpgradePlayerId() { return pendingUpgradePlayerId; }
    public void setPendingUpgradePlayerId(Long pendingUpgradePlayerId) { this.pendingUpgradePlayerId = pendingUpgradePlayerId; }

    public Integer getPendingUpgradeCost() { return pendingUpgradeCost; }
    public void setPendingUpgradeCost(Integer pendingUpgradeCost) { this.pendingUpgradeCost = pendingUpgradeCost; }

    public void clearPendingUpgrade() {
        this.pendingUpgradePos = null;
        this.pendingUpgradePlayerId = null;
        this.pendingUpgradeCost = null;
    }

    public Integer getPendingBuybackPos() { return pendingBuybackPos; }
    public void setPendingBuybackPos(Integer pendingBuybackPos) { this.pendingBuybackPos = pendingBuybackPos; }

    public Long getPendingBuybackVictimId() { return pendingBuybackVictimId; }
    public void setPendingBuybackVictimId(Long pendingBuybackVictimId) { this.pendingBuybackVictimId = pendingBuybackVictimId; }

    public Long getPendingBuybackHolderId() { return pendingBuybackHolderId; }
    public void setPendingBuybackHolderId(Long pendingBuybackHolderId) { this.pendingBuybackHolderId = pendingBuybackHolderId; }

    public Integer getPendingBuybackPrice() { return pendingBuybackPrice; }
    public void setPendingBuybackPrice(Integer pendingBuybackPrice) { this.pendingBuybackPrice = pendingBuybackPrice; }

    public void clearPendingBuyback() {
        this.pendingBuybackPos = null;
        this.pendingBuybackVictimId = null;
        this.pendingBuybackHolderId = null;
        this.pendingBuybackPrice = null;
    }

    public Long getPendingExtraRollPlayerId() { return pendingExtraRollPlayerId; }
    public void setPendingExtraRollPlayerId(Long pendingExtraRollPlayerId) { this.pendingExtraRollPlayerId = pendingExtraRollPlayerId; }

    public Integer getPendingTakeoverPos() { return pendingTakeoverPos; }
    public void setPendingTakeoverPos(Integer pendingTakeoverPos) { this.pendingTakeoverPos = pendingTakeoverPos; }

    public Long getPendingTakeoverBuyerId() { return pendingTakeoverBuyerId; }
    public void setPendingTakeoverBuyerId(Long pendingTakeoverBuyerId) { this.pendingTakeoverBuyerId = pendingTakeoverBuyerId; }

    public Long getPendingTakeoverSellerId() { return pendingTakeoverSellerId; }
    public void setPendingTakeoverSellerId(Long pendingTakeoverSellerId) { this.pendingTakeoverSellerId = pendingTakeoverSellerId; }

    public Integer getPendingTakeoverPrice() { return pendingTakeoverPrice; }
    public void setPendingTakeoverPrice(Integer pendingTakeoverPrice) { this.pendingTakeoverPrice = pendingTakeoverPrice; }

    public void clearPendingTakeover() {
        this.pendingTakeoverPos = null;
        this.pendingTakeoverBuyerId = null;
        this.pendingTakeoverSellerId = null;
        this.pendingTakeoverPrice = null;
    }

    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
}
