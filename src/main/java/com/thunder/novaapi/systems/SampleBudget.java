package com.thunder.novaapi.systems;

public final class SampleBudget {
    private int remaining;

    public SampleBudget(int maxSamples) {
        this.remaining = Math.max(0, maxSamples);
    }

    public boolean tryConsume(int amount) {
        int cost = Math.max(0, amount);
        if (remaining < cost) {
            return false;
        }
        remaining -= cost;
        return true;
    }

    public int remaining() {
        return remaining;
    }
}
