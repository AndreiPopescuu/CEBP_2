package com.bureaucracy.service.domain;

import java.util.ArrayList;
import java.util.List;

public class Office {
    private final String name;
    private final List<Counter> counters = new ArrayList<>();

    public Office(String name) {
        this.name = name;
    }

    public void addCounter(Counter counter) {
        this.counters.add(counter);
    }

    public void startAllCounters() {
        for (Counter c : counters) {
            c.start(); // This assumes your Counter has a start() method!
        }
    }

    public void submit(CitizenAgent agent, Document doc) {
        if (counters.isEmpty()) {
            System.err.println("❌ ERROR: Office '" + name + "' has NO open counters! Client " + agent.getName() + " is stranded.");
            return;
        }

        // Round-robin or Hash-based selection
        // We use Math.abs because hashCode can be negative, which crashes lists
        int index = Math.abs(agent.getName().hashCode()) % counters.size();
        Counter selectedCounter = counters.get(index);

        System.out.println("DEBUG: " + name + " assigned " + agent.getName() + " to " + selectedCounter.getName());

        selectedCounter.enqueue(agent, doc);
    }

    public String getName() { return name; }
}