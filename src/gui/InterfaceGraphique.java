package gui;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class InterfaceGraphique extends JFrame {
    private JTable tableTaches, tableEvenements;
    private DefaultTableModel modelTaches, modelEvenements;
    private JTextArea zoneRecherche;
    private JTextField champCommande;
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
        JTabbedPane onglets = new JTabbedPane();

        // Onglet Tâches
        String[] colonnesTaches = {"Description", "Date d'échéance", "Priorité"};
        modelTaches = new DefaultTableModel(colonnesTaches, 0);
        tableTaches = new JTable(modelTaches);
        tableTaches.setFillsViewportHeight(true);
        onglets.addTab("Tâches", new JScrollPane(tableTaches));

        // Onglet Événements
        String[] colonnesEvenements = {"Description", "Date"};
        modelEvenements = new DefaultTableModel(colonnesEvenements, 0);
        tableEvenements = new JTable(modelEvenements);
        tableEvenements.setFillsViewportHeight(true);
        onglets.addTab("Événements", new JScrollPane(tableEvenements));

        // Onglet Recherche
        zoneRecherche = new JTextArea();
        zoneRecherche.setEditable(false);
        onglets.addTab("Recherche", new JScrollPane(zoneRecherche));

        add(panneauHaut, BorderLayout.NORTH);
        add(onglets, BorderLayout.CENTER);

        setVisible(true);
    }

    private void envoyerCommande(ActionEvent e) {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty()) return;

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        if (commande.startsWith("ajoute ")) {
            msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
            String[] parts = commande.substring(7).split(":");
            String content = "ajoute_tache:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31") + ":" + (parts.length > 2 ? parts[2] : "Medium");
            msg.setContent(content);
        } else if (commande.startsWith("supprime ")) {
            msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
            msg.setContent("supprime_tache:" + commande.substring(9));
        } else if (commande.equals("liste")) {
            msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
            msg.setContent("liste_taches");
        } else if (commande.startsWith("meteo ")) {
            msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
            msg.setContent("recherche_meteo:" + commande.substring(6));
        } else if (commande.startsWith("recette ")) {
            msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
            msg.setContent("recherche_recette:" + commande.substring(8));
        } else if (commande.startsWith("planifie ")) {
            msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
            String[] parts = commande.substring(9).split(":");
            String content = "planifie_evenement:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31 15:00");
            msg.setContent(content);
        } else if (commande.equals("liste_evenements")) {
            msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
            msg.setContent("liste_evenements");
        } else {
            afficherReponse("Commande inconnue. Essayez : ajoute <tâche>:<date>:<priorité>, supprime <tâche>, liste, meteo <ville>, recette <nom>, planifie <événement>:<date>, liste_evenements");
            return;
        }

        agent.send(msg);
        champCommande.setText("");
    }

    public void afficherReponse(String message) { // Changed to non-static
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("Liste des tâches:")) {
                modelTaches.setRowCount(0);
                String[] lignes = message.split("\n");
                for (int i = 1; i < lignes.length; i++) {
                    if (!lignes[i].isEmpty()) {
                        String[] parts = lignes[i].substring(2).split(" \\(Due: |, Priority: ");
                        modelTaches.addRow(new Object[]{parts[0], parts[1].replace(")", ""), parts[2].replace(")", "")});
                    }
                }
            } else if (message.startsWith("Liste des événements:")) {
                modelEvenements.setRowCount(0);
                String[] lignes = message.split("\n");
                for (int i = 1; i < lignes.length; i++) {
                    if (!lignes[i].isEmpty()) {
                        String[] parts = lignes[i].substring(2).split(" \\(Date: ");
                        modelEvenements.addRow(new Object[]{parts[0], parts[1].replace(")", "")});
                    }
                }
            } else if (message.startsWith("Météo à") || message.startsWith("Recette pour")) {
                zoneRecherche.setText(message);
            } else {
                JOptionPane.showMessageDialog(null, message, "SmartAssistant", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}