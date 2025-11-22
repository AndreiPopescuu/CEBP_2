package com.bureaucracy.service.domain;

import java.util.ArrayList;
import java.util.List;

public class Office {
    private final String name;
    private final List<Counter> counterList = new ArrayList<>();
    private final List<Document> canEmit = new ArrayList<>();
    private int rrIndex = 0;

    public Office(String name) { this.name = name; }

    public String getName() { return name; }

    public void addCounter(Counter counter) { counterList.add(counter); }

    public void addDocument(Document document) {
        canEmit.add(document);
        document.setEmittingOffice(this);
    }

    public void startAllCounters() { counterList.forEach(Counter::start); }

    public void submit(CitizenAgent client, Document document) {
        if (!canEmit.contains(document)) throw new IllegalArgumentException("Office cannot issue " + document);
        Counter counter = pickCounter();
        counter.enqueue(client, document);
    }

    private synchronized Counter pickCounter() {
        if (counterList.isEmpty()) throw new IllegalStateException("No counters in " + name);
        Counter c = counterList.get(rrIndex % counterList.size());
        rrIndex++;
        return c;
    }
}