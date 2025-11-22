package com.bureaucracy.service.domain;

import com.bureaucracy.service.EventPublisher;
import java.util.*;

public class Counter {
    private final String name;
    private final Queue<HashMap<CitizenAgent, Document>> queue = new LinkedList<>();
    private volatile boolean running = false;
    private final EventPublisher publisher;
    private Thread worker;

    // Generator de numere aleatorii pentru pauza de cafea
    private final Random rnd = new Random();

    public Counter(String name, EventPublisher publisher) {
        this.name = name;
        this.publisher = publisher;
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::runLoop, "Counter-" + name);
        worker.start();
    }

    public void stop() {
        running = false;
        synchronized (queue) { queue.notifyAll(); }
    }

    public void enqueue(CitizenAgent client, Document document) {
        HashMap<CitizenAgent, Document> entry = new HashMap<>();
        entry.put(client, document);
        synchronized (queue) {
            queue.add(entry);
            // Publicăm evenimentul că s-a așezat la coadă
            publisher.publishEvent("QUEUE_JOIN", client.getName(),
                    name + ": Joined queue for " + document + ". Position: " + queue.size());
            queue.notifyAll();
        }
    }

    private void runLoop() {
        while (running) {
            HashMap<CitizenAgent, Document> entry;
            synchronized (queue) {
                while (running && queue.isEmpty()) {
                    try { queue.wait(); } catch (InterruptedException ignored) { }
                }
                if (!running) break;
                entry = queue.poll();
            }

            if (entry != null) {
                // --- AICI ESTE LOGICA DE COFFEE BREAK ---
                // Înainte de a procesa clientul, verificăm dacă funcționarul vrea cafea
                maybeTakeCoffeeBreak();

                Map.Entry<CitizenAgent, Document> req = entry.entrySet().iterator().next();
                CitizenAgent client = req.getKey();
                Document doc = req.getValue();

                // Simulăm timpul de procesare a actului (muncă)
                try { Thread.sleep(200); } catch (InterruptedException e) {}

                publisher.publishEvent("DOC_ISSUED", client.getName(),
                        name + " issued " + doc + " to " + client.getName());

                client.onDocumentIssued(doc);
            }
        }
    }

    /**
     * Funcționarul are 10% șanse să ia o pauză înainte de a servi următorul client.
     */
    private void maybeTakeCoffeeBreak() {
        // 10% șansă (0.1)
        if (rnd.nextDouble() < 0.1) {
            publisher.publishEvent("COFFEE_BREAK", "System",
                    "☕ " + name + " is taking a coffee break... (Queue halted)");

            try {
                // Pauza durează între 1 și 2 secunde
                Thread.sleep(1000 + rnd.nextInt(1000));
            } catch (InterruptedException ignored) {}

            publisher.publishEvent("COFFEE_BREAK_END", "System",
                    "✅ " + name + " is back from coffee break.");
        }
    }
}