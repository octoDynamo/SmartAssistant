package gui;

import agents.ClientAgent;
import jade.core.Agent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;

public class InterfaceGraphique extends JFrame {
    private JTable tableTaches, tableEvenements;
    private DefaultTableModel modelTaches, modelEvenements;
    private JTextArea zoneRecherche;
    private JTextField champCommande;
    private JTextArea zoneErreurs;
    private JTabbedPane onglets;
    private Agent agent;

    public InterfaceGraphique(Agent agent) {
        this.agent = agent;
        setTitle("SmartAssistant - Interface Graphique");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Champ de commande
        champCommande = new JTextField();
        JButton boutonEnvoyer = new JButton("Envoyer");
        boutonEnvoyer.addActionListener(this::envoyerCommande);

        JPanel panneauHaut = new JPanel(new BorderLayout());
        panneauHaut.add(new JLabel("Commande : "), BorderLayout.WEST);
        panneauHaut.add(champCommande, BorderLayout.CENTER);
        panneauHaut.add(boutonEnvoyer, BorderLayout.EAST);

        // Onglets
        onglets = new JTabbedPane();

        // Onglet Tâches
        String[] colonnesTaches = {"Description", "Date d'échéance", "Priorité"};
        modelTaches = new DefaultTableModel(colonnesTaches, 0);
        tableTaches = new JTable(modelTaches);
        tableTaches.setFillsViewportHeight(true);
        tableTaches.setAutoCreateRowSorter(true);
        onglets.addTab("Tâches", new JScrollPane(tableTaches));

        // Onglet Événements
        String[] colonnesEvenements = {"Description", "Date"};
        modelEvenements = new DefaultTableModel(colonnesEvenements, 0);
        tableEvenements = new JTable(modelEvenements);
        tableEvenements.setFillsViewportHeight(true);
        tableEvenements.setAutoCreateRowSorter(true);
        onglets.addTab("Événements", new JScrollPane(tableEvenements));

        // Onglet Recherche
        zoneRecherche = new JTextArea();
        zoneRecherche.setEditable(false);
        onglets.addTab("Recherche", new JScrollPane(zoneRecherche));

        // Onglet Erreurs
        zoneErreurs = new JTextArea();
        zoneErreurs.setEditable(false);
        onglets.addTab("Erreurs", new JScrollPane(zoneErreurs));

        add(panneauHaut, BorderLayout.NORTH);
        add(onglets, BorderLayout.CENTER);

        setVisible(true);
    }

    private void envoyerCommande(ActionEvent e) {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty()) {
            afficherReponse("Erreur : Commande vide.");
            return;
        }

        try {
            // Validate commands
            if (commande.toLowerCase().startsWith("planifie ")) {
                String[] parts = commande.substring(9).split(":");
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    afficherReponse("Erreur : Description d'événement manquante.");
                    return;
                }
                String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31 15:00";
                try {
                    if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                        dateStr = dateStr.matches("\\d{4}-\\d{2}-\\d{2}") ? dateStr + " 15:00" : dateStr;
                    }
                    new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateStr);
                } catch (Exception ex) {
                    afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd ou yyyy-MM-dd HH:mm.");
                    return;
                }
            } else if (commande.toLowerCase().startsWith("ajoute ")) {
                String[] parts = commande.substring(7).split(":");
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    afficherReponse("Erreur : Description de tâche manquante.");
                    return;
                }
                String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31";
                try {
                    new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                } catch (Exception ex) {
                    afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd.");
                    return;
                }
            } else if (commande.toLowerCase().startsWith("supprime ")) {
                String desc = commande.substring(9).replaceAll("(?i)événement ", "").trim();
                if (desc.isEmpty()) {
                    afficherReponse("Erreur : Description manquante pour suppression.");
                    return;
                }
            } else if (!commande.toLowerCase().equals("liste") && !commande.toLowerCase().equals("liste_evenements") &&
                    !commande.toLowerCase().equals("vider") && !commande.toLowerCase().startsWith("meteo ") &&
                    !commande.toLowerCase().startsWith("recette ")) {
                afficherReponse("Commande inconnue. Essayez : planifie <événement>:<date>, liste_evenements, supprime événement <nom>, ajoute <tâche>:<date>:<priorité>, supprime <tâche>, liste, vider, meteo <ville>, recette <nom>");
                return;
            }

            // Delegate to ClientAgent
            ((ClientAgent) agent).envoyerCommande(commande);
            champCommande.setText("");
        } catch (Exception ex) {
            afficherReponse("Erreur : " + ex.getMessage());
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
                } else if (message.toLowerCase().startsWith("météo à") || message.toLowerCase().startsWith("recette pour")) {
                    zoneRecherche.setText(message);
                    onglets.setSelectedIndex(onglets.indexOfTab("Recherche"));
                } else if (message.toLowerCase().startsWith("erreur :")) {
                    zoneErreurs.append(message + "\n");
                    onglets.setSelectedIndex(onglets.indexOfTab("Erreurs"));
                } else {
                    JOptionPane.showMessageDialog(null, message, "SmartAssistant", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                zoneErreurs.append("Erreur parsing réponse : " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(null, "Erreur parsing réponse : " + e.getMessage(), "SmartAssistant", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}