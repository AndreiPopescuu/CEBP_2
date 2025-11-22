import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CitizenGUI extends JFrame {

    private final JTextField nameField;
    private final JTextArea logArea;
    private final HttpClient httpClient;

    public CitizenGUI() {
        // 1. Configurare Fereastră
        setTitle("Portalul Cetățeanului (Client App)");
        setSize(600, 600); // Mai mare pentru butonul de test
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Setup HTTP Client
        httpClient = HttpClient.newHttpClient();

        // --- PANOU DE SUS (Input) ---
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Nume Cetățean:"));
        nameField = new JTextField("Cetatean-" + new Random().nextInt(100), 15);
        topPanel.add(nameField);
        add(topPanel, BorderLayout.NORTH);

        // --- PANOU CENTRAL (Butoane Documente) ---
        JPanel centerPanel = new JPanel(new GridLayout(5, 2, 10, 10)); // 5 randuri
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton btnPassport = new JButton("📘 PAȘAPORT (Final Boss)");
        JButton btnID = new JButton("🪪 Carte de Identitate");
        JButton btnBirth = new JButton("👶 Certificat de Naștere");
        JButton btnMarriage = new JButton("💍 Certificat de Căsătorie");
        JButton btnFiscal = new JButton("💰 Certificat Fiscal");
        JButton btnDomicile = new JButton("🏠 Adeverință Domiciliu");
        JButton btnTax = new JButton("🧾 Chitanță Taxă");
        JButton btnRequest = new JButton("📝 Cerere Pașaport");

        // Butonul Special de STRESS TEST
        JButton btnStress = new JButton("⚔️ TEST CONCURENȚĂ (2 Clienți)");
        btnStress.setBackground(Color.PINK);
        btnStress.setFont(new Font("Arial", Font.BOLD, 12));

        // Adăugăm acțiuni
        btnPassport.addActionListener(e -> sendRequest(nameField.getText(), "Pasaport"));
        btnID.addActionListener(e -> sendRequest(nameField.getText(), "Carte de identitate"));
        btnBirth.addActionListener(e -> sendRequest(nameField.getText(), "Certificat de nastere"));
        btnMarriage.addActionListener(e -> sendRequest(nameField.getText(), "Certificat de casatorie"));
        btnFiscal.addActionListener(e -> sendRequest(nameField.getText(), "Certificat fiscal"));
        btnDomicile.addActionListener(e -> sendRequest(nameField.getText(), "Adeverinta de domiciliu"));
        btnTax.addActionListener(e -> sendRequest(nameField.getText(), "Chitanta taxa pasaport"));
        btnRequest.addActionListener(e -> sendRequest(nameField.getText(), "Cerere pasaport"));

        // Acțiunea de Stress Test
        btnStress.addActionListener(e -> runConcurrencyTest());

        // Adăugare în panou
        centerPanel.add(btnPassport);   centerPanel.add(btnID);
        centerPanel.add(btnBirth);      centerPanel.add(btnMarriage);
        centerPanel.add(btnFiscal);     centerPanel.add(btnDomicile);
        centerPanel.add(btnTax);        centerPanel.add(btnRequest);

        // Adaugam butonul de stress pe tot randul de jos
        JPanel stressPanel = new JPanel(new BorderLayout());
        stressPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        stressPanel.add(btnStress, BorderLayout.CENTER);

        // Container intermediar
        JPanel mainCenter = new JPanel(new BorderLayout());
        mainCenter.add(centerPanel, BorderLayout.CENTER);
        mainCenter.add(stressPanel, BorderLayout.SOUTH);

        add(mainCenter, BorderLayout.CENTER);

        // --- PANOU DE JOS (Loguri) ---
        logArea = new JTextArea(12, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Status Cereri"));
        add(scrollPane, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Metoda care lansează 2 clienți simultan
    // Metoda care lansează 5 clienți simultan
    private void runConcurrencyTest() {
        log("⚡ PORNIRE MEGA-STRESS TEST (5 CLIENȚI)...");
        log("Se trimit 5 cereri simultane la Primărie...");

        for (int i = 1; i <= 5; i++) {
            // Trebuie să facem variabilele "finale" pentru a le folosi în lambda
            final String numeClient = "Concurent-" + i;

            // Lansăm cererea pe un thread separat (asincron)
            CompletableFuture.runAsync(() ->
                    sendRequest(numeClient, "Certificat de nastere")
            );
        }

        // Notă: Toți 5 vor cere "Certificat de naștere" pentru a bloca Ghișeul 1
        // și a forța creșterea cozii.
    }

    private void sendRequest(String inputName, String documentType) {
        // 1. Creăm o variabilă nouă 'finală' pe care NU o mai modificăm
        final String name = (inputName == null || inputName.trim().isEmpty()) ? "Anonim" : inputName;

        String jsonBody = String.format("{\"name\": \"%s\", \"documents\": [\"%s\"]}", name, documentType);
        log("📤 [" + name + "] cere: " + documentType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/citizens/apply"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Acum folosim 'name' care este sigur și final
                    if (response.statusCode() == 200 || response.statusCode() == 202) {
                        log("✅ [" + name + "] Cerere acceptată.");
                    } else {
                        log("❌ [" + name + "] EROARE: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    log("❌ EROARE CONEXIUNE");
                    return null;
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