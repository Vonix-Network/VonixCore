package network.vonix.vonixcore.economy.rewards;

public class DailyRewardResult {

    private final Status status;
    private final long cooldown;
    private final double amount;
    private final int streak;

    public DailyRewardResult(Status status, long cooldown, double amount, int streak) {
        this.status = status;
        this.cooldown = cooldown;
        this.amount = amount;
        this.streak = streak;
    }

    public Status getStatus() {
        return status;
    }

    public long getCooldown() {
        return cooldown;
    }

    public double getAmount() {
        return amount;
    }

    public int getStreak() {
        return streak;
    }

    public enum Status {
        SUCCESS,
        COOLDOWN,
        ERROR
    }
}
