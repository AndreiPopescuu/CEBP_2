package com.bureaucracy.insight.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LogListener {

    // Memorie temporară (Buffer) pentru a stoca logurile
    private final List<String> logBuffer = new ArrayList<>();

    // Serviciul nostru care vorbește cu Google Gemini
    private final AiAnalyst aiAnalyst;

    // Injectăm Analistul prin constructor
    public LogListener(AiAnalyst aiAnalyst) {
        this.aiAnalyst = aiAnalyst;
    }

    /**
     * Această metodă ascultă coada RabbitMQ "bureaucracy-logs".
     * Se activează automat de fiecare dată când Serverul 1 trimite un mesaj.
     */
    @RabbitListener(queues = "bureaucracy-logs")
    public void receiveMessage(Map<String, Object> event) {
        // 1. Extragem mesajul text din eveniment
        String message = (String) event.get("message");
        String clientName = (String) event.get("client");

        // 2. Îl afișăm în consolă ca să vedem că sistemul merge live
        System.out.println("📥 [RECEIVED on 8081]: " + message);

        // 3. Adăugăm mesajul în buffer (într-un bloc sincronizat pentru siguranță)
        synchronized (logBuffer) {
            logBuffer.add(message);

            // 4. Verificăm dacă am strâns destule date pentru o analiză (Batching)
            // Am pus 7 ca să prindem un flux complet de client (Start -> Cozi -> Finish)
            if (logBuffer.size() >= 7) {
                triggerAIAnalysis();
            }
        }
    }

    /**
     * Trimite datele colectate către AI și golește buffer-ul.
     */
    private void triggerAIAnalysis() {
        System.out.println("\n⏳ --- BATCH COMPLET (7 loguri). TRIMIT LA AI... ---");

        // Facem o copie a listei curente.
        // De ce? Ca să putem goli buffer-ul imediat pentru noi mesaje,
        // în timp ce AI-ul procesează copia veche.
        List<String> logsToAnalyze = new ArrayList<>(logBuffer);

        // Golim buffer-ul principal pentru a face loc la date noi
        logBuffer.clear();

        // Apelăm serviciul care face request-ul HTTP la Google
        // (Aceasta poate dura 1-2 secunde, deci e bine că am eliberat buffer-ul)
        aiAnalyst.analyzeLogs(logsToAnalyze);
    }
}