package com.bureaucracy.service.controller;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.config.BureaucracyRegistry;
import com.bureaucracy.service.domain.CitizenAgent;
import com.bureaucracy.service.domain.Document;
import com.bureaucracy.service.domain.RequestService;
import com.bureaucracy.service.entity.Citizen;
import com.bureaucracy.service.repository.CitizenRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener; // Import
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/citizens")
public class BureaucracyController {

    private final BureaucracyRegistry registry;
    private final EventPublisher publisher;
    private final CitizenRepository citizenRepo;
    private final RequestService requestService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // --- NEW: Store reports here ---
    // Maps "Alin" -> "Alin waited 5 minutes..."
    private static final Map<String, String> completedReports = new ConcurrentHashMap<>();

    public BureaucracyController(BureaucracyRegistry registry,
                                 EventPublisher publisher,
                                 CitizenRepository citizenRepo,
                                 RequestService requestService) {
        this.registry = registry;
        this.publisher = publisher;
        this.citizenRepo = citizenRepo;
        this.requestService = requestService;
    }
    @Bean
    public Queue aiResponsesQueue() {
        return new Queue("ai-responses", false);
    }

    // --- NEW: LISTENER FOR RETURNED MESSAGES ---
    @RabbitListener(queues = "ai-responses")
    public void receiveAiReport(String message) {
        // We expect "Name###Report"
        String[] parts = message.split("###", 2);
        if (parts.length == 2) {
            completedReports.put(parts[0], parts[1]);
            System.out.println("✅ SERVER A: Received Report for " + parts[0]);
        }
    }

    // --- NEW: ENDPOINT FOR GUI TO CHECK ---
    @GetMapping("/report/{name}")
    public ResponseEntity<String> getReport(@PathVariable String name) {
        if (completedReports.containsKey(name)) {
            String report = completedReports.get(name);
            completedReports.remove(name); // Clean up after reading
            return ResponseEntity.ok(report);
        } else {
            return ResponseEntity.status(202).body("PENDING");
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<String> apply(@RequestBody RequestData request) {
        // ... (Keep existing code exactly the same) ...
        Citizen citizen = citizenRepo.findByName(request.name)
                .orElseGet(() -> citizenRepo.save(new Citizen(request.name)));

        List<Document> docs = new ArrayList<>();
        for (String d : request.documents) {
            Document doc = registry.getDocumentByName(d);
            if (doc != null) docs.add(doc);
        }

        CitizenAgent agent = new CitizenAgent(citizen, docs, publisher, requestService);
        executor.submit(agent);

        return ResponseEntity.ok("Application started for " + request.name);
    }

    public static class RequestData {
        public String name;
        public List<String> documents;
    }
}