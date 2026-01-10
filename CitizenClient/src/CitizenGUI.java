import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CitizenGUI extends JFrame {

    private final JTextField nameField;
    private final JTextArea logArea;
    private final HttpClient httpClient;

    private final List<JCheckBox> documentCheckboxes = new ArrayList<>();

    public CitizenGUI() {
        setTitle("Portalul Cetățeanului - AI Integrated");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        httpClient = HttpClient.newHttpClient();

        // --- TOP PANEL ---
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Nume Cetățean:"));
        nameField = new JTextField("Cetatean-" + new Random().nextInt(100), 15);
        topPanel.add(nameField);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL (Checkboxes) ---
        JPanel centerContainer = new JPanel(new BorderLayout());

        JPanel checkboxPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        String[] docNames = {
                "Pasaport",
                "Carte de identitate",
                "Certificat de nastere",
                "Certificat de casatorie",
                "Certificat fiscal",
                "Adeverinta de domiciliu",
                "Chitanta taxa pasaport",
                "Cerere pasaport"
        };

        for (String docName : docNames) {
            JCheckBox checkBox = new JCheckBox(docName);
            checkBox.setFont(new Font("Arial", Font.BOLD, 14));
            documentCheckboxes.add(checkBox);
            checkboxPanel.add(checkBox);
        }

        JButton btnStress = new JButton("⚔️ TEST CONCURENȚĂ (5 Clienți)");
        btnStress.setFont(new Font("Arial", Font.BOLD, 12));
        btnStress.addActionListener(e -> runConcurrencyTest());

        JPanel stressPanel = new JPanel(new BorderLayout());
        stressPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        stressPanel.add(btnStress, BorderLayout.CENTER);

        centerContainer.add(checkboxPanel, BorderLayout.CENTER);
        centerContainer.add(stressPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        // --- BOTTOM PANEL ---
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JButton submitButton = new JButton("TRIMITE CEREREA (APPLY)");
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        submitButton.addActionListener(this::onSubmit);

        logArea = new JTextArea(14, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Status & AI Reports"));

        bottomPanel.add(submitButton, BorderLayout.NORTH);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void onSubmit(ActionEvent e) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduceți un nume!");
            return;
        }

        List<String> selectedDocs = new ArrayList<>();
        for (JCheckBox box : documentCheckboxes) {
            if (box.isSelected()) selectedDocs.add(box.getText());
        }

        if (selectedDocs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selectați cel puțin un document!");
            return;
        }

        sendRequest(name, selectedDocs);
    }

    private void runConcurrencyTest() {
        log("⚡ PORNIRE MEGA-STRESS TEST (5 CLIENȚI)...");
        for (int i = 1; i <= 5; i++) {
            final String numeClient = "Concurent-" + i;
            CompletableFuture.runAsync(() ->
                    sendRequest(numeClient, Collections.singletonList("Certificat de nastere"))
            );
        }
    }

    private void sendRequest(String inputName, List<String> documents) {
        final String name = (inputName == null || inputName.trim().isEmpty()) ? "Anonim" : inputName;

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"name\":\"").append(name).append("\",");
        jsonBuilder.append("\"documents\":[");
        for (int i = 0; i < documents.size(); i++) {
            jsonBuilder.append("\"").append(documents.get(i)).append("\"");
            if (i < documents.size() - 1) jsonBuilder.append(",");
        }
        jsonBuilder.append("]");
        jsonBuilder.append("}");

        log("📤 [" + name + "] cere: " + documents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/citizens/apply"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBuilder.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 202) {
                        log("✅ [" + name + "] Cerere acceptată. Așteptăm AI...");
                        // START LISTENING FOR THE AI REPORT
                        startPollingForReport(name);
                    } else {
                        log("❌ [" + name + "] EROARE: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    log("❌ EROARE CONEXIUNE: " + e.getMessage());
                    return null;
                });
    }

    // --- NEW: Polling Method for AI Report ---
    private void startPollingForReport(String name) {
        CompletableFuture.runAsync(() -> {
            try {
                int attempts = 0;
                // Try for 40 seconds (20 * 2s)
                while (attempts < 20) {
                    Thread.sleep(2000); // Wait 2 seconds

                    HttpRequest pollRequest = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/citizens/report/" + name))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(pollRequest, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String report = response.body();
                        SwingUtilities.invokeLater(() -> {
                            logArea.append("\n✨✨ RAPORT AI PENTRU " + name + " ✨✨\n");
                            logArea.append(report + "\n");
                            logArea.append("------------------------------------------------\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                        break; // Stop polling
                    }
                    attempts++;
                }
            } catch (Exception e) {
                // Silent fail or minimal log
            }
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CitizenGUI::new);
    }
}