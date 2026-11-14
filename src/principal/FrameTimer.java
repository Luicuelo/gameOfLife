package principal;

/**
 * Converts the pulses of the animation loop into a fixed number of simulation
 * ticks per second, so the speed of the game no longer depends on the refresh
 * rate of the display (60 Hz, 144 Hz, ...) or on how fast the machine is.
 * <p>
 * The timer only answers "how many ticks are due now"; advancing the game is
 * the caller's job.
 */
final class FrameTimer {

    /** Length of one tick of the game: sixty per second. */
    private static final long TICK_NANOS = 1_000_000_000L / 60;
    /**
     * Longest delay one pulse makes up: four ticks, so down to ~15 frames per
     * second.
     */
    private static final int MAX_TICKS_PER_PULSE = 4;

    /** Timestamp of the previous pulse, in nanoseconds; -1 before the first one. */
    private long last = -1;
    /** Time left over from the previous pulses, in nanoseconds. */
    private long pending;

    /**
     * Answers how many ticks the game has to advance to keep its speed.
     *
     * @param now timestamp of the pulse, in nanoseconds
     * @return ticks to advance now: 0, 1, or more if the machine is falling behind
     */
    int ticksFor(long now) {
        if (last < 0) {
            last = now;
            return 0;                      // the first pulse only opens the count
        }
        long elapsed = Math.max(0,
                Math.min(now - last, TICK_NANOS * MAX_TICKS_PER_PULSE));
        last = now;
        pending += elapsed;

        int ticks = 0;
        while (pending >= TICK_NANOS) {
            pending -= TICK_NANOS;
            ticks++;
        }
        return ticks;
    }

    /**
     * Drops the time accumulated so far. Called when the game resumes after a
     * pause, so the frames spent paused are not paid back in one burst.
     */
    void reset() {
        last = -1;
        pending = 0;
    }
}
