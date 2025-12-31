package com.example.qidian.service;

import java.util.concurrent.ThreadLocalRandom;

public class RateLimiter {

    private final int minDelayMs;
    private final int maxDelayMs;
    private long lastTs = 0L;

    public RateLimiter(int minDelayMs, int maxDelayMs) {
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = Math.max(maxDelayMs, minDelayMs);
    }

    public synchronized void sleepBetweenRequests() {
        int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        long now = System.currentTimeMillis();
        long elapsed = now - lastTs;
        long toSleep = delay - elapsed;
        if (toSleep > 0) {
            try {
                Thread.sleep(toSleep);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        lastTs = System.currentTimeMillis();
    }
}
