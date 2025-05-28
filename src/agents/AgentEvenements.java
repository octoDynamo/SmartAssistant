package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import models.Event;

import javax.swing.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class AgentEvenements extends Agent {
    private static final Logger LOGGER = Logger.getLogger(AgentEvenements.class.getName());
    private List<Event> listeEvenements = new ArrayList<>();
    private final File file = new File("events.json");
    private final Set<String> notificationsEnvoyees = new HashSet<>();
    private static final long CONFLICT_WINDOW_MILLIS = 3600000; // 1 hour in milliseconds

    protected void setup() {
        try {
            System.out.println("AgentEvenements démarré.");
            loadEvents();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de AgentEvenements : " + e.getMessage());
            e.printStackTrace();
        }

        // Add behaviour to handle incoming messages
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent().trim().replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+", " ");
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    try {
                        LOGGER.info("Received command: '" + contenu + "'");

                        if (contenu.startsWith("check_time_slot:")) {
                            long timeMillis = Long.parseLong(contenu.split(":")[1]);
                            Date date = new Date(timeMillis);
                            Event tempEvent = new Event("temp_check", date);
                            boolean conflit = checkConflit(tempEvent);
                            reply.setContent(conflit ? "unavailable" : "available");
                        } else if (contenu.startsWith("reserve_time_slot:")) {
                            String[] parts = contenu.split(":", 3);
                            String description = parts[1];
                            long timeMillis = Long.parseLong(parts[2]);
                            Date date = new Date(timeMillis);
                            Event blockingEvent = new Event("Tâche: " + description, date);
                            if (checkConflit(blockingEvent)) {
                                reply.setContent("conflit");
                            } else {
                                listeEvenements.add(blockingEvent);
                                saveEvents();
                                reply.setContent("ok");
                            }
                        } else if (contenu.startsWith("planifie_evenement:")) {
                            String[] parts = contenu.split(":", 3);
                            if (parts.length < 2) {
                                throw new IllegalArgumentException("Description ou date manquante");
                            }
                            String description = parts[1].trim();
                            if (description.isEmpty()) {
                                throw new IllegalArgumentException("Description vide");
                            }
                            String dateTimeStr = parts.length > 2 ? parts[2].trim() : "2025-12-31;15:00";
                            Date date = parseDate(dateTimeStr);
                            Event evenement = new Event(description, date);
                            if (checkConflit(evenement)) {
                                reply.setContent("⚠️ Conflit détecté pour \"" + description + "\". Proposez une autre date.");
                            } else {
                                planifieEvenement(evenement);
                                reply.setContent("✅ Événement planifié : " + evenement);
                            }
                        } else if (contenu.startsWith("supprime_evenement:")) {
                            String description = contenu.split(":", 2)[1].trim();
                            boolean removed = listeEvenements.removeIf(e -> e.getTitre().equalsIgnoreCase(description)); // Changed to getTitre()
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
                        } else {
                            reply.setContent("❌ Commande inconnue. Essayez : planifie_evenement:<événement>:<yyyy-MM-dd;HH:mm>, supprime_evenement:<événement>, liste_evenements");
                        }
                    } catch (IllegalArgumentException e) {
                        reply.setContent("Erreur : " + e.getMessage());
                    } catch (Exception e) {
                        reply.setContent("Erreur : " + e.getMessage());
                        LOGGER.severe("Exception processing command: " + e.getMessage());
                        e.printStackTrace();
                    }
                    send(reply);
                } else {
                    block();
                }
            }

            private Date parseDate(String dateTimeStr) throws ParseException {
                dateTimeStr = dateTimeStr.replaceAll("[\\u00A0\\u200B\\uFEFF]", " ").replaceAll("[-–—]", "-");
                if (dateTimeStr.contains(" ") && !dateTimeStr.contains(";")) {
                    dateTimeStr = dateTimeStr.replaceFirst(" ", ";");
                    LOGGER.info("Converted space to semicolon: '" + dateTimeStr + "'");
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd;HH:mm", Locale.US);
                sdf.setLenient(false);
                try {
                    return sdf.parse(dateTimeStr);
                } catch (ParseException e) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd;H:mm", Locale.US);
                    sdf.setLenient(false);
                    return sdf.parse(dateTimeStr);
                }
            }
        });

        // Optimized notification system
        addBehaviour(new TickerBehaviour(this, 60000) { // Check every minute
            @Override
            protected void onTick() {
                Date now = new Date();
                Date notificationTime = new Date(now.getTime() + 300000); // 5 minutes later

                listeEvenements.stream()
                        .filter(e -> e.getDate().after(now) && e.getDate().before(notificationTime))
                        .forEach(e -> {
                            String notificationKey = e.getTitre() + "@" + e.getDate().getTime(); // Changed to getTitre()
                            if (notificationsEnvoyees.add(notificationKey)) { // Add returns true if not present
                                showNotification(e);
                            }
                        });

                // Cleanup old notifications
                notificationsEnvoyees.removeIf(key -> {
                    long eventTime = Long.parseLong(key.split("@")[1]);
                    return eventTime < now.getTime();
                });
            }
        });
    }

    public void planifieEvenement(Event evenement) {
        if (checkConflit(evenement)) {
            throw new IllegalStateException("Conflit détecté pour l'événement : " + evenement.getTitre()); // Changed to getTitre()
        }
        listeEvenements.add(evenement);
        saveEvents();
    }

    private boolean checkConflit(Event newEvent) {
        for (Event e : listeEvenements) {
            if (Math.abs(e.getDate().getTime() - newEvent.getDate().getTime()) < CONFLICT_WINDOW_MILLIS) {
                return true;
            }
        }
        return false;
    }

    private void showNotification(Event event) {
        String message = String.format(
                "L'événement \"%s\" commence à %s",
                event.getTitre(), // Changed to getTitre()
                new SimpleDateFormat("HH:mm").format(event.getDate())
        );

        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "🔔 Rappel d'événement",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );
    }

    private void saveEvents() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(file, listeEvenements);
            LOGGER.info("Événements sauvegardés avec succès dans " + file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la sauvegarde des événements : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadEvents() {
        try {
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                List<Event> loadedEvents = mapper.readValue(file,
                        mapper.getTypeFactory().constructCollectionType(List.class, Event.class));
                listeEvenements.addAll(loadedEvents);
                LOGGER.info("Événements chargés avec succès depuis " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.severe("Erreur lors du chargement des événements : " + e.getMessage());
            e.printStackTrace();
        }
    }
}