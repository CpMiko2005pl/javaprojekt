package pl.pb.monopoly.domain;

/** Stan sesji rozgrywki. */
public enum GameStatus {
    /** Pokoj utworzony, czeka na graczy. */
    WAITING,
    /** Trwa rozgrywka. */
    ACTIVE,
    /** Rozgrywka zakonczona. */
    FINISHED
}
