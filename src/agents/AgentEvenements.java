package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import models.Event;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AgentEvenements extends Agent {
    private static final Logger LOGGER = Logger.getLogger(AgentEvenements.class.getName());
    private List<Event> listeEvenements = new ArrayList<>();
    private final File file = new File("events.json");

    protected void setup() {
        try {
            System.out.println("AgentEvenements démarré.");
            loadEvents();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de AgentEvenements : " + e.getMessage());
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent().trim();
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    try {
                        LOGGER.info("Received command: " + contenu);
                        if (contenu.startsWith("planifie_evenement:")) {
                            String[] parts = contenu.split(":", 3);
                            if (parts.length < 2) {
                                throw new IllegalArgumentException("Description ou date manquante");
                            }
                            String description = parts[1].trim();
                            if (description.isEmpty()) {
                                throw new IllegalArgumentException("Description vide");
                            }
                            String dateStr = parts.length > 2 ? parts[2].trim() : "2025-12-31 15:00";
                            Date date;
                            try {
                                date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateStr);
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Format de date invalide. Utilisez yyyy-MM-dd HH:mm");
                            }
                            Event evenement = new Event(description, date);
                            if (checkConflit(evenement)) {
                                reply.setContent("⚠️ Conflit détecté pour \"" + description + "\". Proposez une autre date.");
                            } else {
                                listeEvenements.add(evenement);
                                saveEvents();
                                reply.setContent("✅ Événement planifié : " + evenement);
                            }
                        } else if (contenu.startsWith("supprime_evenement:")) {
                            String description = contenu.split(":", 2)[1].trim();
                            boolean removed = listeEvenements.removeIf(e -> e.getDescription().equalsIgnoreCase(description));
                            if (removed) {
                                saveEvents();
                                reply.setContent("✅ Événement supprimé : " + description);
                            } else {
                                reply.setContent("❌ Événement non trouvé : " + description);
                            }
                        } else if (contenu.equals("liste_evenements")) {
                            if (listeEvenements.isEmpty()) {
                                reply.setContent("Liste des événements:\nAucun événement planifié.");
                            } else {
                                StringBuilder sb = new StringBuilder("Liste des événements:\n");
                                for (Event e : listeEvenements) {
                                    sb.append("- ").append(e).append("\n");
                                }
                                reply.setContent(sb.toString());
                            }
                            LOGGER.info("Sent event list: " + reply.getContent());
                        } else {
                            reply.setContent("❌ Commande inconnue. Essayez : planifie <événement>:<yyyy-MM-dd HH:mm>, supprime <événement>, liste_evenements");
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Erreur lors du traitement de la commande : " + contenu + " - " + e.getMessage());
                        e.printStackTrace();
                        reply.setContent("Erreur : " + e.getMessage());
                    }
                    send(reply);
                } else {
                    block();
                }
            }

            private boolean checkConflit(Event newEvent) {
                for (Event e : listeEvenements) {
                    if (Math.abs(e.getDate().getTime() - newEvent.getDate().getTime()) < 3600000) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void saveEvents() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(file, listeEvenements);
            LOGGER.info("Événements sauvegardés dans events.json");
        } catch (Throwable t) { // Catch Throwable to handle NoClassDefFoundError
            LOGGER.severe("Erreur sauvegarde événements : " + t.getMessage());
            t.printStackTrace();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("client", AID.ISLOCALNAME));
            msg.setContent("Erreur sauvegarde événements : " + t.getMessage());
            send(msg);
        }
    }

    private void loadEvents() {
        try {
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                List<Event> loadedEvents = mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, Event.class));
                listeEvenements.addAll(loadedEvents);
                LOGGER.info("Loaded " + loadedEvents.size() + " events from events.json");
            } else {
                LOGGER.info("Aucun fichier events.json trouvé, démarrage avec une liste vide.");
            }
        } catch (Throwable t) { // Catch Throwable to handle NoClassDefFoundError
            LOGGER.severe("Erreur chargement événements : " + t.getMessage());
            t.printStackTrace();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("client", AID.ISLOCALNAME));
            msg.setContent("Erreur chargement événements : " + t.getMessage());
            send(msg);
        }
    }
}