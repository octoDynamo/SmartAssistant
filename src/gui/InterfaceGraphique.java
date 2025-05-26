package gui;

import agents.ClientAgent;
import com.formdev.flatlaf.FlatDarkLaf;
import jade.core.Agent;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.text.SimpleDateFormat;

public class InterfaceGraphique extends JFrame {
    private JTable tableTaches, tableEvenements;
    private DefaultTableModel modelTaches, modelEvenements;
    private JTextArea zoneRecherche, zoneErreurs;
    private JTextField champCommande;
    private JTabbedPane onglets;
    private JLabel statusLabel;
    private Agent agent;

    public InterfaceGraphique(Agent agent) {
        this.agent = agent;

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 15);
            UIManager.put("TextComponent.arc", 15);
            UIManager.put("TabbedPane.selectedBackground", new Color(45, 45, 60));
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        setTitle("SmartAssistant");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(650, 450));

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
            System.err.println("Failed to load icon: " + e.getMessage());
        }
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel commandPanel = new JPanel(new BorderLayout(10, 0));
        commandPanel.setBackground(new Color(35, 35, 50));
        commandPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel commandLabel = new JLabel("Command: ");
        commandLabel.setFont(regularFont);
        commandLabel.setForeground(Color.WHITE);
        champCommande = new JTextField();
        champCommande.setFont(regularFont);
        champCommande.setBackground(new Color(60, 63, 65));
        champCommande.setForeground(Color.WHITE);
        champCommande.setCaretColor(Color.WHITE);
        JButton sendButton = new JButton("Send");
        sendButton.setFont(regularFont);
        sendButton.setBackground(new Color(76, 175, 80));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(this::envoyerCommande);
        JButton clearButton = new JButton("Clear");
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

        String[] colonnesTaches = {"Description", "Due Date", "Priority"};
        modelTaches = new DefaultTableModel(colonnesTaches, 0);
        tableTaches = new JTable(modelTaches);
        styleTable(tableTaches);
        onglets.addTab("Tasks", new JScrollPane(tableTaches));

        String[] colonnesEvenements = {"Description", "Date"};
        modelEvenements = new DefaultTableModel(colonnesEvenements, 0);
        tableEvenements = new JTable(modelEvenements);
        styleTable(tableEvenements);
        onglets.addTab("Events", new JScrollPane(tableEvenements));

        zoneRecherche = new JTextArea();
        zoneRecherche.setEditable(false);
        zoneRecherche.setFont(regularFont);
        zoneRecherche.setBackground(new Color(60, 63, 65));
        zoneRecherche.setForeground(Color.WHITE);
        onglets.addTab("Search", new JScrollPane(zoneRecherche));

        zoneErreurs = new JTextArea();
        zoneErreurs.setEditable(false);
        zoneErreurs.setFont(regularFont);
        zoneErreurs.setBackground(new Color(60, 63, 65));
        zoneErreurs.setForeground(new Color(255, 82, 82));
        onglets.addTab("Errors", new JScrollPane(zoneErreurs));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(25, 25, 40));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(regularFont);
        statusLabel.setForeground(new Color(200, 200, 200));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(commandPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        add(onglets, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
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

    private void envoyerCommande(ActionEvent e) {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty()) {
            afficherReponse("Erreur : Commande vide.");
            statusLabel.setText("Error: Empty command");
            return;
        }

        try {
            // Validate commands
            if (commande.toLowerCase().startsWith("planifie ")) {
                String[] parts = commande.substring(9).split(":");
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    afficherReponse("Erreur : Description d'événement manquante.");
                    statusLabel.setText("Error: Missing event description");
                    return;
                }
                String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31 15:00";
                try {
                    if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                        dateStr = dateStr.matches("\\d{4}-\\d{2}-\\d{2}") ? dateStr + " 15:00" : dateStr;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sdf.setLenient(false);
                    sdf.parse(dateStr);
                } catch (Exception ex) {
                    afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd ou yyyy-MM-dd HH:mm.");
                    statusLabel.setText("Error: Invalid date format");
                    return;
                }
            } else if (commande.toLowerCase().startsWith("ajoute ")) {
                String[] parts = commande.substring(7).split(":");
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    afficherReponse("Erreur : Description de tâche manquante.");
                    statusLabel.setText("Error: Missing task description");
                    return;
                }
                String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31";
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setLenient(false);
                    sdf.parse(dateStr);
                } catch (Exception ex) {
                    afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd.");
                    statusLabel.setText("Error: Invalid date format");
                    return;
                }
            } else if (commande.toLowerCase().startsWith("supprime ")) {
                String desc = commande.substring(9).replaceAll("(?i)événement ", "").trim();
                if (desc.isEmpty()) {
                    afficherReponse("Erreur : Description manquante pour suppression.");
                    statusLabel.setText("Error: Missing description");
                    return;
                }
            } else if (!commande.toLowerCase().equals("liste") && !commande.toLowerCase().equals("liste_evenements") &&
                    !commande.toLowerCase().equals("vider") && !commande.toLowerCase().startsWith("meteo ") &&
                    !commande.toLowerCase().startsWith("recette ")) {
                afficherReponse("Commande inconnue. Essayez : planifie <événement>:<date>, liste_evenements, supprime événement <nom>, ajoute <tâche>:<date>:<priorité>, supprime <tâche>, liste, vider, meteo <ville>, recette <nom>, recherche <item>");
                statusLabel.setText("Error: Unknown command");
                return;
            }

            // Delegate to ClientAgent
            ((ClientAgent) agent).envoyerCommande(commande);
            champCommande.setText("");
            statusLabel.setText("Command sent: " + commande);
        } catch (Exception ex) {
            afficherReponse("Erreur : " + ex.getMessage());
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    public void afficherReponse(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (message.toLowerCase().startsWith("liste des événements:")) {
                    modelEvenements.setRowCount(0);
                    String[] lignes = message.split("\n");
                    boolean hasEvents = false;
                    for (int i = 1; i < lignes.length; i++) {
                        String line = lignes[i].trim();
                        if (!line.isEmpty() && !line.equalsIgnoreCase("Aucun événement planifié.")) {
                            if (line.startsWith("- ")) {
                                line = line.substring(2);
                            }
                            String[] parts = line.split(" \\(Date: ");
                            if (parts.length == 2) {
                                String description = parts[0].trim();
                                String date = parts[1].replace(")", "").trim();
                                modelEvenements.addRow(new Object[]{description, date});
                                hasEvents = true;
                            }
                        }
                    }
                    if (!hasEvents) {
                        zoneErreurs.append("Aucun événement planifié.\n");
                        JOptionPane.showMessageDialog(null, "Aucun événement planifié.", "SmartAssistant", JOptionPane.INFORMATION_MESSAGE);
                    }
                    onglets.setSelectedIndex(onglets.indexOfTab("Events"));
                    statusLabel.setText("Events list updated");
                } else if (message.toLowerCase().startsWith("liste des tâches:")) {
                    modelTaches.setRowCount(0);
                    String[] lignes = message.split("\n");
                    for (int i = 1; i < lignes.length; i++) {
                        String line = lignes[i].trim();
                        if (!line.isEmpty() && !line.equalsIgnoreCase("Aucune tâche.")) {
                            if (line.startsWith("- ")) {
                                line = line.substring(2);
                            }
                            String[] parts = line.split(" \\(Due: |, Priority: ");
                            if (parts.length == 3) {
                                String description = parts[0].trim();
                                String date = parts[1].replace(")", "").trim();
                                String priority = parts[2].replace(")", "").trim();
                                modelTaches.addRow(new Object[]{description, date, priority});
                            }
                        }
                    }
                    onglets.setSelectedIndex(onglets.indexOfTab("Tasks"));
                    statusLabel.setText("Tasks list updated");
                } else if (message.toLowerCase().startsWith("météo à") || message.toLowerCase().startsWith("recette pour")) {
                    zoneRecherche.setText(message);
                    onglets.setSelectedIndex(onglets.indexOfTab("Search"));
                    statusLabel.setText("Search results updated");
                } else if (message.toLowerCase().startsWith("erreur :")) {
                    zoneErreurs.append(message + "\n");
                    onglets.setSelectedIndex(onglets.indexOfTab("Errors"));
                    statusLabel.setText("Error logged");
                } else {
                    JOptionPane.showMessageDialog(null, message, "SmartAssistant", JOptionPane.INFORMATION_MESSAGE);
                    statusLabel.setText("Response received");
                }
            } catch (Exception e) {
                zoneErreurs.append("Erreur parsing réponse : " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(null, "Erreur parsing réponse : " + e.getMessage(), "SmartAssistant", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Error parsing response");
            }
        });
    }
}