package agents;

import gui.InterfaceGraphique;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.logging.Logger;

public class ClientAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(ClientAgent.class.getName());
    private InterfaceGraphique gui;

    @Override
    protected void setup() {
        try {
            System.out.println("ClientAgent prêt. Lancement de l'interface graphique...");
            gui = new InterfaceGraphique(this);
            LOGGER.info("InterfaceGraphique initialized successfully.");
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de l'interface graphique : " + e.getMessage());
            e.printStackTrace();
            return; // Prevent agent from continuing if GUI fails
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    if (content != null) {
                        gui.afficherReponse(content);
                        LOGGER.info("Received response: " + content);
                    } else {
                        LOGGER.warning("Received empty message from " + msg.getSender().getName());
                    }
                } else {
                    block();
                }
            }
        });
    }

    public void envoyerCommande(String commande) {
        if (commande == null || commande.trim().isEmpty()) {
            gui.afficherReponse("Erreur : Commande vide.");
            LOGGER.warning("Empty command received.");
            return;
        }
        commande = commande.trim();
        String cmdLower = commande.toLowerCase();

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        try {
            if (cmdLower.startsWith("planifie ")) {
                handlePlanifieCommand(commande, msg);
            } else if (cmdLower.equals("liste_evenements")) {
                msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
                msg.setContent("liste_evenements");
            } else if (cmdLower.startsWith("supprime ") && cmdLower.contains("événement")) {
                String description = commande.substring(9).replaceAll("(?i)événement\\s+", "").trim();
                if (description.isEmpty()) {
                    gui.afficherReponse("Erreur : Description d'événement manquante pour suppression.");
                    LOGGER.warning("Empty event description for suppression.");
                    return;
                }
                msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
                msg.setContent("supprime_evenement:" + description);
            } else if (cmdLower.startsWith("meteo ")) {
                String ville = commande.substring(6).trim();
                if (ville.isEmpty()) {
                    gui.afficherReponse("Erreur : Nom de ville manquant.");
                    LOGGER.warning("Empty city name for meteo command.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_meteo:" + ville);
            } else if (cmdLower.startsWith("recette ")) {
                String recette = commande.substring(8).trim();
                if (recette.isEmpty()) {
                    gui.afficherReponse("Erreur : Nom de recette manquant.");
                    LOGGER.warning("Empty recipe name for recette command.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_recette:" + recette);
            } else if (cmdLower.startsWith("ajoute ")) {
                handleAjouteCommand(commande, msg);
            } else if (cmdLower.startsWith("supprime ") && !cmdLower.contains("événement")) {
                String description = commande.substring(9).trim();
                if (description.isEmpty()) {
                    gui.afficherReponse("Erreur : Description de tâche manquante pour suppression.");
                    LOGGER.warning("Empty task description for suppression.");
                    return;
                }
                msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
                msg.setContent("supprime_tache:" + description);
            } else if (cmdLower.equals("liste")) {
                msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
                msg.setContent("liste_taches");
            } else if (cmdLower.equals("vider")) {
                msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
                msg.setContent("vider_taches");
            } else {
                gui.afficherReponse("Commande inconnue. Essayez : planifie <événement>:<date>, liste_evenements, supprime événement <nom>, ajoute <tâche>:<date>:<priorité>, supprime <tâche>, liste, vider, meteo <ville>, recette <nom>");
                LOGGER.warning("Unknown command: " + commande);
                return;
            }
            // Safely log receiver name
            Iterator<?> receiverIter = msg.getAllReceiver();
            String receiverName = "unknown";
            if (receiverIter.hasNext()) {
                Object receiver = receiverIter.next();
                if (receiver instanceof AID) {
                    receiverName = ((AID) receiver).getName();
                }
            }
            LOGGER.info("Sending command: " + msg.getContent() + " to " + receiverName);
            send(msg);
        } catch (Exception e) {
            String errorMsg = "Erreur de traitement de la commande '" + commande + "' : " + e.getMessage();
            gui.afficherReponse(errorMsg);
            LOGGER.severe(errorMsg);
            e.printStackTrace();
        }
    }

    private void handlePlanifieCommand(String commande, ACLMessage msg) {
        String[] parts = commande.substring(9).split(":");
        if (parts.length < 1 || parts[0].trim().isEmpty()) {
            gui.afficherReponse("Erreur : Description d'événement manquante.");
            LOGGER.warning("Empty event description for planifie command.");
            return;
        }
        String description = parts[0].trim();
        String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31 15:00";

        // Append default time if only date is provided
        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            dateStr = dateStr + " 15:00";
        }

        // Validate date format
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            sdf.setLenient(false); // Strict parsing
            sdf.parse(dateStr);
        } catch (Exception e) {
            gui.afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd ou yyyy-MM-dd HH:mm (ex. 2025-06-01 15:00).");
            LOGGER.warning("Invalid date format for planifie command: " + dateStr);
            return;
        }

        msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
        msg.setContent("planifie_evenement:" + description + ":" + dateStr);
    }

    private void handleAjouteCommand(String commande, ACLMessage msg) {
        String[] parts = commande.substring(7).split(":");
        if (parts.length < 1 || parts[0].trim().isEmpty()) {
            gui.afficherReponse("Erreur : Description de tâche manquante.");
            LOGGER.warning("Empty task description for ajoute command.");
            return;
        }
        String description = parts[0].trim();
        String dateStr = parts.length > 1 ? parts[1].trim() : "2025-12-31";
        String priority = parts.length > 2 ? parts[2].trim() : "Medium";

        // Validate date format
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false); // Strict parsing
            sdf.parse(dateStr);
        } catch (Exception e) {
            gui.afficherReponse("Erreur : Format de date invalide pour tâche. Utilisez yyyy-MM-dd (ex. 2025-06-01).");
            LOGGER.warning("Invalid date format for ajoute command: " + dateStr);
            return;
        }

        msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
        msg.setContent("ajoute_tache:" + description + ":" + dateStr + ":" + priority);
    }
}