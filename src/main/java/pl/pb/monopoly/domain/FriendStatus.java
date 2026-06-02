package pl.pb.monopoly.domain;

/** Status relacji znajomosci miedzy graczami. */
public enum FriendStatus {
    /** Zaproszenie wyslane, oczekuje na akceptacje. */
    PENDING,
    /** Zaproszenie zaakceptowane - gracze sa znajomymi. */
    ACCEPTED
}
