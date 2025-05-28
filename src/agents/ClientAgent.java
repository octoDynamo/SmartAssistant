package agents;

import gui.InterfaceGraphique;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import models.Task;
import models.Event;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.util.Locale;

public class ClientAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(ClientAgent.class.getName());
    private InterfaceGraphique gui;
    private AgentCoordination agentCoordination;
    private boolean isLoggedIn = false;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof AgentCoordination) {
            agentCoordination = (AgentCoordination) args[0];
            agentCoordination.setClientAgent(this);
        } else {
            LOGGER.severe("AgentCoordination not provided. Terminating ClientAgent.");
            doDelete();
            return;
        }

        try {
            System.out.println("ClientAgent prêt. Lancement de l'interface graphique...");
            gui = new InterfaceGraphique(agentCoordination);
            LOGGER.info("InterfaceGraphique initialized successfully.");
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de l'interface graphique : " + e.getMessage());
            e.printStackTrace();
            doDelete();
            return;
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

        // Remove the redundant CyclicBehaviour for login state
        // We'll handle this directly via setLoggedIn
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
        if (loggedIn) {
            LOGGER.info("User logged in successfully: " + agentCoordination.getUtilisateurActuel().getNomUtilisateur());
            gui.afficherReponse("Connexion réussie ! Vous pouvez maintenant envoyer des commandes.");
        }
    }

    public void envoyerCommande(String commande) {
        if (commande == null || commande.trim().isEmpty()) {
            gui.afficherReponse("Erreur : Commande vide.");
            LOGGER.warning("Empty command received.");
            return;
        }

        if (!isLoggedIn && !commande.trim().toLowerCase().startsWith("login ")) {
            gui.afficherReponse("Erreur : Vous devez vous connecter d'abord.");
            LOGGER.warning("User not logged in. Command rejected: " + commande);
            return;
        }

        commande = commande.trim().replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+", " ");
        String cmdLower = commande.toLowerCase();

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        try {
            if (cmdLower.startsWith("planifie ")) {
                handlePlanifieCommand(commande, msg);
            } else if (cmdLower.equals("liste_evenements")) {
                StringBuilder result = new StringBuilder("Événements :\n");
                for (Event e : agentCoordination.getUtilisateurActuel().getEvenements()) {
                    result.append("- ").append(e.getTitre())
                            .append(" (Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(e.getDate()))
                            .append(")\n");
                }
                gui.afficherReponse(result.length() <= "Événements :\n".length() ? "Aucun événement trouvé." : result.toString());
                return;
            } else if (cmdLower.startsWith("supprime ") && cmdLower.contains("événement")) {
                String description = commande.substring(9).replaceAll("(?i)événement\\s+", "").trim();
                if (description.isEmpty()) {
                    gui.afficherReponse("Erreur : Description d'événement manquante pour suppression.");
                    LOGGER.warning("Erreur : Description d'événement manquante pour suppression.");
                    return;
                }
                msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
                msg.setContent("supprime_evenement:" + description);
            } else if (cmdLower.startsWith("meteo ")) {
                String ville = commande.substring(6).trim();
                if (ville.isEmpty()) {
                    gui.afficherReponse("Erreur : Nom de ville manquant.");
                    LOGGER.warning("Nom de ville manquant.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_meteo:" + ville);
            } else if (cmdLower.startsWith("recette ")) {
                String recette = commande.substring(8).trim();
                if (recette.isEmpty()) {
                    gui.afficherReponse("Erreur : Nom de recette manquant.");
                    LOGGER.warning("Nom de recette manquant.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_recette:" + recette);
            } else if (cmdLower.startsWith("recherche ")) {
                String terme = commande.substring(10).trim();
                if (terme.isEmpty()) {
                    gui.afficherReponse("Erreur : Terme de recherche manquant.");
                    LOGGER.warning("Terme de recherche manquant.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("rechercher_web:" + terme);
            } else if (cmdLower.startsWith("wiki ")) {
                String terme = commande.substring(5).trim();
                if (terme.isEmpty()) {
                    gui.afficherReponse("Erreur : Terme de recherche Wiki manquant.");
                    LOGGER.warning("Terme de recherche Wiki manquant.");
                    return;
                }
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_wiki:" + terme);
            } else if (cmdLower.equals("actualités")) {
                msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
                msg.setContent("recherche_news");
            } else if (cmdLower.startsWith("ajoute ")) {
                String description = commande.substring(7).trim();
                if (description.isEmpty()) {
                    gui.afficherReponse("Erreur : Description de tâche manquante.");
                    LOGGER.warning("Empty task description for ajout.");
                    return;
                }
                agentCoordination.ajouterTache(description, 1);
                gui.afficherReponse("Tâche ajoutée : " + description);
                return;
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
                StringBuilder result = new StringBuilder("Tâches :\n");
                for (Task t : agentCoordination.getUtilisateurActuel().getTaches()) {
                    result.append("- ").append(t.getDescription())
                            .append(" (Priorité: ").append(t.getPriority())
                            .append(")\n");
                }
                gui.afficherReponse(result.length() <= "Tâches :\n".length() ? "Aucune tâche trouvée." : result.toString());
                return;
            } else if (cmdLower.equals("vider")) {
                msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
                msg.setContent("vider_taches");
            } else {
                gui.afficherReponse("Commande inconnue. Essayez : planifie <événement>:<date> (ex. 2025-06-01), liste_evenements, supprime événement <nom>, ajoute <tâche description>, supprime <tâche>, liste, vider, meteo <ville>, recette <nom>, wiki <terme>, recherche <terme>, actualités");
                LOGGER.warning("Unknown command: " + commande);
                return;
            }

            LOGGER.info("Sending command: " + msg.getContent() + " to " + msg.getAllReceiver().next().toString());
            send(msg);
        } catch (Exception e) {
            String errorMsg = "Erreur de traitement de la commande '" + commande + "' : " + e.getMessage();
            gui.afficherReponse(errorMsg);
            LOGGER.severe(errorMsg);
            e.printStackTrace();
        }
    }

    private void handlePlanifieCommand(String commande, ACLMessage msg) {
        commande = commande.trim().replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+", " ");
        LOGGER.info("Raw input command: '" + commande + "'");

        String[] parts = commande.substring(9).split(":", 2);
        if (parts.length < 1 || parts[0].trim().isEmpty()) {
            gui.afficherReponse("Erreur : Description d'événement manquante.");
            LOGGER.warning("Empty event description for planifie command: " + commande);
            return;
        }

        String description = parts[0].trim();
        String dateStr = parts.length > 1 ? parts[1].trim() : null;

        if (dateStr == null || dateStr.isEmpty()) {
            gui.afficherReponse("Erreur : Date manquante. Utilisez yyyy-MM-dd (ex. 2025-06-01).");
            LOGGER.warning("Missing date for planifie command: " + commande);
            return;
        }

        dateStr = dateStr.replaceAll("[\\u00A0\\u200B\\uFEFF]", " ").replaceAll("[-–—]", "-");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            sdf.parse(dateStr);
        } catch (ParseException e) {
            gui.afficherReponse("Erreur : Format de date invalide. Utilisez yyyy-MM-dd (ex. 2025-06-01).");
            LOGGER.warning("Invalid date format for planifie command: '" + dateStr + "'. Error: " + e.getMessage());
            return;
        }

        String timeStr = gui.showTimePicker();
        if (timeStr == null || timeStr.trim().isEmpty()) {
            gui.afficherReponse("Erreur : Heure non sélectionnée. Planification annulée.");
            LOGGER.warning("No time selected for planifie command: " + commande);
            return;
        }

        timeStr = timeStr.trim();
        String fullDateStr = dateStr + ";" + timeStr;
        LOGGER.info("Attempting to parse dateStr: '" + fullDateStr + "'");

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd;HH:mm", Locale.US);
            sdf.setLenient(false);
            try {
                sdf.parse(fullDateStr);
            } catch (ParseException e) {
                sdf = new SimpleDateFormat("yyyy-MM-dd;H:mm", Locale.US);
                sdf.setLenient(false);
                sdf.parse(fullDateStr);
            }
        } catch (ParseException e) {
            gui.afficherReponse("Erreur : Format d'heure invalide. Utilisez HH:mm ou H:mm (ex. 15:00 ou 9:00).");
            LOGGER.warning("Invalid time format for planifie command: '" + fullDateStr + "'. Error: " + e.getMessage());
            return;
        }

        agentCoordination.planifierEvenement(description, fullDateStr);
        gui.afficherReponse("Événement planifié : " + description + " le " + fullDateStr);
    }
}