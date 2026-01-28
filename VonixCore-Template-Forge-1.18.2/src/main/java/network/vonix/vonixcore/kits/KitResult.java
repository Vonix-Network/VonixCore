package network.vonix.vonixcore.kits;

public class KitResult {

    private final Status status;
    private final long cooldown;

    public KitResult(Status status, long cooldown) {
        this.status = status;
        this.cooldown = cooldown;
    }

    public Status getStatus() {
        return status;
    }

    public long getCooldown() {
        return cooldown;
    }

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        ON_COOLDOWN,
        ALREADY_CLAIMED
    }
}
