package gui;

import agents.AgentCoordination;
import com.formdev.flatlaf.FlatDarkLaf;
import models.Task;
import models.Event;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

public class InterfaceGraphique extends JFrame {
    private JTable tableTaches, tableEvenements;
    private DefaultTableModel modelTaches, modelEvenements;
    private JTextArea zoneRecherche, zoneErreurs;
    private JTextField champCommande;
    private JTabbedPane onglets;
    private JLabel statusLabel;
    private AgentCoordination agentCoordination;
    private JTextField nomUtilisateurField;
    private JPasswordField motDePasseField;
    private JPanel mainPanel;

    public InterfaceGraphique(AgentCoordination agentCoordination) {
        this.agentCoordination = agentCoordination;

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 15);
            UIManager.put("TextComponent.arc", 15);
            UIManager.put("TabbedPane.selectedBackground", new Color(45, 45, 60));
        } catch (Exception e) {
            System.err.println("Échec de l'application du thème FlatLaf: " + e.getMessage());
        }

        setTitle("SmartAssistant");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(650, 450));

        // Initialize login panel
        showLoginPanel();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showLoginPanel() {
        Font regularFont = new Font("Segoe UI", Font.PLAIN, 14);

        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBackground(new Color(35, 35, 50));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel usernameLabel = new JLabel("Nom d'utilisateur:");
        usernameLabel.setFont(regularFont);
        usernameLabel.setForeground(Color.WHITE);
        nomUtilisateurField = new JTextField();
        nomUtilisateurField.setFont(regularFont);
        nomUtilisateurField.setBackground(new Color(60, 63, 65));
        nomUtilisateurField.setForeground(Color.WHITE);
        nomUtilisateurField.setCaretColor(Color.WHITE);

        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setFont(regularFont);
        passwordLabel.setForeground(Color.WHITE);
        motDePasseField = new JPasswordField();
        motDePasseField.setFont(regularFont);
        motDePasseField.setBackground(new Color(60, 63, 65));
        motDePasseField.setForeground(Color.WHITE);
        motDePasseField.setCaretColor(Color.WHITE);

        JButton loginButton = new JButton("Se connecter");
        loginButton.setFont(regularFont);
        loginButton.setBackground(new Color(76, 175, 80));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> {
            String nomUtilisateur = nomUtilisateurField.getText();
            String motDePasse = new String(motDePasseField.getPassword());
            if (agentCoordination.connecterUtilisateur(nomUtilisateur, motDePasse)) {
                agentCoordination.getClientAgent().setLoggedIn(true); // Notify ClientAgent of login
                showMainInterface();
            } else {
                JOptionPane.showMessageDialog(this, "Échec de la connexion.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginPanel.add(usernameLabel);
        loginPanel.add(nomUtilisateurField);
        loginPanel.add(passwordLabel);
        loginPanel.add(motDePasseField);
        loginPanel.add(new JLabel());
        loginPanel.add(loginButton);

        getContentPane().removeAll();
        add(loginPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showMainInterface() {
        getContentPane().removeAll();

        Font titleFont = new Font("Segoe UI", Font.BOLD, 20);
        Font regularFont = new Font("Segoe UI", Font.PLAIN, 14);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(25, 25, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel titleLabel = new JLabel("SmartAssistant", JLabel.LEFT);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(3, 169, 244));
        try {
            URL iconUrl = new URL("https://raw.githubusercontent.com/google/material-design-icons/master/png/action/assistant/black/48dp.png");
            titleLabel.setIcon(new ImageIcon(iconUrl));
        } catch (Exception e) {
            System.err.println("Échec du chargement de l'icône: " + e.getMessage());
        }
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel commandPanel = new JPanel(new BorderLayout(10, 0));
        commandPanel.setBackground(new Color(35, 35, 50));
        commandPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel commandLabel = new JLabel("Commande : ");
        commandLabel.setFont(regularFont);
        commandLabel.setForeground(Color.WHITE);
        champCommande = new JTextField();
        champCommande.setFont(regularFont);
        champCommande.setBackground(new Color(60, 63, 65));
        champCommande.setForeground(Color.WHITE);
        champCommande.setCaretColor(Color.WHITE);
        JButton sendButton = new JButton("Envoyer");
        sendButton.setFont(regularFont);
        sendButton.setBackground(new Color(76, 175, 80));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(this::envoyerCommande);
        JButton clearButton = new JButton("Effacer");
        clearButton.setFont(regularFont);
        clearButton.setBackground(new Color(244, 67, 54));
        clearButton.setForeground(Color.WHITE);
        clearButton.addActionListener(e -> champCommande.setText(""));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(commandPanel.getBackground());
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);
        commandPanel.add(commandLabel, BorderLayout.WEST);
        commandPanel.add(champCommande, BorderLayout.CENTER);
        commandPanel.add(buttonPanel, BorderLayout.EAST);

        onglets = new JTabbedPane();
        onglets.setFont(regularFont);
        onglets.setBackground(new Color(45, 45, 60));
        onglets.setForeground(Color.WHITE);

        String[] colonnesTaches = {"Description", "Priorité"};
        modelTaches = new DefaultTableModel(colonnesTaches, 0);
        tableTaches = new JTable(modelTaches);
        styleTable(tableTaches);
        onglets.addTab("Tâches", new JScrollPane(tableTaches));

        String[] colonnesEvenements = {"Description", "Date"};
        modelEvenements = new DefaultTableModel(colonnesEvenements, 0);
        tableEvenements = new JTable(modelEvenements);
        styleTable(tableEvenements);
        onglets.addTab("Événements", new JScrollPane(tableEvenements));

        zoneRecherche = new JTextArea();
        zoneRecherche.setEditable(false);
        zoneRecherche.setFont(regularFont);
        zoneRecherche.setBackground(new Color(60, 63, 65));
        zoneRecherche.setForeground(Color.WHITE);
        onglets.addTab("Recherche", new JScrollPane(zoneRecherche));

        zoneErreurs = new JTextArea();
        zoneErreurs.setEditable(false);
        zoneErreurs.setFont(regularFont);
        zoneErreurs.setBackground(new Color(60, 63, 65));
        zoneErreurs.setForeground(new Color(255, 82, 82));
        onglets.addTab("Erreurs", new JScrollPane(zoneErreurs));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(25, 25, 40));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        statusLabel = new JLabel("Prêt");
        statusLabel.setFont(regularFont);
        statusLabel.setForeground(new Color(200, 200, 200));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(commandPanel, BorderLayout.CENTER);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(onglets, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();

        // Populate tables with user's tasks and events after login
        updateTables();
    }

    private void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(new Color(60, 63, 65));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(90, 90, 90));
        table.getTableHeader().setBackground(new Color(33, 150, 243));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(60, 63, 65) : new Color(70, 73, 75));
                c.setForeground(Color.WHITE);
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void updateTables() {
        // Update tasks table
        modelTaches.setRowCount(0);
        for (Task task : agentCoordination.getUtilisateurActuel().getTaches()) {
            modelTaches.addRow(new Object[]{task.getDescription(), task.getPriority()});
        }

        // Update events table
        modelEvenements.setRowCount(0);
        for (Event event : agentCoordination.getUtilisateurActuel().getEvenements()) {
            modelEvenements.addRow(new Object[]{event.getTitre(), event.getDate()});
        }
    }

    private void envoyerCommande(ActionEvent e) {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty()) {
            afficherReponse("Erreur : Commande vide.");
            statusLabel.setText("Erreur: Commande vide.");
            return;
        }

        try {
            // Validation minimale pour les commandes "ajoute"
            if (commande.toLowerCase().startsWith("ajoute ") && commande.length() < 10) {
                afficherReponse("Erreur : Description de tâche trop courte.");
                statusLabel.setText("Erreur: Description trop courte");
                return;
            }

            // Send command via ClientAgent (accessed through AgentCoordination)
            agentCoordination.getClientAgent().envoyerCommande(commande);
            champCommande.setText("");
            statusLabel.setText("Commande envoyée: " + commande);
        } catch (Exception ex) {
            afficherReponse("Erreur : " + ex.getMessage());
            statusLabel.setText("Erreur: " + ex.getMessage());
        }
    }

    public void afficherReponse(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (message.equalsIgnoreCase("Aucun événement trouvé.") || message.toLowerCase().startsWith("événements :")) {
                    updateTables(); // Refresh tables to show updated events
                    onglets.setSelectedIndex(onglets.indexOfTab("Événements"));
                    statusLabel.setText("Liste des événements mise à jour");
                } else if (message.equalsIgnoreCase("Aucune tâche trouvée.") || message.toLowerCase().startsWith("tâches :")) {
                    updateTables(); // Refresh tables to show updated tasks
                    onglets.setSelectedIndex(onglets.indexOfTab("Tâches"));
                    statusLabel.setText("Liste des tâches mise à jour");
                } else if (message.toLowerCase().startsWith("météo à") || message.toLowerCase().startsWith("recette :") ||
                        message.toLowerCase().startsWith("extrait wikipedia :") || message.toLowerCase().startsWith("1. ")) {
                    zoneRecherche.setText(message);
                    onglets.setSelectedIndex(onglets.indexOfTab("Recherche"));
                    statusLabel.setText("Résultats de recherche mis à jour");
                } else if (message.toLowerCase().startsWith("erreur :")) {
                    zoneErreurs.append(message + "\n");
                    onglets.setSelectedIndex(onglets.indexOfTab("Erreurs"));
                    statusLabel.setText("Erreur enregistrée");
                } else {
                    JOptionPane.showMessageDialog(null, message, "SmartAssistant", JOptionPane.INFORMATION_MESSAGE);
                    statusLabel.setText("Réponse reçue");
                }
            } catch (Exception e) {
                zoneErreurs.append("Erreur d'analyse de la réponse : " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(null, "Erreur d'analyse de la réponse : " + e.getMessage(), "SmartAssistant", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Erreur d'analyse de la réponse");
            }
        });
    }

    public String showTimePicker() {
        JDialog dialog = new JDialog(this, "Sélectionner l'heure de l'événement", true);
        dialog.setLayout(new FlowLayout());
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        Font regularFont = new Font("Segoe UI", Font.PLAIN, 14);

        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = String.format("%02d", i);
        }
        String[] minutes = {"00", "15", "30", "45"};

        JComboBox<String> hourCombo = new JComboBox<>(hours);
        JComboBox<String> minuteCombo = new JComboBox<>(minutes);
        hourCombo.setFont(regularFont);
        minuteCombo.setFont(regularFont);
        hourCombo.setBackground(new Color(60, 63, 65));
        minuteCombo.setBackground(new Color(60, 63, 65));
        hourCombo.setForeground(Color.WHITE);
        minuteCombo.setForeground(Color.WHITE);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        hourCombo.setSelectedIndex(cal.get(java.util.Calendar.HOUR_OF_DAY));
        minuteCombo.setSelectedIndex(0);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");
        okButton.setFont(regularFont);
        cancelButton.setFont(regularFont);
        okButton.setBackground(new Color(76, 175, 80));
        cancelButton.setBackground(new Color(244, 67, 54));
        okButton.setForeground(Color.WHITE);
        cancelButton.setForeground(Color.WHITE);

        okButton.addActionListener(e -> dialog.dispose());
        cancelButton.addActionListener(e -> {
            hourCombo.setSelectedItem(null);
            dialog.dispose();
        });

        dialog.add(new JLabel("Heure: ", SwingConstants.RIGHT));
        dialog.add(hourCombo);
        dialog.add(new JLabel(":", SwingConstants.CENTER));
        dialog.add(minuteCombo);
        dialog.add(okButton);
        dialog.add(cancelButton);

        dialog.getContentPane().setBackground(new Color(35, 35, 50));
        dialog.setVisible(true);

        if (hourCombo.getSelectedItem() == null || minuteCombo.getSelectedItem() == null) {
            return null;
        }
        return hourCombo.getSelectedItem() + ":" + minuteCombo.getSelectedItem();
    }
}